package com.chillpavz.dynamicnutrition.nutrition;

/**
 * How good a food is for this player right now, for the food finder's "best for me" sort.
 *
 * <p>Arrays are indexed in {@link Nutrients#all()} order. Knows nothing about Minecraft, so a guard
 * can run it from source.
 *
 * <p>What counts is what the meal would really do: a gain only scores up to the nutrient's green
 * line, the further below that line the more it is worth, and whatever would spill over the ceiling
 * costs a little. So a food that fixes one low bar outranks a bigger one that overfills the others.
 */
public final class FoodRanking {

    /** A gain above the green line still counts for a little, so a full player gets a sensible list. */
    private static final float ABOVE_TARGET_WORTH = 0.05F;
    private static final float WASTE_COST = 0.25F;

    private FoodRanking() {
    }

    /**
     * @param multiplier what the repeat penalty leaves of this food, 0 to 1
     * @param max        the ceiling every nutrient stops at
     */
    public static float score(float[] levels, float[] targets, int[] values, float multiplier, float max) {
        float score = 0.0F;
        for (int i = 0; i < values.length; i++) {
            float gain = values[i] * multiplier;
            float room = Math.max(0.0F, targets[i] - levels[i]);
            float useful = Math.min(gain, room);
            float urgency = targets[i] > 0.0F ? 1.0F + room / targets[i] : 1.0F;
            float waste = Math.max(0.0F, levels[i] + gain - max);
            score += useful * urgency + (gain - useful) * ABOVE_TARGET_WORTH - waste * WASTE_COST;
        }
        return score;
    }
}
