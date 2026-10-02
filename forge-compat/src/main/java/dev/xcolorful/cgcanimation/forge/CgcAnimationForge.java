package dev.xcolorful.cgcanimation.forge;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.forgeclient.CgcAnimationForgeClient;
import dev.xcolorful.customgun.core.api.common.McSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(CgcAnimation.MOD_ID)
public class CgcAnimationForge {

    public CgcAnimationForge() {
        Dist dist = FMLLoader.getDist();
        McSide mcSide = dist.isClient() ? McSide.CLIENT : McSide.DEDICATED_SERVER;

        CgcAnimation.init();

        if (mcSide == McSide.CLIENT) {
            _CgcAnimationForgeClient.init();
        }
    }

    private static class _CgcAnimationForgeClient {
        public static void init() {
            CgcAnimationForgeClient.init();
        }
    }
}