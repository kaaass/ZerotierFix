package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.MoonOrbit;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrbitMoonEvent {

    private List<MoonOrbit> moonOrbits;
}
