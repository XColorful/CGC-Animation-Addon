package dev.xcolorful.cgcanimation.client.animation.shooter.animator;

import dev.xcolorful.cgcanimation.client.CgcAnimationClient;
import dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy.AnimationManager;
import dev.xcolorful.cgcanimation.client.config.CgcAnimationConfig;
import dev.xcolorful.customgun.client.CustomGunClient;
import dev.xcolorful.customgun.client.animation.shooter.animator.DefaultShooterAnimator;
import dev.xcolorful.customgun.client.api.animation.shooter.IShooterAnimator;
import dev.xcolorful.customgun.client.api.item.gun.IShooterAnimationCategory;
import dev.xcolorful.customgun.client.api.item.gun.ShooterAnimationCategory;
import dev.xcolorful.customgun.client.resource.instance.assets.GunDisplayInstance;
import dev.xcolorful.customgun.core.api.entity.ILivingShooter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 对应 {@link DefaultShooterAnimator}
 */
public class AddonShooterAnimator implements IShooterAnimator {
    public static final AddonShooterAnimator INSTANCE = new AddonShooterAnimator();

    protected AddonShooterAnimator() {
    }

    public static void init() {
        if (CgcAnimationConfig.enableAnimator) {
            register();
        }
    }
    public static void register() {
        CustomGunClient.getShooterAnimationManager().registerAnimator(INSTANCE);
        AnimationManager.register();
        CgcAnimationConfig.enableAnimator = true;
        CgcAnimationClient.saveConfig();
    }
    public static void unregister() {
        CustomGunClient.getShooterAnimationManager().registerAnimator(DefaultShooterAnimator.INSTANCE);
        AnimationManager.unregister();
        CgcAnimationConfig.enableAnimator = false;
        CgcAnimationClient.saveConfig();

        _stopAllPlayerAnimation();
    }
    /**
     * 关闭时清掉已经在播放的动画。{@code ModifierLayer} 一旦被 playerAnimator 接管就会自行 tick，
     * 仅停止逐帧驱动不会让残留动画停下，必须显式 replace 成 null
     */
    private static void _stopAllPlayerAnimation() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        for (AbstractClientPlayer player : level.players()) {
            AnimationManager.stopAllAnimation(player);
        }
    }

    @Override
    public String getAnimatorName() {
        return "AddonShooterAnimator";
    }

    @Override
    public IShooterAnimationCategory getAnimationCategory() {
        return ShooterAnimationCategory.DEFAULT;
    }

    @Override
    public void animateShooter(ModelPart head, ModelPart body, ModelPart leftArm, ModelPart rightArm,
                               ILivingShooter iLivingShooter, LivingEntity livingShooter,
                               GunDisplayInstance gunDisplayInstance) {
        DefaultShooterAnimator.INSTANCE.animateShooter(head, body, leftArm, rightArm, iLivingShooter, livingShooter, gunDisplayInstance);
    }
}
