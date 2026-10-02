package dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;

/**
 * 原 {@code com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory}
 */
public class AnimationDataRegisterFactory {
    public static void registerData() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.LOWER_ANIMATION, 93, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.LOOP_UPPER_ANIMATION, 94, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.ONCE_UPPER_ANIMATION, 95, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.ROTATION_ANIMATION, 96,
                player -> new ModifierLayer<>(null, AdjustmentYRotModifier.getModifier(player)));
    }
}
