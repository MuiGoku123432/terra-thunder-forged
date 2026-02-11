package org.sentinovo.terra_thunder_forged;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import terrablender.api.Regions;

@Mod(Terra_thunder_forged.MOD_ID)
public class Terra_thunder_forged {
    public static final String MOD_ID = "terra_thunder_forged";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Terra_thunder_forged() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new TerraThunderRegion(new ResourceLocation(MOD_ID, "overworld"), 1));
            LOGGER.info("Terra Thunder Forged: Registered TerraBLENDER region (weight=1)");
        });
    }
}
