package com.stone.wms.infrastructure.repository;

import com.stone.wms.domain.repository.DispatchHistoryRepository;
import com.stone.wms.infrastructure.mapper.DispatchHistoryMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class DispatchHistoryRepositoryImpl implements DispatchHistoryRepository {
    private final DispatchHistoryMapper mapper;
    public DispatchHistoryRepositoryImpl(DispatchHistoryMapper mapper) { this.mapper=mapper; }
    @Override public List<Row> events(long warehouseId,long taskId) { return mapper.events(warehouseId,taskId); }
}
