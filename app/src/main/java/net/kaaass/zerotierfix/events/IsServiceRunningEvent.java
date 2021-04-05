package net.kaaass.zerotierfix.events;

public class IsServiceRunningEvent {
    public boolean isRunning = false;
    public Type type = Type.REQUEST;

    private IsServiceRunningEvent() {
    }

    public static IsServiceRunningEvent NewRequest() {
        return new IsServiceRunningEvent();
    }

    public static IsServiceRunningEvent NewReply(boolean z) {
        IsServiceRunningEvent isServiceRunningEvent = new IsServiceRunningEvent();
        isServiceRunningEvent.isRunning = z;
        isServiceRunningEvent.type = Type.REPLY;
        return isServiceRunningEvent;
    }

    public enum Type {
        REQUEST,
        REPLY
    }
}
