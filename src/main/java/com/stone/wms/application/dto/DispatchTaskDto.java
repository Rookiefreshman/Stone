package com.stone.wms.application.dto;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.inventory.StockWork;
import com.fasterxml.jackson.annotation.JsonInclude;

public final class DispatchTaskDto {
    public final long id;
    public final String taskKey;
    public final int priority;
    public final String state;
    public final String deviceCode;
    public final String claimKey;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final Work work;

    public DispatchTaskDto(DispatchTask task) {
        this.id = task.id;
        this.taskKey = task.taskKey;
        this.priority = task.priority;
        this.state = task.state.name();
        this.deviceCode = task.deviceCode;
        this.claimKey = task.claimKey;
        this.work = task.work == null ? null : new Work(task.work);
    }

    public static final class Work {
        public final String type;
        public final String receiptKey;
        public final long skuId;
        public final String lotNo;
        public final Long sourceId;
        public final long destinationId;
        public final int quantity;

        private Work(StockWork work) {
            type = work.type.name();
            receiptKey = work.receiptKey;
            skuId = work.skuId;
            lotNo = work.lotNo;
            sourceId = work.sourceId;
            destinationId = work.destinationId;
            quantity = work.quantity;
        }
    }
}
