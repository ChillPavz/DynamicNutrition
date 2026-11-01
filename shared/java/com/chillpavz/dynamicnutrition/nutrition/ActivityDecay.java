package com.chillpavz.dynamicnutrition.nutrition;

import java.util.Collection;

import com.chillpavz.dynamicnutrition.config.NutritionConfig;

/**
 * What the player is doing decides WHICH nutrients pay for the food they burn.
 *
 * <p>Running draws on carbohydrates; fighting on protein; the cold on fat; swimming on carbohydrates
 * and fat, because water takes heat as well as effort; working with tools on minerals; heat on
 * vitamins and minerals, which sweat takes; healing on vitamins.
 *
 * <p>The total is unchanged, so this moves decay between nutrients and adds none. That is what keeps
 * the rest of the balance standing. Multipliers chosen on the simulation: a varied diet dips under
 * a green line in heavy running or heat, and reaches no red line. Fighting and tools take the largest
 * figures because a fight is short and tools burn very little food on their own.
 */
public final class ActivityDecay {

    public static final int RUNNING = 1;
    public static final int FIGHTING = 2;
    public static final int COLD = 4;
    public static final int HOT = 8;
    public static final int SWIMMING = 16;
    public static final int LABOUR = 32;
    public static final int HEALING = 64;
    public static final int KETOSIS = 128;

    private ActivityDecay() {
    }

    private static float factor(int activity, int flag, float multiplier) {
        return (activity & flag) != 0 ? multiplier : 1.0F;
    }

    private static float raw(Nutrient nutrient, int activity) {
        float multiplier = 1.0F;
        if (nutrient == Nutrients.CARBOHYDRATES) {
            multiplier = factor(activity, RUNNING, 2.0F) * factor(activity, SWIMMING, 2.0F);
        } else if (nutrient == Nutrients.PROTEIN) {
            multiplier = factor(activity, FIGHTING, 3.0F);
        } else if (nutrient == Nutrients.FAT) {
            multiplier = factor(activity, COLD, 2.0F) * factor(activity, SWIMMING, 2.0F);
        } else if (nutrient == Nutrients.VITAMINS) {
            multiplier = factor(activity, HOT, 2.0F) * factor(activity, HEALING, 2.0F);
        } else if (nutrient == Nutrients.MINERALS) {
            multiplier = factor(activity, LABOUR, 3.0F) * factor(activity, HOT, 1.5F);
        }
        float scaled = 1.0F + (multiplier - 1.0F) * NutritionConfig.activityDecayPercent / 100.0F;
        // Ketosis is its own setting: it holds with activity decay switched off.
        if ((activity & KETOSIS) != 0) {
            if (nutrient == Nutrients.CARBOHYDRATES) {
                return scaled * 0.25F;
            }
            if (nutrient == Nutrients.FAT) {
                return scaled * 2.5F;
            }
        }
        return scaled;
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
