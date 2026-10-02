package dev.xcolorful.cgcanimation.forgeclient.init;

import dev.xcolorful.cgcanimation.CgcAnimation;
import dev.xcolorful.cgcanimation.client.init.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CgcAnimation.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClientSetup {

    private static final ClientSetup CLIENT_SETUP = ClientSetup.get();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CLIENT_SETUP::onClientSetup);
    }
}
