package org.sentinovo.terra_thunder_forged;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.io.PrintWriter;

@Mod.EventBusSubscriber(modid = Terra_thunder_forged.MOD_ID)
public class DebugCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEA_LEVEL = 63;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ttdebug")
                .requires(s -> s.hasPermission(2))
                .executes(DebugCommand::spotCheck)
                .then(Commands.literal("survey")
                    .executes(ctx -> survey(ctx, 200))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(10, 1000))
                        .executes(ctx -> survey(ctx, IntegerArgumentType.getInteger(ctx, "radius")))
                    )
                )
        );
    }

    private static DensityFunction.FunctionContext at(int x, int y, int z) {
        return new DensityFunction.FunctionContext() {
            @Override public int blockX() { return x; }
            @Override public int blockY() { return y; }
            @Override public int blockZ() { return z; }
        };
    }

    private static int spotCheck(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        try {
            NoiseBasedChunkGenerator noiseGen = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            NoiseGeneratorSettings settings = noiseGen.generatorSettings().value();
            RandomState randomState = RandomState.create(settings,
                level.registryAccess().lookupOrThrow(Registries.NOISE), level.getSeed());
            NoiseRouter router = randomState.router();

            DensityFunction.FunctionContext fc = at(pos.getX(), pos.getY(), pos.getZ());
            DensityFunction.FunctionContext fcSea = at(pos.getX(), SEA_LEVEL, pos.getZ());

            double cont = router.continents().compute(fc);
            double eros = router.erosion().compute(fc);
            double ridg = router.ridges().compute(fc);
            double temp = router.temperature().compute(fc);
            double veg = router.vegetation().compute(fc);
            double dep = router.depth().compute(fc);
            double finalD = router.finalDensity().compute(fcSea);

            double pv = -(Math.abs(Math.abs(ridg) - 0.6666) - 0.3333) * 3.0;
            boolean solidAtSea = finalD > 0;

            Holder<Biome> biome = level.getBiome(pos);
            String biomeName = biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");

            source.sendSuccess(() -> Component.literal("\u00a7e=== Terra Thunder Debug ==="), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a77Pos: \u00a7f%d, %d, %d  \u00a77Biome: \u00a7f%s",
                pos.getX(), pos.getY(), pos.getZ(), biomeName)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a77Continentalness: \u00a7f%.4f  \u00a77Erosion: \u00a7f%.4f",
                cont, eros)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a77Ridges: \u00a7f%.4f  \u00a77Peaks&Valleys: \u00a7f%.4f",
                ridg, pv)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a77Temp: \u00a7f%.4f  \u00a77Vegetation: \u00a7f%.4f  \u00a77Depth: \u00a7f%.4f",
                temp, veg, dep)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a77Terrain@SeaLevel: \u00a7f%.4f (%s\u00a7f)",
                finalD, solidAtSea ? "\u00a7aLAND" : "\u00a79WATER")), false);

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            LOGGER.error("ttdebug error", e);
        }
        return 1;
    }

    private static int survey(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        try {
            NoiseBasedChunkGenerator noiseGen = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            NoiseGeneratorSettings settings = noiseGen.generatorSettings().value();
            RandomState randomState = RandomState.create(settings,
                level.registryAccess().lookupOrThrow(Registries.NOISE), level.getSeed());
            NoiseRouter router = randomState.router();

            String filename = "ttdebug_survey.csv";
            int count = 0;
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("x,z,y,biome,continentalness,erosion,ridges,peaks_valleys,temperature,vegetation,depth,final_density_sea,terrain_type");

                for (int x = pos.getX() - radius; x <= pos.getX() + radius; x += 4) {
                    DensityFunction.FunctionContext fc = at(x, pos.getY(), pos.getZ());
                    DensityFunction.FunctionContext fcSea = at(x, SEA_LEVEL, pos.getZ());

                    double cont = router.continents().compute(fc);
                    double eros = router.erosion().compute(fc);
                    double ridg = router.ridges().compute(fc);
                    double pv = -(Math.abs(Math.abs(ridg) - 0.6666) - 0.3333) * 3.0;
                    double temp = router.temperature().compute(fc);
                    double veg = router.vegetation().compute(fc);
                    double dep = router.depth().compute(fc);
                    double finalD = router.finalDensity().compute(fcSea);
                    String terrainType = finalD > 0 ? "LAND" : "WATER";

                    String biomeName;
                    try {
                        Holder<Biome> biome = level.getBiome(new BlockPos(x, pos.getY(), pos.getZ()));
                        biomeName = biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
                    } catch (Exception e) {
                        biomeName = "unloaded";
                    }

                    writer.printf("%d,%d,%d,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%s%n",
                        x, pos.getZ(), pos.getY(), biomeName,
                        cont, eros, ridg, pv, temp, veg, dep, finalD, terrainType);
                    count++;
                }
            }

            final int finalCount = count;
            source.sendSuccess(() -> Component.literal(String.format(
                "\u00a7aSurvey done! %d samples written to %s", finalCount, filename)), false);
            source.sendSuccess(() -> Component.literal(
                "\u00a77File is in your Minecraft instance directory"), false);

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            LOGGER.error("ttdebug survey error", e);
        }
        return 1;
    }
}
