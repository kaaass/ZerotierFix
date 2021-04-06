package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class MoonOrbit {

    @Id
    private Long moonWorldId;

    private Long moonSeed;

    @Generated(hash = 203405888)
    public MoonOrbit(Long moonWorldId, Long moonSeed) {
        this.moonWorldId = moonWorldId;
        this.moonSeed = moonSeed;
    }

    @Generated(hash = 1757249393)
    public MoonOrbit() {
    }

    public Long getMoonWorldId() {
        return this.moonWorldId;
    }

    public void setMoonWorldId(Long moonWorldId) {
        this.moonWorldId = moonWorldId;
    }

    public Long getMoonSeed() {
        return this.moonSeed;
    }

    public void setMoonSeed(Long moonSeed) {
        this.moonSeed = moonSeed;
    }
}
