package org.sentinovo.terra_thunder_forged.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safety clamp for JJ Thunder's biome parameter values.
 * The density functions should already output vanilla-compatible ranges,
 * but this Mixin provides a backup clamp + diagnostic logging.
 */
@Mixin(Climate.Sampler.class)
public class ClimateSamplerMixin {

    @Unique private static final int SAMPLE_COUNT_1K = 1000;
    @Unique private static final int SAMPLE_COUNT_10K = 10000;
    @Unique private static volatile int sampleIndex = 0;
    @Unique private static volatile boolean summarized1k = false;
    @Unique private static volatile boolean summarized10k = false;
    @Unique private static volatile boolean initialized = false;

    // min/max/sum for: temp, humidity, cont, erosion, weirdness, depth
    @Unique private static final float[] mins = new float[6];
    @Unique private static final float[] maxs = new float[6];
    @Unique private static final double[] sums = new double[6];

    // quart coordinate tracking
    @Unique private static int minQuartX = Integer.MAX_VALUE;
    @Unique private static int maxQuartX = Integer.MIN_VALUE;
    @Unique private static int minQuartZ = Integer.MAX_VALUE;
    @Unique private static int maxQuartZ = Integer.MIN_VALUE;

    // continentalness positive/negative count
    @Unique private static int contPositiveCount = 0;

    @Unique
    private static void ensureInit() {
        if (!initialized) {
            for (int i = 0; i < 6; i++) {
                mins[i] = Float.MAX_VALUE;
                maxs[i] = -Float.MAX_VALUE;
                sums[i] = 0.0;
            }
            initialized = true;
        }
    }

    @Unique
    private static void logSummary(int count) {
        String[] names = {"temp", "hum", "cont", "ero", "weird", "depth"};
        StringBuilder sb = new StringBuilder();
        sb.append("[TerraThunder] ").append(count).append("-sample summary:\n");
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("  %-6s min=%.4f  max=%.4f  avg=%.4f\n",
                names[i], mins[i], maxs[i], sums[i] / count));
        }
        int blockSpanX = (maxQuartX - minQuartX) * 4;
        int blockSpanZ = (maxQuartZ - minQuartZ) * 4;
        sb.append(String.format("  quartX=[%d, %d] quartZ=[%d, %d] (block span: %dx%d)\n",
            minQuartX, maxQuartX, minQuartZ, maxQuartZ, blockSpanX, blockSpanZ));
        float contPosPct = (count > 0) ? (contPositiveCount * 100.0f / count) : 0;
        sb.append(String.format("  cont positive: %d/%d (%.1f%%)\n",
            contPositiveCount, count, contPosPct));
        System.out.println(sb.toString());
    }

    @Inject(method = "sample", at = @At("RETURN"), cancellable = true)
    private void terraThunderNormalizeBiomeParams(int quartX, int quartY, int quartZ,
                                                   CallbackInfoReturnable<Climate.TargetPoint> cir) {
        Climate.TargetPoint original = cir.getReturnValue();

        float rawTemp = Climate.unquantizeCoord(original.temperature());
        float rawHum = Climate.unquantizeCoord(original.humidity());
        float rawCont = Climate.unquantizeCoord(original.continentalness());
        float rawEro = Climate.unquantizeCoord(original.erosion());
        float rawWeird = Climate.unquantizeCoord(original.weirdness());
        float rawDepth = Climate.unquantizeCoord(original.depth());

        if (!summarized10k) {
            ensureInit();
            float[] vals = {rawTemp, rawHum, rawCont, rawEro, rawWeird, rawDepth};
            for (int i = 0; i < 6; i++) {
                if (vals[i] < mins[i]) mins[i] = vals[i];
                if (vals[i] > maxs[i]) maxs[i] = vals[i];
                sums[i] += vals[i];
            }

            if (quartX < minQuartX) minQuartX = quartX;
            if (quartX > maxQuartX) maxQuartX = quartX;
            if (quartZ < minQuartZ) minQuartZ = quartZ;
            if (quartZ > maxQuartZ) maxQuartZ = quartZ;

            if (rawCont > 0) contPositiveCount++;

            sampleIndex++;

            if (sampleIndex >= SAMPLE_COUNT_1K && !summarized1k) {
                summarized1k = true;
                logSummary(sampleIndex);
            }

            if (sampleIndex >= SAMPLE_COUNT_10K) {
                summarized10k = true;
                logSummary(sampleIndex);
            }
        }

        // Safety clamp — density functions should already output correct ranges
        float temp = Mth.clamp(rawTemp, -1.0f, 1.0f);
        float humidity = Mth.clamp(rawHum, -1.0f, 1.0f);
        float cont = Mth.clamp(rawCont, -1.05f, 1.0f);
        float erosion = Mth.clamp(rawEro, -1.0f, 1.0f);
        float weirdness = Mth.clamp(rawWeird, -1.0f, 1.0f);

        cir.setReturnValue(new Climate.TargetPoint(
            Climate.quantizeCoord(temp),
            Climate.quantizeCoord(humidity),
            Climate.quantizeCoord(cont),
            Climate.quantizeCoord(erosion),
            original.depth(),
            Climate.quantizeCoord(weirdness)
        ));
    }
}
