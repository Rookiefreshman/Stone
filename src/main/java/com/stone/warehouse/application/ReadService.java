package com.stone.warehouse.application;

import com.stone.warehouse.domain.WarehouseRepository;
import com.stone.warehouse.domain.WarehouseModel.Command;
import com.stone.warehouse.domain.WarehouseModel.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.stone.warehouse.application.Views.fields;
import static com.stone.warehouse.domain.WarehouseProblem.found;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReadService {
    private final WarehouseRepository repository;

    public ReadService(WarehouseRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> warehouses() {
        return repository.warehouses().stream().map(Views::master).collect(Collectors.toList());
    }

    public Map<String, Object> catalog(String id) {
        return fields(
                "skus", repository.skus(id).stream().map(Views::master).collect(Collectors.toList()),
                "locations", repository.locations(id).stream().map(Views::master).collect(Collectors.toList()),
                "devices", repository.devices(id).stream().map(Views::master).collect(Collectors.toList()));
    }

    public List<Map<String, Object>> documents(String id, int limit, int offset) {
        return repository.documents(id, limit, offset).stream().map(Views::document).collect(Collectors.toList());
    }

    public Map<String, Object> document(String id) {
        return fields("document", Views.document(found(repository.document(id), "单据")),
                "lines", repository.lines(id).stream().map(Views::line).collect(Collectors.toList()),
                "tasks", repository.tasks(id).stream().map(Views::task).collect(Collectors.toList()),
                "audit", repository.audits(id).stream().map(Views::audit).collect(Collectors.toList()));
    }

    public List<Map<String, Object>> queue(String id, int limit, int offset) {
        return repository.queue(id, limit, offset).stream().map(Views::task).collect(Collectors.toList());
    }

    public Map<String, Object> task(String id) {
        Task task = found(repository.task(id), "任务");
        List<Command> commands = repository.commands(id);
        Command latest = commands.isEmpty() ? null : commands.get(commands.size() - 1);
        return fields("task", Views.task(task),
                "document", Views.document(found(repository.document(task.documentId), "单据")),
                "commands", commands.stream().map(Views::command).collect(Collectors.toList()),
                "events", latest == null ? java.util.Collections.emptyList()
                        : repository.events(latest.id).stream().map(Views::event).collect(Collectors.toList()));
    }

    public List<Map<String, Object>> waves(String warehouseId, int limit, int offset) {
        return repository.waves(warehouseId, limit, offset).stream().map(Views::wave).collect(Collectors.toList());
    }

    public Map<String, Object> wave(String id) {
        return fields("wave", Views.wave(found(repository.wave(id), "波次")),
                "documents", repository.waveDocuments(id).stream().map(Views::document).collect(Collectors.toList()));
    }

    public List<Map<String, Object>> commands(String id) {
        found(repository.task(id), "任务");
        return repository.commands(id).stream().map(Views::command).collect(Collectors.toList());
    }

    public Map<String, Object> command(String id) {
        Command command = found(repository.command(id), "设备指令");
        return fields("command", Views.command(command),
                "task", Views.task(found(repository.task(command.taskId), "任务")),
                "events", repository.events(id).stream().map(Views::event).collect(Collectors.toList()));
    }

    public List<Map<String, Object>> stocks(String id, int limit, int offset) {
        return repository.stocks(id, limit, offset).stream().map(Views::stock).collect(Collectors.toList());
    }

    public List<Map<String, Object>> ledger(String id, int limit, int offset) {
        return repository.ledger(id, limit, offset).stream().map(Views::ledger).collect(Collectors.toList());
    }
}
