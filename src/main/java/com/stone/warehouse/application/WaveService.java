package com.stone.warehouse.application;

import com.stone.warehouse.domain.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import static com.stone.warehouse.application.Inputs.*;
import static com.stone.warehouse.domain.WarehouseModel.*;
import static com.stone.warehouse.domain.WarehouseProblem.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class WaveService {
    private final WarehouseRepository repository;
    private final DocumentService documents;

    public WaveService(WarehouseRepository repository, DocumentService documents) {
        this.repository = repository;
        this.documents = documents;
    }

    public Wave create(String warehouseId, WaveInput input) {
        found(repository.lockWarehouse(warehouseId), "仓库");
        require(new HashSet<>(input.documentIds).size() == input.documentIds.size(),
                "INVALID_WAVE", "波次内不可重复选择同一出库单");
        Wave wave = new Wave();
        wave.id = UUID.randomUUID().toString();
        wave.warehouseId = warehouseId;
        wave.code = input.code.toUpperCase(Locale.ROOT);
        wave.priority = input.priority;
        wave.state = WaveState.DRAFT;
        repository.insertWave(wave);
        int sequence = 0;
        for (String id : input.documentIds) {
            Document document = found(repository.document(id), "出库单");
            require(warehouseId.equals(document.warehouseId) && document.kind == Kind.OUTBOUND,
                    "INVALID_WAVE", "波次只能选择同仓出库单");
            require(document.state == DocumentState.APPROVED && repository.activeWave(id) == null,
                    "WAVE_ASSIGNED", "只能选择已审核、且未加入其他有效波次的单据");
            repository.addWaveDocument(wave.id, document.id, ++sequence);
        }
        return repository.wave(wave.id);
    }

    public Wave act(String waveId, WaveAction action, String reason) {
        Wave wave = found(repository.wave(waveId), "波次");
        found(repository.lockWarehouse(wave.warehouseId), "仓库");
        wave = found(repository.wave(waveId), "波次");
        require(wave.state == WaveState.DRAFT, "INVALID_STATE", "只有草稿波次可释放或取消");
        if (action == WaveAction.RELEASE) {
            for (Document document : repository.waveDocuments(wave.id)) {
                documents.releaseWaveDocument(document, reason);
            }
            wave.state = WaveState.RELEASED;
        } else {
            wave.state = WaveState.CANCELLED;
        }
        wave.reason = reason;
        repository.updateWave(wave);
        return repository.wave(waveId);
    }
}
