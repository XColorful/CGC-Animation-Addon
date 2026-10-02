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
import net.minecraft.client.model.geom.ModelPart;
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
