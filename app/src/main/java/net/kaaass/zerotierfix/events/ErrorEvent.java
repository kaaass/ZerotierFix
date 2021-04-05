package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.ResultCode;

public class ErrorEvent {
    ResultCode result;

    public ErrorEvent(ResultCode resultCode) {
        this.result = resultCode;
    }

    public String getError() {
        return this.result.toString();
    }
}
