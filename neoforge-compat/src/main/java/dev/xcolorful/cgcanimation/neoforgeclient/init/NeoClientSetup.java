package dev.xcolorful.cgcanimation.neoforgeclient.init;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.client.init.ClientSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = CgcAnimation.MOD_ID)
public class NeoClientSetup {

    private static final ClientSetup CLIENT_SETUP = ClientSetup.get();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CLIENT_SETUP::onClientSetup);
    }
}
