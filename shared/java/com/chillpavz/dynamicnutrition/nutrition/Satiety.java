package com.chillpavz.dynamicnutrition.nutrition;

import java.util.Map;
import java.util.WeakHashMap;

import com.chillpavz.dynamicnutrition.config.NutritionConfig;

/**
 * What a meal's makeup does to how long it keeps a player full, and the one setting that scales
 * hunger itself.
 *
 * <p>Protein and fat stretch the saturation a meal gave, carbohydrates shorten it, and vitamins and
 * minerals sit between. Weights chosen on the balance simulation: a meat diet needs about 15% fewer
 * meals, a bread diet about 8% more, and a varied diet stays above every green line.
 *
 * <p><b>The saturation is MEASURED one tick after the meal, not read from the food.</b> Around
 * twenty mods mixin into {@code FoodData.eat}, and the eat hook fires before vanilla applies the
 * food on Fabric and after it on the others. Comparing the bar across the tick needs neither a
 * mixin there nor the per-band food API, and it scales whatever the player really received.
 *
 * <p>Keyed by the player object and weakly held, so a respawned or departed player needs no cleanup.
 * Server thread only.
 */
public final class Satiety {

    private static final float CARBOHYDRATE_WEIGHT = 0.6F;
    private static final float PROTEIN_WEIGHT = 1.4F;
    private static final float FAT_WEIGHT = 1.4F;

    /** Where this setting is at this value, hunger is exactly vanilla's. */
    private static final int NORMAL_DRAIN = 100;

    private static final Map<Object, Track> TRACKS = new WeakHashMap<>();

    private Satiety() {
    }

    private static final class Track {
        float saturation = -1.0F;
        float factor = 1.0F;
    }

    /** Vanilla's exhaustion scaled by the drain setting. Untouched at the default. */
    public static float scaleExhaustion(float exhaustion) {
        int percent = NutritionConfig.hungerDrainPercent;
        return percent == NORMAL_DRAIN ? exhaustion : exhaustion * percent / NORMAL_DRAIN;
    }

    /** The multiplier on a meal's saturation, from its nutrient amounts, before the setting. */
    public static float factor(int carbohydrates, int protein, int fat, int total) {
        if (total <= 0) {
            return 1.0F;
        }
        float others = Math.max(0, total - carbohydrates - protein - fat);
        return (carbohydrates * CARBOHYDRATE_WEIGHT + protein * PROTEIN_WEIGHT + fat * FAT_WEIGHT
                + others) / total;
    }

    /** The multiplier a meal's factor comes to at the current setting. */
    public static float scaled(float factor) {
        return 1.0F + (factor - 1.0F) * NutritionConfig.satietyPercent / 100.0F;
    }

    /** A meal was eaten: its saturation is scaled on this player's next tick. */
    public static void onEaten(Object player, float factor) {
        if (NutritionConfig.satietyPercent > 0) {
            TRACKS.computeIfAbsent(player, p -> new Track()).factor = factor;
        }
    }

    /**
     * Once a tick with the player's saturation.
     *
     * @return what to add to it, 0 unless a meal has just landed.
     */
    public static float onTick(Object player, float saturation) {
        Track track = TRACKS.computeIfAbsent(player, p -> new Track());
        float previous = track.saturation;
        float factor = track.factor;
        track.saturation = saturation;
        track.factor = 1.0F;
        float gained = saturation - previous;
        if (factor == 1.0F || previous < 0.0F || gained <= 0.0F) {
            return 0.0F;
        }
        float change = gained * (scaled(factor) - 1.0F);
        track.saturation = saturation + change;
        return change;
    }
}
