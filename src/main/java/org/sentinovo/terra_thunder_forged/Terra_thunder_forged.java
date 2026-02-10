package org.sentinovo.terra_thunder_forged;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import terrablender.api.Regions;

@Mod(Terra_thunder_forged.MODID)
public class Terra_thunder_forged {

    public static final String MODID = "terra_thunder_forged";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Terra_thunder_forged() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Terra Thunder Forged loaded - 2096-block extreme terrain platform");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Weight 5 = very common — ensures JJ Thunder terrain gets strong representation
            // Terralith and other biome mods add their own regions alongside this
            Regions.register(new TerraThunderRegion(
                new ResourceLocation(MODID, "overworld"), 5));
            LOGGER.info("Terra Thunder Forged: TerraBlender region registered");
        });
    }
}
