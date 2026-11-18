package com.stone.wms.domain.model.dispatch;

public final class DispatchTask {
    public enum State {
        READY, IN_FLIGHT, UNKNOWN, SUCCEEDED, FAILED;

        public boolean allows(State next, boolean reconciled) {
            if (reconciled) {
                return this == UNKNOWN && (next == SUCCEEDED || next == FAILED);
            }
            return this == IN_FLIGHT && (next == UNKNOWN || next == SUCCEEDED || next == FAILED);
        }
    }

    public final long id;
    public final String taskKey;
    public final int priority;
    public final State state;
    public final String deviceCode;
    public final String claimKey;

    public DispatchTask(long id, String taskKey, int priority, State state, String deviceCode, String claimKey) {
        this.id = id;
        this.taskKey = taskKey;
        this.priority = priority;
        this.state = state;
        this.deviceCode = deviceCode;
        this.claimKey = claimKey;
    }
}
