package dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy;

import dev.xcolorful.customgun.CustomGun;
import dev.xcolorful.customgun.core.api.event.*;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterDrawEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterFireEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterMeleeEvent;
import dev.xcolorful.customgun.core.api.event.shooter.ShooterReloadEvent;
import net.minecraft.resources.ResourceLocation;

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
    }
    private void onShooterReload(ShooterReloadEvent event) {
    }
    private void onShooterPrepareMelee(ShooterMeleeEvent event) {
    }
    private void onShooterDraw(ShooterDrawEvent event) {
    }
}
