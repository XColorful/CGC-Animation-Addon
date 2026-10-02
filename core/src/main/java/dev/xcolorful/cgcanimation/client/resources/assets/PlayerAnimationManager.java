package dev.xcolorful.cgcanimation.client.resources.assets;

import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.customgun.client.api.resource.assets.AssetsFolderType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
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

    public @Nullable KeyframeAnimation getAnimations(ResourceLocation id, String name) {
        var animationHashMap = this.animations.get(id);
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

            try (Reader reader = entry.getValue().openAsReader()) {
                List<KeyframeAnimation> keyframeAnimations = AnimationSerializing.deserializeAnimation(reader);
                for (var animation : keyframeAnimations) {
                    if (animation.extraData.get("name") instanceof String text) {
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
}
