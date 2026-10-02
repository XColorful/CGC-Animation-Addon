package dev.xcolorful.cgcanimation.client.init;

import dev.xcolorful.cgcanimation.client.animation.shooter.animator.AddonShooterAnimator;

public class ClientSetup {

    private static final ClientSetup INSTANCE = new ClientSetup();
    public static ClientSetup get() {
        return INSTANCE;
    }
    private ClientSetup() {}

    public void onClientSetup() {
        AddonShooterAnimator.init();
    }
}
