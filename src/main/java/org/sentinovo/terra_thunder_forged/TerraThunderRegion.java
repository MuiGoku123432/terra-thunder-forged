package org.sentinovo.terra_thunder_forged;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class TerraThunderRegion extends Region {

    public TerraThunderRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // Use vanilla biome distribution — TerraBlender biome mods (Terralith etc.)
        // add their own regions alongside this one
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // Can optionally replace biomes that don't suit 2096-block terrain
        });
    }
}
