package com.stone.wms.application.dto;

import com.stone.wms.domain.model.dispatch.DispatchTask;

public final class DispatchTaskDto {
    public final long id;
    public final String taskKey;
    public final int priority;
    public final String state;
    public final String deviceCode;
    public final String claimKey;

    public DispatchTaskDto(DispatchTask task) {
        this.id = task.id;
        this.taskKey = task.taskKey;
        this.priority = task.priority;
        this.state = task.state.name();
        this.deviceCode = task.deviceCode;
        this.claimKey = task.claimKey;
    }
}
