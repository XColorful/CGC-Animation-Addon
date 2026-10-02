package dev.xcolorful.cgcanimation.client.mixin.playeranimator;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy.AnimationDataRegisterFactory;
import dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy.AnimationManager;
import dev.xcolorful.cgcanimation.client.config.CgcAnimationConfig;
import dev.xcolorful.cgcanimation.client.resources.assets.PlayerAnimationManager;
import dev.xcolorful.customgun.CustomGun;
import dev.xcolorful.customgun.client.api.event.IAddClientReloadListenerEvent;
import dev.xcolorful.customgun.client.compat.playeranimator.PlayerAnimator;
import dev.xcolorful.customgun.client.compat.playeranimator.PlayerAnimatorCompat;
import dev.xcolorful.customgun.client.resource.instance.assets.GunDisplayInstance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原 {@code com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat}
 * <p>
 * 注入目标为模组类，方法名不经混淆，故 {@code remap = false}
 */
@Mixin(PlayerAnimatorCompat.class)
public class PlayerAnimatorCompatMixin {

    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private static void cgc$init(CallbackInfo ci) {
        if (CustomGun.getMcRegistry().isModLoaded(PlayerAnimator.MOD_ID)) {
            AnimationDataRegisterFactory.registerData();
        }
    }

    @Inject(method = "registerReloadListener", at = @At("HEAD"), remap = false)
    private static void cgc$registerReloadListener(IAddClientReloadListenerEvent event,
                                                   CallbackInfo ci) {
        if (CustomGun.getMcRegistry().isModLoaded(PlayerAnimator.MOD_ID)) {
            event.addListener(CustomGun.getMcRegistry().createResourceLocation(CgcAnimation.MOD_ID + ":player_animation_manager"),
                    PlayerAnimationManager.get());
        }
    }

    @Inject(method = "stopAllAnimation", at = @At("HEAD"), remap = false)
    private static void cgc$stopAllAnimation(LivingEntity livingEntity,
                                             CallbackInfo ci) {
        if (livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.stopAllAnimation(player);
        }
    }

    @Inject(method = "playAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cgc$playAnimation(LivingEntity livingEntity,
                                          GunDisplayInstance gunDisplayInstance,
                                          float limbSwingAmount,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(livingEntity instanceof AbstractClientPlayer player)) return;

        if (
                !CgcAnimationConfig.enableAnimator
                || !AnimationManager.hasPlayerAnimator3rd(gunDisplayInstance)
        ) return;

        AnimationManager.playLowerAnimation(player, gunDisplayInstance, limbSwingAmount);
        AnimationManager.playLoopUpperAnimation(player, gunDisplayInstance, limbSwingAmount);
        AnimationManager.playRotationAnimation(player, gunDisplayInstance);
        cir.setReturnValue(true);
    }
}
