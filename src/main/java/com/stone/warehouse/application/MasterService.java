package com.stone.warehouse.application;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.stone.warehouse.domain.WarehouseRepository;
import static com.stone.warehouse.domain.WarehouseModel.*;
import static com.stone.warehouse.domain.WarehouseProblem.*;
import static com.stone.warehouse.application.Inputs.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class MasterService {
    private final WarehouseRepository repository;

    public MasterService(WarehouseRepository repository) {
        this.repository = repository;
    }

    public enum MasterType { WAREHOUSE, SKU, LOCATION, DEVICE }

    public Master create(String warehouseId, MasterType type, MasterInput input) {
        require((type == MasterType.LOCATION) == (input.locationType != null)
                        && (type == MasterType.DEVICE) == (input.deviceMode != null),
                "INVALID_MASTER", "库位须提供库位类型，设备须提供设备模式");
        Master master = new Master();
        master.id = UUID.randomUUID().toString();
        master.warehouseId = warehouseId;
        master.code = input.code.toUpperCase(Locale.ROOT);
        master.name = input.name.trim();
        master.locationType = input.locationType;
        master.deviceMode = input.deviceMode;
        if (type != MasterType.WAREHOUSE) {
            found(repository.lockWarehouse(warehouseId), "仓库");
        }
        switch (type) {
            case WAREHOUSE: repository.insertWarehouse(master); break;
            case SKU: repository.insertSku(master); break;
            case LOCATION: repository.insertLocation(master); break;
            case DEVICE:
                master.state = DeviceState.ENABLED;
                repository.insertDevice(master);
                break;
            default: throw new AssertionError(type);
        }
        return master;
    }

    public Master deviceState(String deviceId, DeviceState state) {
        Master device = found(repository.device(deviceId), "设备");
        found(repository.lockWarehouse(device.warehouseId), "仓库");
        require(state != DeviceState.DISABLED || repository.activeCommands(device.id) == 0,
                "DEVICE_BUSY", "设备仍有执行中的指令");
        repository.setDeviceState(device.id, state);
        return repository.device(device.id);
    }
}
