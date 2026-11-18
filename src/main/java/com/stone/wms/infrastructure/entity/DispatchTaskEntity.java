package com.stone.wms.infrastructure.entity;

import com.stone.wms.domain.model.dispatch.DispatchTask;
import com.stone.wms.domain.model.inventory.StockWork;

public class DispatchTaskEntity {
    public long id;
    public String taskKey;
    public int priority;
    public DispatchTask.State state;
    public String deviceCode;
    public String claimKey;
    public StockWork.Type workType;
    public String receiptKey;
    public long skuId;
    public String lotNo;
    public Long sourceId;
    public long destinationId;
    public int quantity;
}
