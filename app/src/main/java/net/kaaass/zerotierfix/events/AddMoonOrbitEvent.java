package net.kaaass.zerotierfix.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddMoonOrbitEvent {

    private Long moonWorldId;

    private Long moonSeed;
}
