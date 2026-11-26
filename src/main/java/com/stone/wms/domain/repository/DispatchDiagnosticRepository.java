package com.stone.wms.domain.repository;

/** Current, read-only capacity and task counts for one warehouse and registered device. */
public interface DispatchDiagnosticRepository {
    class Row {
        public long warehouseId;
        public String deviceCode;
        public long maxReady, maxActive, maxBatch, deviceMaxActive;
        public long ready, inFlight, unknown, deviceInFlight, deviceUnknown;
    }
    Row snapshot(long warehouseId, String deviceCode);
}
