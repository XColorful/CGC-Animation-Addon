package dev.xcolorful.cgcanimation.forgeclient;

import dev.xcolorful.cgcanimation.client.CgcAnimationClient;
import net.minecraftforge.fml.loading.FMLPaths;

public class CgcAnimationForgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgcAnimationClient.init(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get());
        initialized = true;
    }
}
