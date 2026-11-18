package com.stone.wms.infrastructure.entity;

import com.stone.wms.domain.model.dispatch.DispatchTask;

public class DispatchTaskEntity {
    public long id;
    public String taskKey;
    public int priority;
    public DispatchTask.State state;
    public String deviceCode;
    public String claimKey;
}
