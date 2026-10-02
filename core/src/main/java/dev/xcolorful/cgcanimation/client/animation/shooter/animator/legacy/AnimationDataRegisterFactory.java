package dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;

/**
 * 原 {@code com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory}
 */
public class AnimationDataRegisterFactory {
    private static boolean registered = false;

    /**
     * 只能注册一次。{@code PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory} 是往一个静态 List 里 add，
     * 不去重；而本方法挂在 {@link dev.xcolorful.customgun.client.compat.playeranimator.PlayerAnimatorCompat#init()} 上，
     * CGC 每次客户端资源重载（F3+T、切资源包）都会调它。
     * 重复注册会让每个玩家拿到多份同名层，rotation 层的 AdjustmentModifier 叠加，姿态被放大数倍
     */
    public static void registerData() {
        if (registered) return;
        registered = true;

        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.LOWER_ANIMATION, 93, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.LOOP_UPPER_ANIMATION, 94, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.ONCE_UPPER_ANIMATION, 95, player -> new ModifierLayer<>());
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(AnimationManager.ROTATION_ANIMATION, 96,
                player -> new ModifierLayer<>(null, AdjustmentYRotModifier.getModifier(player)));
    }
}
