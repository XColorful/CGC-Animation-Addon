package dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.xcolorful.cgcanimation.client.resources.assets.PlayerAnimationManager;
import dev.xcolorful.cgcanimation.core.api.resource.assets.animation.BedrockAnimationTag;
import dev.xcolorful.customgun.CustomGun;
import dev.xcolorful.customgun.client.api.resource.ClientResourceApi;
import dev.xcolorful.customgun.client.resource.instance.assets.GunDisplayInstance;
import dev.xcolorful.customgun.core.api.entity.ILivingShooter;
import dev.xcolorful.customgun.core.api.entity.shooter.ILivingShooterGetter;
import dev.xcolorful.customgun.core.api.event.*;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterDrawEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterFireEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterMeleeEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterReloadEvent;
import dev.xcolorful.customgun.core.api.item.IGun;
import dev.xcolorful.customgun.core.api.item.gun.IGunGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AnimationManager implements ICustomEventHandler {
    public static final AnimationManager INSTANCE = new AnimationManager();

    public static ResourceLocation LOWER_ANIMATION = CustomGun.getMcRegistry().createResourceLocation(String.format("%s:%s", CustomGun.MOD_ID_OLD1, "lower_animation"));
    public static ResourceLocation LOOP_UPPER_ANIMATION = CustomGun.getMcRegistry().createResourceLocation(String.format("%s:%s", CustomGun.MOD_ID_OLD1, "loop_upper_animation"));
    public static ResourceLocation ONCE_UPPER_ANIMATION = CustomGun.getMcRegistry().createResourceLocation(String.format("%s:%s", CustomGun.MOD_ID_OLD1, "once_upper_animation"));
    public static ResourceLocation ROTATION_ANIMATION = CustomGun.getMcRegistry().createResourceLocation(String.format("%s:%s", CustomGun.MOD_ID_OLD1, "rotation"));

    protected AnimationManager() {
    }

    public static void register() {
        ICustomEventRegister eventRegister = CustomGun.getEventRegister();
        eventRegister.register(INSTANCE, CustomEventType.SHOOTER_FIRE_EVENT, EventPriority.NORMAL, false);
        eventRegister.register(INSTANCE, CustomEventType.SHOOTER_RELOAD_EVENT, EventPriority.NORMAL, false);
        eventRegister.register(INSTANCE, CustomEventType.SHOOTER_PREPARE_MELEE_EVENT, EventPriority.NORMAL, false);
        eventRegister.register(INSTANCE, CustomEventType.SHOOTER_DRAW_EVENT, EventPriority.NORMAL, false);
    }
    public static void unregister() {
        ICustomEventRegister eventRegister = CustomGun.getEventRegister();
        eventRegister.unregister(INSTANCE, CustomEventType.SHOOTER_FIRE_EVENT, EventPriority.NORMAL, false);
        eventRegister.unregister(INSTANCE, CustomEventType.SHOOTER_RELOAD_EVENT, EventPriority.NORMAL, false);
        eventRegister.unregister(INSTANCE, CustomEventType.SHOOTER_PREPARE_MELEE_EVENT, EventPriority.NORMAL, false);
        eventRegister.unregister(INSTANCE, CustomEventType.SHOOTER_DRAW_EVENT, EventPriority.NORMAL, false);
    }
    @Override
    public String getEventHandlerName() {
        return this.getClass().getName();
    }
    @Override
    public void handleEvent(CustomEventType eventType, ICustomEvent event) {
        switch (eventType) {
            case SHOOTER_FIRE_EVENT -> {
                onShooterFire((ShooterFireEvent) event);
            }
            case SHOOTER_RELOAD_EVENT -> {
                onShooterReload((ShooterReloadEvent) event);
            }
            case SHOOTER_PREPARE_MELEE_EVENT -> {
                onShooterPrepareMelee((ShooterMeleeEvent) event);
            }
            case SHOOTER_DRAW_EVENT -> {
                onShooterDraw((ShooterDrawEvent) event);
            }
            default -> {
                onReceiveWrongEvent(eventType);
            }
        }
    }
    private void onShooterFire(ShooterFireEvent event) {
        if (event.getLogicalSide().isServer()) return;

        @Nullable LivingEntity livingShooter = event.getLivingShooter();
        if (!(livingShooter instanceof AbstractClientPlayer player)) return;

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }

        ItemStack gunItem = event.getGunItem();
        @Nullable IGun iGun = IGunGetter.fromItemStack(gunItem);
        if (iGun == null) return;

        @Nullable GunDisplayInstance gunDisplayInstance = ClientResourceApi.getGunDisplayInstance(gunItem);
        if (gunDisplayInstance == null) return;

        ILivingShooter iLivingShooter = ILivingShooterGetter.cgc$fromLivingEntity(player);
        float aimingProgress = iLivingShooter.cgc$getSynAimingProgress();
        if (aimingProgress <= 0) {
            if (isPlayerLie(player)) {
                playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.LIE_NORMAL_FIRE);
            } else {
                playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.NORMAL_FIRE_UPPER);
            }
        } else {
            if (isPlayerLie(player)) {
                playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.LIE_AIM_FIRE);
            } else {
                playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.AIM_FIRE_UPPER);
            }
        }
    }
    private void onShooterReload(ShooterReloadEvent event) {
        if (event.getLogicalSide().isServer()) return;

        @Nullable LivingEntity livingShooter = event.getLivingShooter();
        if (!(livingShooter instanceof AbstractClientPlayer player)) return;

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }

        ItemStack gunItem = event.getGunItem();
        @Nullable IGun iGun = IGunGetter.fromItemStack(gunItem);
        if (iGun == null) return;

        @Nullable GunDisplayInstance gunDisplayInstance = ClientResourceApi.getGunDisplayInstance(gunItem);
        if (gunDisplayInstance == null) return;

        if (isPlayerLie(player)) {
            playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.LIE_RELOAD);
        } else {
            playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, BedrockAnimationTag.RELOAD_UPPER);
        }
    }
    private void onShooterPrepareMelee(ShooterMeleeEvent event) {
        if (event.getLogicalSide().isServer()) return;

        @Nullable LivingEntity livingShooter = event.getLivingShooter();
        if (!(livingShooter instanceof AbstractClientPlayer player)) return;

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }

        ItemStack gunItem = event.getGunItem();
        @Nullable IGun iGun = IGunGetter.fromItemStack(gunItem);
        if (iGun == null) return;

        int randomIndex = livingShooter.getRandom().nextInt(3);
        String animationName = switch (randomIndex) {
            case 0 -> BedrockAnimationTag.MELEE_UPPER;
            case 1 -> BedrockAnimationTag.MELEE_2_UPPER;
            default -> BedrockAnimationTag.MELEE_3_UPPER;
        };

        @Nullable GunDisplayInstance gunDisplayInstance = ClientResourceApi.getGunDisplayInstance(gunItem);
        if (gunDisplayInstance == null) return;

        playOnceAnimation(player, gunDisplayInstance, ONCE_UPPER_ANIMATION, animationName);
    }
    private void onShooterDraw(ShooterDrawEvent event) {
        if (event.getLogicalSide().isServer()) return;

        LivingEntity entity = event.getLivingShooter();
        if (!(entity instanceof AbstractClientPlayer player)) return;

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }

        ItemStack currentGunItem = event.getCurrentItem();
        ItemStack previousGunItem = event.getPreviousItem();
        // 在切枪时，重置上半身动画
        if (IGunGetter.fromItemStack(currentGunItem) != null && IGunGetter.fromItemStack(previousGunItem) != null) {
            stopAnimation(player, LOOP_UPPER_ANIMATION, 8);
            stopAnimation(player, ONCE_UPPER_ANIMATION, 8);
            stopAnimation(player, LOWER_ANIMATION, 8);
        }
    }

    // --------animation--------

    public static boolean hasPlayerAnimator3rd(GunDisplayInstance display) {
        var location = display.getPojo().getPlayerAnimatorLocation();
        if (location == null) return false;

        return PlayerAnimationManager.get().containsKey(location);
    }

    public static boolean isFlying(AbstractClientPlayer player) {
        return !player.onGround() && player.getAbilities().flying;
    }

    @SuppressWarnings("unchecked")
    public static void playRotationAnimation(AbstractClientPlayer player, GunDisplayInstance display) {
        String animationName = BedrockAnimationTag.EMPTY;
        var dataId = ROTATION_ANIMATION;
        @Nullable var animator3rd = display.getPojo().getPlayerAnimatorLocation();

        if (
                animator3rd == null
                || !PlayerAnimationManager.get().containsKey(animator3rd)
        ) return;

        PlayerAnimationManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) return;

            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    public static void playLowerAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        // 如果玩家趴下，不播放下半身动画
        if (isPlayerLie(player)) return;

        if (player.getVehicle() != null) {
            // 如果玩家骑乘
            playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.RIDE_LOWER);
            return;
        } else if (isFlying(player)) {
            // 如果玩家在天上，下半身动画就是站立动画
            playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.HOLD_LOWER);
            return;
        } else if (player.isSprinting()) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.RUN_LOWER);
            }
            return;
        } else if (limbSwingAmount > 0.05) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.WALK_LOWER);
            }
            return;
        }

        if (player.getPose() == Pose.CROUCHING) {
            playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.CROUCH_LOWER);
        } else {
            playLoopAnimation(player, display, LOWER_ANIMATION, BedrockAnimationTag.HOLD_LOWER);
        }
    }

    public static void playLoopUpperAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        ILivingShooter operator = ILivingShooterGetter.cgc$fromLivingEntity(player);
        float aimingProgress = operator.cgc$getSynAimingProgress();
        if (aimingProgress <= 0) {
            // 疾跑时播放的动画
            if (!isFlying(player) && player.isSprinting()) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.RUN_UPPER);
                }
                return;
            }

            // 行走时的动画
            if (!isFlying(player) && limbSwingAmount > 0.05) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.WALK_UPPER);
                }
                return;
            }

            if (isPlayerLie(player)) {
                // 趴下时的动画
                playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.LIE);
            } else {
                // 普通待命
                playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.HOLD_UPPER);
            }
        } else {
            if (isPlayerLie(player)) {
                // 趴下时瞄准
                if (!isFlying(player) && limbSwingAmount > 0.05) {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.LIE_MOVE);
                } else {
                    playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.LIE_AIM);
                }
            } else {
                // 普通瞄准
                playLoopAnimation(player, display, LOOP_UPPER_ANIMATION, BedrockAnimationTag.AIM_UPPER);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void playLoopAnimation(AbstractClientPlayer player, GunDisplayInstance display, ResourceLocation dataId, String animationName) {
        @Nullable var animator3rd = display.getPojo().getPlayerAnimatorLocation();
        if (animator3rd == null) return;

        if (!PlayerAnimationManager.get().containsKey(animator3rd)) return;

        PlayerAnimationManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            if (modifierLayer.getAnimation() instanceof KeyframeAnimationPlayer animationPlayer && animationPlayer.isActive()) {
                Object extraDataName = animationPlayer.getData().extraData.get("name");
                if (extraDataName instanceof String name && !animationName.equals(name)) {
                    AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                    modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
                }
                return;
            }
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    @SuppressWarnings("unchecked")
    public static void playOnceAnimation(AbstractClientPlayer player, GunDisplayInstance display, ResourceLocation dataId, String animationName) {
        @Nullable var animator3rd = display.getPojo().getPlayerAnimatorLocation();
        if (animator3rd == null) return;

        if (!PlayerAnimationManager.get().containsKey(animator3rd)) return;

        PlayerAnimationManager.get().getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) return;

            IAnimation animation = modifierLayer.getAnimation();
            if (animation == null || !animation.isActive()) {
                AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
            }
        });
    }

    public static void stopAllAnimation(AbstractClientPlayer player) {
        stopAllAnimation(player, 8);
    }

    public static void stopAllAnimation(AbstractClientPlayer player, int fadeTime) {
        stopAnimation(player, LOWER_ANIMATION, fadeTime);
        stopAnimation(player, LOOP_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, ONCE_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, ROTATION_ANIMATION, fadeTime);
    }

    @SuppressWarnings("unchecked")
    private static void stopAnimation(AbstractClientPlayer player, ResourceLocation dataId, int fadeTime) {
        var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
        if (modifierLayer != null && modifierLayer.isActive()) {
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(fadeTime, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, null);
        }
    }

    private static boolean isPlayerLie(AbstractClientPlayer player) {
        return !player.isSwimming() && player.getPose() == Pose.SWIMMING;
    }
}
