package dev.xcolorful.cgcanimation.forgeclient;

import dev.xcolorful.cgcanimation.client.CgcAnimationClient;

public class CgcAnimationForgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgcAnimationClient.init();
        initialized = true;
    }
}
