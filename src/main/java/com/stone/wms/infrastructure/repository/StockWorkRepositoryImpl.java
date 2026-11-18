package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.model.inventory.StockWork;
import com.stone.wms.domain.repository.StockWorkRepository;
import com.stone.wms.infrastructure.mapper.StockWorkMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StockWorkRepositoryImpl implements StockWorkRepository {
    private final StockWorkMapper mapper;

    public StockWorkRepositoryImpl(StockWorkMapper mapper) {
        this.mapper = mapper;
    }

    @Override public List<LedgerRow> ledger(long warehouse, long taskId) { return mapper.ledger(warehouse, taskId); }
    @Override public Location lockLocation(long warehouse, long location) { return mapper.lockLocation(warehouse, location); }
    @Override public long onHand(long warehouse, long location) { return mapper.onHand(warehouse, location); }
    @Override public long incoming(long warehouse, long location) { return mapper.incoming(warehouse, location); }

    @Override
    public boolean hasMixConflict(long warehouse, StockWork work, MixPolicy policy) {
        return policy != MixPolicy.ANY && mapper.hasMixConflict(warehouse, work, policy == MixPolicy.SAME_BATCH);
    }

    @Override
    public boolean hasActiveStockOrWork(long warehouse, long location) {
        return mapper.hasActiveStockOrWork(warehouse, location);
    }

    @Override public String freezeReason(long warehouse, long location, String key) { return mapper.freezeReason(warehouse, location, key); }

    @Override
    public void openFreeze(long warehouse, long location, String key, String reason) {
        mapper.insertFreeze(warehouse, location, key, reason);
        mapper.setFreeze(warehouse, location, key);
    }

    @Override
    public void closeFreeze(long warehouse, long location, String key) {
        mapper.closeFreeze(warehouse, location, key);
        mapper.setFreeze(warehouse, location, null);
    }

    @Override public Receipt receipt(long warehouse, String key) { return mapper.receipt(warehouse, key); }

    @Override
    public void insertReceipt(long warehouse, String key, long sku, String lot, int quantity) {
        mapper.insertReceipt(warehouse, key, sku, lot, quantity);
    }

    @Override public long committedReceipt(long warehouse, String key) { return mapper.committedReceipt(warehouse, key); }

    @Override public void insertWork(long taskId, long warehouse, StockWork work) { mapper.insertWork(taskId, warehouse, work); }
}
