package dev.xcolorful.cgcanimation.neoforgeclient;

import dev.xcolorful.cgcanimation.client.CgcAnimationClient;
import net.neoforged.fml.loading.FMLPaths;

public class CgcAnimationNeoforgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgcAnimationClient.init(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get());
        initialized = true;
    }
}
