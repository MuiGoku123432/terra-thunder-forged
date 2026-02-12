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

        // Always register the override pack at TOP priority so our noise_settings
        // wins over Terralith's (which otherwise wins due to mod ID alphabetical ordering)
        Path overridePath = ModList.get().getModFileById(MOD_ID).getFile()
                .findResource("override_data");

        event.addRepositorySource(consumer -> {
            Pack overridePack = Pack.readMetaAndCreate(
                    "terra_thunder_override",
                    Component.literal("Terra Thunder Priority Overrides"),
                    true,  // required = true, always enabled
                    id -> new PathPackResources(id, overridePath, true),
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (overridePack != null) {
                consumer.accept(overridePack);
                LOGGER.info("Terra Thunder Forged: Registered override pack (TOP priority)");
            }
        });

        // Register fallback pack only when Terralith is not present
        if (!ModList.get().isLoaded("terralith")) {
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
        } else {
            LOGGER.info("Terra Thunder Forged: Terralith detected, skipping fallback data pack");
        }
    }
}
