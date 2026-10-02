package dev.xcolorful.cgcanimation.neoforge;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.neoforgeclient.CgcAnimationNeoforgeClient;
import dev.xcolorful.customgun.core.api.common.McSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(CgcAnimation.MOD_ID)
public class CgcAnimationNeoforge {

    public CgcAnimationNeoforge() {
        Dist dist = FMLLoader.getDist();
        McSide mcSide = dist.isClient() ? McSide.CLIENT : McSide.DEDICATED_SERVER;

        CgcAnimation.init();

        if (mcSide == McSide.CLIENT) {
            _CgcAnimationNeoforgeClient.init();
        }
    }

    private static class _CgcAnimationNeoforgeClient {
        public static void init() {
            CgcAnimationNeoforgeClient.init();
        }
    }
}