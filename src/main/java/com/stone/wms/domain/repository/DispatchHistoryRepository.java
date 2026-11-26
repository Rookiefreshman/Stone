package com.stone.wms.domain.repository;

import java.util.List;

/** Read-only port; never invokes dispatch claim or report. */
public interface DispatchHistoryRepository {
    class Row {
        public String eventKey;
        public String targetState;
        public boolean reconciled;
        public String reason;
        public Integer completedQty;
        public String createdAt;
    }
    List<Row> events(long warehouseId, long taskId);
}
