package dev.xcolorful.cgcanimation.client;

public class CgcAnimationClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        initialized = true;
    }
}
