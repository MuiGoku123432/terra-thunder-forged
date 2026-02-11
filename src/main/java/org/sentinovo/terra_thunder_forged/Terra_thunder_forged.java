package org.sentinovo.terra_thunder_forged;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import terrablender.api.Regions;

import java.nio.file.Path;

@Mod(Terra_thunder_forged.MOD_ID)
public class Terra_thunder_forged {
    public static final String MOD_ID = "terra_thunder_forged";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Terra_thunder_forged() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addPackFinders);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new TerraThunderRegion(new ResourceLocation(MOD_ID, "overworld"), 1));
            LOGGER.info("Terra Thunder Forged: Registered TerraBLENDER region (weight=1)");
        });
    }

    private void addPackFinders(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        if (ModList.get().isLoaded("terralith")) {
            LOGGER.info("Terra Thunder Forged: Terralith detected, skipping fallback data pack");
            return;
        }

        Path fallbackPath = ModList.get().getModFileById(MOD_ID).getFile()
                .findResource("fallback_data");

        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    "terra_thunder_fallback",
                    Component.literal("Terra Thunder Fallback"),
                    false,
                    id -> new PathPackResources(id, fallbackPath, true),
                    PackType.SERVER_DATA,
                    Pack.Position.BOTTOM,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
                LOGGER.info("Terra Thunder Forged: Registered fallback data pack (BOTTOM priority)");
            }
        });
    }
}
