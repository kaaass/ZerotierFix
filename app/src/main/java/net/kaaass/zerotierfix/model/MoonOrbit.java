package net.kaaass.zerotierfix.model;

import android.content.Context;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.File;

import lombok.Getter;

/**
 * Moon 入轨信息实体
 *
 * @author kaaass
 */
@Entity
public class MoonOrbit {

    /**
     * Moon 文件相对路径格式
     */
    public static final String MOON_FILE_PATH = "moons.d/%016x.moon";

    /**
     * Moon 地址
     */
    @Id
    private Long moonWorldId;

    /**
     * Moon 种子
     */
    private Long moonSeed;

    /**
     * Moon 是否从文件导入
     */
    private boolean fromFile = false;

    /**
     * Moon 文件是否已缓存
     */
    @Getter
    private transient boolean cacheFile = false;

    @Generated(hash = 322766063)
    public MoonOrbit(Long moonWorldId, Long moonSeed, boolean fromFile) {
        this.moonWorldId = moonWorldId;
        this.moonSeed = moonSeed;
        this.fromFile = fromFile;
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

    public boolean getFromFile() {
        return this.fromFile;
    }

    public void setFromFile(boolean fromFile) {
        this.fromFile = fromFile;
    }

    /**
     * 判断 Moon 是否有缓存
     */
    public void checkCacheFile(Context context) {
        File moon = new File(context.getFilesDir(), String.format(MOON_FILE_PATH, moonWorldId));
        this.cacheFile = moon.exists();
    }

    /**
     * 删除 Moon 缓存
     */
    public void deleteCacheFile(Context context) {
        File moon = new File(context.getFilesDir(), String.format(MOON_FILE_PATH, moonWorldId));
        moon.delete();
    }
}
