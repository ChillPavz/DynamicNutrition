package com.chillpavz.dynamicnutrition.nutrition;

import java.util.Collection;

import com.chillpavz.dynamicnutrition.config.NutritionConfig;

/**
 * What the player is doing decides WHICH nutrients pay for the food they burn: running draws on
 * carbohydrates, fighting on protein, the cold on fat.
 *
 * <p>The total is unchanged, so this moves decay between nutrients and adds none. That is what keeps
 * the rest of the balance standing. Multipliers chosen on the simulation: with a varied diet a player
 * sprinting half the time dips under the carbohydrate green line, and nobody reaches a red line.
 * Fighting takes the largest figure because fights are short.
 */
public final class ActivityDecay {

    public static final int RUNNING = 1;
    public static final int FIGHTING = 2;
    public static final int COLD = 4;

    private static final float RUNNING_CARBOHYDRATES = 2.0F;
    private static final float FIGHTING_PROTEIN = 3.0F;
    private static final float COLD_FAT = 2.0F;

    private ActivityDecay() {
    }

    private static float raw(Nutrient nutrient, int activity) {
        float multiplier = 1.0F;
        if ((activity & RUNNING) != 0 && nutrient == Nutrients.CARBOHYDRATES) {
            multiplier *= RUNNING_CARBOHYDRATES;
        }
        if ((activity & FIGHTING) != 0 && nutrient == Nutrients.PROTEIN) {
            multiplier *= FIGHTING_PROTEIN;
        }
        if ((activity & COLD) != 0 && nutrient == Nutrients.FAT) {
            multiplier *= COLD_FAT;
        }
        return 1.0F + (multiplier - 1.0F) * NutritionConfig.activityDecayPercent / 100.0F;
    }

    /** This nutrient's share of decay for that activity, scaled so the total stays what it was. */
    public static float multiplier(Nutrient nutrient, int activity) {
        if (activity == 0) {
            return 1.0F;
        }
        Collection<Nutrient> all = Nutrients.all();
        float plain = 0.0F;
        float tilted = 0.0F;
        for (Nutrient n : all) {
            plain += n.decayRate();
            tilted += n.decayRate() * raw(n, activity);
        }
        return raw(nutrient, activity) * plain / tilted;
    }
}
