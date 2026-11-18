package com.stone.warehouse.interfaces;

import com.stone.warehouse.application.*;
import com.stone.warehouse.application.MasterService.MasterType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

import static com.stone.warehouse.application.Inputs.*;

@RestController
@RequestMapping("/api")
@Validated
public class WarehouseController {
    private final MasterService masters;
    private final DocumentService documents;
    private final ExecutionService execution;
    private final WaveService waves;
    private final ReadService read;

    public WarehouseController(MasterService masters, DocumentService documents, ExecutionService execution,
                               WaveService waves, ReadService read) {
        this.masters = masters;
        this.documents = documents;
        this.execution = execution;
        this.waves = waves;
        this.read = read;
    }

    @InitBinder
    void fieldValidation(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/warehouses")
    List<Map<String, Object>> warehouses() {
        return read.warehouses();
    }

    @PostMapping("/warehouses")
    Map<String, Object> warehouse(@Valid @RequestBody MasterInput input) {
        return Views.master(masters.create(null, MasterType.WAREHOUSE, input));
    }

    @GetMapping("/warehouses/{id}/catalog")
    Map<String, Object> catalog(@PathVariable String id) {
        return read.catalog(id);
    }

    @PostMapping("/warehouses/{id}/skus")
    Map<String, Object> sku(@PathVariable String id, @Valid @RequestBody MasterInput input) {
        return Views.master(masters.create(id, MasterType.SKU, input));
    }

    @PostMapping("/warehouses/{id}/locations")
    Map<String, Object> location(@PathVariable String id, @Valid @RequestBody MasterInput input) {
        return Views.master(masters.create(id, MasterType.LOCATION, input));
    }

    @PostMapping("/warehouses/{id}/devices")
    Map<String, Object> device(@PathVariable String id, @Valid @RequestBody MasterInput input) {
        return Views.master(masters.create(id, MasterType.DEVICE, input));
    }

    @PutMapping("/devices/{id}/state")
    Map<String, Object> deviceState(@PathVariable String id, @Valid @RequestBody DeviceStateInput input) {
        return Views.master(masters.deviceState(id, input.state));
    }

    @GetMapping("/warehouses/{id}/documents")
    List<Map<String, Object>> documents(@PathVariable String id, @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
                                        @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return read.documents(id, limit, offset);
    }

    @PostMapping("/warehouses/{id}/documents")
    Map<String, Object> create(@PathVariable String id, @Valid @RequestBody DocumentInput input) {
        return Views.document(documents.create(id, input));
    }

    @GetMapping("/documents/{id}")
    Map<String, Object> document(@PathVariable String id) {
        return read.document(id);
    }

    @PostMapping("/documents/{id}/actions/{action}")
    Map<String, Object> action(@PathVariable String id, @PathVariable Action action, @Valid @RequestBody Reason reason) {
        return Views.document(documents.act(id, action, reason.reason));
    }

    @GetMapping("/warehouses/{id}/waves")
    List<Map<String, Object>> waves(@PathVariable String id, @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
                                    @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return read.waves(id, limit, offset);
    }

    @PostMapping("/warehouses/{id}/waves")
    Map<String, Object> createWave(@PathVariable String id, @Valid @RequestBody WaveInput input) {
        return Views.wave(waves.create(id, input));
    }

    @GetMapping("/waves/{id}")
    Map<String, Object> wave(@PathVariable String id) {
        return read.wave(id);
    }

    @PostMapping("/waves/{id}/actions/{action}")
    Map<String, Object> waveAction(@PathVariable String id, @PathVariable WaveAction action,
                                   @Valid @RequestBody Reason reason) {
        return Views.wave(waves.act(id, action, reason.reason));
    }

    @GetMapping("/warehouses/{id}/tasks")
    List<Map<String, Object>> tasks(@PathVariable String id, @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
                                    @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return read.queue(id, limit, offset);
    }

    @PostMapping("/tasks/{id}/dispatch")
    Map<String, Object> dispatch(@PathVariable String id, @Valid @RequestBody DispatchInput input) {
        return Views.command(execution.dispatch(id, input.deviceId));
    }

    @PostMapping("/tasks/{id}/reconcile")
    Map<String, Object> reconcile(@PathVariable String id, @Valid @RequestBody Reconciliation input) {
        return Views.task(execution.reconcile(id, input));
    }

    @GetMapping("/tasks/{id}")
    Map<String, Object> task(@PathVariable String id) {
        return read.task(id);
    }

    @GetMapping("/tasks/{id}/commands")
    List<Map<String, Object>> commands(@PathVariable String id) {
        return read.commands(id);
    }

    @GetMapping("/commands/{id}")
    Map<String, Object> command(@PathVariable String id) {
        return read.command(id);
    }

    @PostMapping("/commands/{id}/events")
    Map<String, Object> event(@PathVariable String id, @Valid @RequestBody EventInput input) {
        return Views.command(execution.feedback(id, input));
    }

    @GetMapping("/warehouses/{id}/inventory")
    List<Map<String, Object>> inventory(@PathVariable String id, @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
                                        @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return read.stocks(id, limit, offset);
    }

    @GetMapping("/warehouses/{id}/ledger")
    List<Map<String, Object>> ledger(@PathVariable String id, @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
                                     @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return read.ledger(id, limit, offset);
    }
}
