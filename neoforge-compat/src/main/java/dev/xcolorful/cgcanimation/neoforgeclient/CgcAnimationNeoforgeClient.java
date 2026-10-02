package dev.xcolorful.cgcanimation.neoforgeclient;

import dev.xcolorful.cgcanimation.client.CgcAnimationClient;

public class CgcAnimationNeoforgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgcAnimationClient.init();
        initialized = true;
    }
}
