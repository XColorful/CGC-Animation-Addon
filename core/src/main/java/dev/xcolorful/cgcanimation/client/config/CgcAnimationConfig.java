package dev.xcolorful.cgcanimation.client.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.core.api.config.CgcAnimationConfigTag;
import dev.xcolorful.customgun.core.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CGC Animation Addon 的客户端运行时配置。
 * <p>
 * 配置存放在平台的 config 目录下（{@link dev.xcolorful.cgcanimation.client.CgcAnimationClient#FILE_NAME}）。
 * 读取采用流式解析，文件不存在或字段缺失时使用默认值，加载后按固定顺序回写，
 * 保证缺省的键也会出现在文件里供使用者编辑。
 */
public class CgcAnimationConfig {

    // --------默认值--------

    private static final boolean DEFAULT_ENABLE_ANIMATOR = true;

    /**
     * 是否启用动画。
     */
    public static boolean enableAnimator = DEFAULT_ENABLE_ANIMATOR;

    private CgcAnimationConfig() {}

    /**
     * 从配置目录加载配置；文件不存在时按默认值创建。
     *
     * @param configFile 配置文件的绝对路径
     */
    public static void load(Path configFile) {
        if (Files.exists(configFile)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(configFile, StandardCharsets.UTF_8))) {
                _read(reader);
            } catch (IOException | RuntimeException exception) {
                CgcAnimation.LOGGER.error("Failed to read config file {}, using defaults", configFile, exception);
            }
        }

        CgcAnimationConfig.save(configFile);
    }

    /**
     * 把配置按固定顺序回写，便于使用者直接编辑。
     *
     * @param configFile 配置文件的绝对路径
     */
    public static void save(Path configFile) {
        try {
            Path parent = configFile.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(configFile, StandardCharsets.UTF_8))) {
                writer.setIndent("\t");
                writer.beginObject(); {
                    JsonUtils.writeBoolean(writer, CgcAnimationConfigTag.ENABLE_ANIMATOR, enableAnimator);
                }
                writer.endObject();
            }
        } catch (IOException exception) {
            CgcAnimation.LOGGER.error("Failed to write config file {}", configFile, exception);
        }
    }

    private static void _read(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case CgcAnimationConfigTag.ENABLE_ANIMATOR -> enableAnimator = JsonUtils.readBoolean(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }
}
