package net.kaaass.zerotierfix.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RemoveMoonOrbitEvent {

    private Long moonWorldId;

    private Long moonSeed;
}
