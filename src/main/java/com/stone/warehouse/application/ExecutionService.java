package com.stone.warehouse.application;

import com.stone.warehouse.domain.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static com.stone.warehouse.application.Inputs.*;
import static com.stone.warehouse.domain.WarehouseModel.*;
import static com.stone.warehouse.domain.WarehouseProblem.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ExecutionService {
    private final WarehouseRepository repository;
    private final DocumentService documents;
    private final InventoryService inventory;

    public ExecutionService(WarehouseRepository repository, DocumentService documents, InventoryService inventory) {
        this.repository = repository;
        this.documents = documents;
        this.inventory = inventory;
    }

    public Command dispatch(String taskId, String deviceId) {
        Task task = found(repository.task(taskId), "任务");
        Document document = documents.locked(task.documentId);
        task = found(repository.task(taskId), "任务");
        Master device = found(repository.device(deviceId), "设备");
        require(task.state == TaskState.READY && document.state == DocumentState.EXECUTING,
                "INVALID_STATE", "仅执行中单据的待派发任务可下发");
        require(document.warehouseId.equals(device.warehouseId), "WRONG_WAREHOUSE", "设备不属于任务仓库");
        require(device.state == DeviceState.ENABLED && repository.activeCommands(device.id) == 0
                        && repository.unresolvedFailures(device.id) == 0,
                "DEVICE_BUSY", "设备停用、正在执行或存在未处理异常");
        if (document.kind == Kind.OUTBOUND || document.kind.movement()) {
            inventory.ensureEligibleBatch(task);
        }
        task.attempt++;
        task.state = TaskState.RUNNING;
        repository.updateTask(task);

        Command command = new Command();
        command.id = UUID.randomUUID().toString();
        command.taskId = task.id;
        command.deviceId = device.id;
        command.attempt = task.attempt;
        command.state = CommandState.SENT;
        repository.insertCommand(command);
        documents.audit(document, task, "DISPATCH", command.id);
        return repository.command(command.id);
    }

    public Command feedback(String commandId, EventInput input) {
        Command command = found(repository.command(commandId), "设备指令");
        Task task = found(repository.task(command.taskId), "任务");
        Document document = documents.locked(task.documentId);
        command = found(repository.command(commandId), "设备指令");
        CommandEvent previous = repository.event(commandId, input.eventKey);
        if (previous != null) {
            require(previous.eventType == input.eventType
                            && Objects.equals(previous.observedQuantity, input.observedQuantity)
                            && Objects.equals(previous.detail, input.detail),
                    "EVENT_CONFLICT", "同一回执编号的内容必须一致");
            return command;
        }
        task = found(repository.task(command.taskId), "任务");
        require(task.state == TaskState.RUNNING && task.attempt == command.attempt,
                "STALE_COMMAND", "指令已处理或已进入其他执行轮次");
        validateTransition(command, input);
        validateObservation(document, input.eventType == CommandState.SUCCEEDED, input.observedQuantity);
        CommandEvent event = new CommandEvent();
        event.commandId = commandId;
        event.eventKey = input.eventKey;
        event.eventType = input.eventType;
        event.observedQuantity = input.observedQuantity;
        event.detail = input.detail;
        repository.insertEvent(event);
        repository.setCommandState(command.id, event.eventType);
        if (event.eventType == CommandState.SUCCEEDED) {
            complete(document, task, event.observedQuantity);
        } else if (event.eventType == CommandState.FAILED) {
            task.state = TaskState.EXCEPTION;
            repository.updateTask(task);
        }
        return repository.command(commandId);
    }

    private void validateTransition(Command command, EventInput input) {
        boolean valid;
        switch (input.eventType) {
            case STARTED: valid = command.state == CommandState.SENT; break;
            case SUCCEEDED: valid = command.state == CommandState.STARTED; break;
            case FAILED:
                valid = (command.state == CommandState.SENT || command.state == CommandState.STARTED)
                        && input.detail != null && !input.detail.trim().isEmpty();
                break;
            default: valid = false;
        }
        require(valid, "INVALID_EVENT", "回执顺序非法，或失败回执缺少原因");
    }

    public Task reconcile(String taskId, Reconciliation input) {
        Task task = found(repository.task(taskId), "任务");
        Document document = documents.locked(task.documentId);
        task = found(repository.task(taskId), "任务");
        require(task.state == TaskState.EXCEPTION, "INVALID_STATE", "仅异常待核对任务可处理");
        validateObservation(document, input.completed, input.observedQuantity);
        if (input.completed) {
            complete(document, task, input.observedQuantity);
        } else {
            task.state = TaskState.READY;
            repository.updateTask(task);
        }
        documents.audit(document, task, input.completed ? "CONFIRM_COMPLETED" : "CONFIRM_NO_MOVEMENT", input.reason);
        return repository.task(taskId);
    }

    private void validateObservation(Document document, boolean success, Long quantity) {
        require((document.kind == Kind.COUNT && success) == (quantity != null),
                "INVALID_OBSERVATION", "仅盘点成功时必须填写实盘数量");
    }

    private void complete(Document document, Task task, Long observation) {
        if (document.kind.receipt()) {
            inventory.post(task, task.destinationId, task.quantity, 0, "PUTAWAY");
        } else if (document.kind.movement()) {
            inventory.post(task, task.sourceId, -task.quantity, -task.quantity, "MOVE_OUT");
            inventory.post(task, task.destinationId, task.quantity, 0, "MOVE_IN");
        }
        task.state = TaskState.DONE;
        task.observedQuantity = observation;
        repository.updateTask(task);
        boolean allDone = repository.tasks(document.id).stream().allMatch(item -> item.state == TaskState.DONE);
        if (allDone) {
            DocumentState next = DocumentState.COMPLETED;
            if (document.kind == Kind.OUTBOUND) {
                next = DocumentState.PICKED;
            } else if (document.kind == Kind.COUNT) {
                next = DocumentState.COUNTED;
            }
            repository.setDocumentState(document.id, next);
        }
    }
}
