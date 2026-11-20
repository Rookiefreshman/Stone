package com.stone.wms.application.dto;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.repository.StockWorkRepository.LedgerRow;

import java.util.List;
import java.util.stream.Collectors;

public final class StockWorkDetailDto {
    public final DispatchTaskDto task;
    public final List<LedgerEntry> ledger;

    public StockWorkDetailDto(DispatchTask task, List<LedgerRow> ledger) {
        this.task = new DispatchTaskDto(task);
        this.ledger = ledger.stream().map(LedgerEntry::new).collect(Collectors.toList());
    }

    public static final class LedgerEntry {
        public final long locationId;
        public final String action;
        public final int deltaOnHand;
        public final int deltaAvailable;
        public final int deltaReserved;
        public final int deltaFrozen;

        private LedgerEntry(LedgerRow row) {
            locationId = row.locationId;
            action = row.action;
            deltaOnHand = row.deltaOnHand;
            deltaAvailable = row.deltaAvailable;
            deltaReserved = row.deltaReserved;
            deltaFrozen = row.deltaFrozen;
        }
    }
}
