package dev.xcolorful.cgcanimation.client.resources.assets;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.customgun.client.api.resource.assets.AssetsFolderType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 原 {@code com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager}
 */
public class PlayerAnimationManager extends SimplePreparableReloadListener<Map<ResourceLocation, HashMap<String, KeyframeAnimation>>> {
    private static PlayerAnimationManager INSTANCE;

    private final FileToIdConverter fileToIdConverter = new FileToIdConverter(AssetsFolderType.PLAYER_ANIMATOR.getFolderName(), ".json");
    private final HashMap<ResourceLocation, HashMap<String, KeyframeAnimation>> animations = new HashMap<>();

    public static PlayerAnimationManager get() {
        if (INSTANCE == null) INSTANCE = new PlayerAnimationManager();

        return INSTANCE;
    }

    public @Nullable KeyframeAnimation getAnimations(ResourceLocation animationLocation,
                                                     String name) {
        var animationHashMap = this.animations.get(animationLocation);
        if (animationHashMap == null) {
            return null;
        }
        return animationHashMap.get(name);
    }

    public boolean containsKey(ResourceLocation id) {
        return animations.containsKey(id);
    }

    @Override
    protected Map<ResourceLocation, HashMap<String, KeyframeAnimation>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, HashMap<String, KeyframeAnimation>> output = Maps.newHashMap();
        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(manager).entrySet()) {
            var resourceLocation = entry.getKey();
            var id = fileToIdConverter.fileToId(resourceLocation);

            try (InputStream stream = entry.getValue().open()) {
                byte[] json = fixBrokenEase(stream.readAllBytes());
                Collection<IPlayable> keyframeAnimations = AnimationCodecs.deserialize("json", () -> new ByteArrayInputStream(json));
                for (var playable : keyframeAnimations) {
                    if (playable instanceof KeyframeAnimation animation && animation.extraData.get("name") instanceof String text) {
                        String name = PlayerAnimationRegistry.serializeTextToString(text).toLowerCase(Locale.ENGLISH);
                        output.computeIfAbsent(id, k -> Maps.newHashMap()).put(name, animation);
                    }
                }
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                CgcAnimation.LOGGER.warn("Failed to player animation file: {}, entry: {}", resourceLocation, entry);
            }
        }
        return output;
    }

    @Override
    protected void apply(Map<ResourceLocation, HashMap<String, KeyframeAnimation>> map, ResourceManager manager, ProfilerFiller profiler) {
        animations.clear();
        animations.putAll(map);
    }

    private static final Gson GSON = new Gson();
    private static final String[] EASE_KEYS = {"lerp_mode", "easing"};
    /** playeranimator 2.x 会把它解析成坏缓动的值 */
    private static final String BROKEN_EASE = "catmullrom";
    private static final String FALLBACK_EASE = "linear";

    /**
     * 把 {@code lerp_mode} / {@code easing} 为 {@code catmullrom} 的键帧改写成 {@code linear}。
     * <p>
     * {@code Ease.CATMULLROM} 是 playeranimator 2.x 才加的，而它的 {@code Easing.catmullRom} 展开后恒等于
     * {@code n + 2}，配上 {@code easeInOut} 会在 f = 0.5 处从 1.5 跳到 -0.5。{@code getValueFromKeyframes}
     * 算的是 {@code lerp(ease(f), before, after)}，于是每段键帧的中点都会弹跳 {@code 2 * (after - before)}，
     * 表现为第三人称持续抽搐；TaCZ 默认枪包里的键帧恰好全是这个值。
     * <p>
     * playeranimator 1.0.2（1.20.1 用的那份）没有这个枚举值，{@code Easing.easeFromString} 会抛异常兜底成
     * {@code LINEAR}，所以 1.20.1 看起来正常——归一成 {@code linear} 就是为了和它保持一致。
     * （TaCZ 1.21.1 是在枪包里把 {@code lerp_mode} 全改成 {@code inoutsine} 绕开的，见其 commit {@code 0c884f33}。）
     */
    private static byte[] fixBrokenEase(byte[] raw) {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return raw;
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            return raw;
        }

        if (!fixAnimations(root.get("animations"))) return raw;

        return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    /** {@code animations -> <动画名> -> bones -> <骨骼名> -> <rotation|position|scale> -> <时间> -> 键帧} */
    private static boolean fixAnimations(@Nullable JsonElement animations) {
        if (animations == null || !animations.isJsonObject()) return false;

        boolean changed = false;
        for (JsonElement animation : animations.getAsJsonObject().asMap().values()) {
            if (!animation.isJsonObject()) continue;

            JsonElement bones = animation.getAsJsonObject().get("bones");
            if (bones == null || !bones.isJsonObject()) continue;

            for (JsonElement bone : bones.getAsJsonObject().asMap().values()) {
                if (!bone.isJsonObject()) continue;

                for (JsonElement channel : bone.getAsJsonObject().asMap().values()) {
                    if (!channel.isJsonObject()) continue;

                    for (JsonElement keyFrame : channel.getAsJsonObject().asMap().values()) {
                        changed |= fixKeyFrame(keyFrame);
                    }
                }
            }
        }
        return changed;
    }

    private static boolean fixKeyFrame(JsonElement keyFrame) {
        if (!keyFrame.isJsonObject()) return false;

        boolean changed = false;
        JsonObject object = keyFrame.getAsJsonObject();
        for (String key : EASE_KEYS) {
            JsonElement ease = object.get(key);
            if (ease != null && ease.isJsonPrimitive() && BROKEN_EASE.equalsIgnoreCase(ease.getAsString())) {
                object.addProperty(key, FALLBACK_EASE);
                changed = true;
            }
        }
        return changed;
    }
}
