package dev.xcolorful.cgcanimation.client;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.client.config.CgcAnimationConfig;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

public class CgcAnimationClient {

    protected static boolean initialized;

    /**
     * 配置文件名，位于平台的 config 目录下
     */
    public static final String FILE_NAME = CgcAnimation.MOD_ID + "-client" + ".json";

    /**
     * 游戏根目录，用于解析配置中的相对路径
     */
    private static Path gameDirectory;

    /**
     * 模组配置文件的绝对路径
     */
    private static Path configFile;

    public static void init(Path gameDirectory, Path configDirectory) {
        if (initialized) return;

        CgcAnimationClient.gameDirectory = gameDirectory;
        CgcAnimationClient.configFile = configDirectory.resolve(FILE_NAME);

        CgcAnimationClient.reloadConfig();

        initialized = true;
    }

    /**
     * 重新从磁盘读取配置
     */
    @ApiStatus.Internal
    public static void reloadConfig() {
        if (configFile == null) return;
        CgcAnimationConfig.load(configFile);
    }

    @ApiStatus.Internal
    public static void saveConfig() {
        if (configFile == null) return;
        CgcAnimationConfig.save(configFile);
    }

    /**
     * @return 游戏根目录
     */
    public static Path gameDirectory() {
        return gameDirectory;
    }

    /**
     * @return 模组配置文件的绝对路径
     */
    public static Path configFile() {
        return configFile;
    }
}
