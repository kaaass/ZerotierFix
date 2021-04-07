package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.Peer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PeerInfoReplyEvent {

    private final Peer[] peers;
}
