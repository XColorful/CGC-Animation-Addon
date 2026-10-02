package dev.xcolorful.cgcanimation;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CgcAnimation {
    public static final String MOD_ID = "cgcanimation";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        initialized = true;
    }
}
