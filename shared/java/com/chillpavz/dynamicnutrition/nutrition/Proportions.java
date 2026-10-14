package com.chillpavz.dynamicnutrition.nutrition;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * The SHAPE of a food: what share of its worth each nutrient is, indexed in {@link Nutrients#all()}
 * order and summing to 1. A derived food takes the mix of its ingredients' shapes, so bread is shaped
 * like wheat and a beef stew comes out mostly protein with the carrot's vitamins, where an even split
 * made every dish 6/6/6/6/6. The SIZE still comes from the food's own hunger values.
 */
public final class Proportions {

    /**
     * Hunger plus saturation, in the explicit table's units. The table's values come from composition
     * at a serving that tracks the hunger value, which made them about three times what a derived food
     * of the same hunger got: rabbit stew 108, a Farmer's Delight beef stew that fills more 30. Fitted
     * on the 13 vanilla foods the pipeline derives from a recipe, with the table holding only their
     * ingredients: the least error is at 2 to 2.5, and a derived food then lands a little under the real
     * food of the same hunger. Raw foods are left out of the fit on purpose: the game makes raw meat
     * low in hunger and it is dense in nutrients, which no hunger-based size can follow.
     */
    public static final float SIZE_SCALE = 2.5F;

    private Proportions() {
    }

    /** The shape of a food with real values: each value over the total. Null when there are none. */
    public static float[] of(NutritionValues values) {
        float total = values.total();
        if (total <= 0) {
            return null;
        }
        List<Nutrient> all = Nutrients.all();
        float[] shape = new float[all.size()];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = values.get(all.get(i)) / total;
        }
        return shape;
    }

    /** An even shape over a set, which is all a tag or a word says. Null for an empty set. */
    public static float[] uniform(Set<Nutrient> set) {
        if (set.isEmpty()) {
            return null;
        }
        List<Nutrient> all = Nutrients.all();
        float[] shape = new float[all.size()];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = set.contains(all.get(i)) ? 1.0F / set.size() : 0.0F;
        }
        return shape;
    }

    /** Add {@code weight} of a shape into a running sum. A null shape adds nothing. */
    public static void add(float[] sum, float[] shape, float weight) {
        if (shape == null || weight <= 0) {
            return;
        }
        for (int i = 0; i < sum.length; i++) {
            sum[i] += shape[i] * weight;
        }
    }

    /** A running sum scaled back to a shape. Null when nothing was added. */
    public static float[] normalised(float[] sum) {
        float total = 0;
        for (float v : sum) {
            total += v;
        }
        if (total <= 0) {
            return null;
        }
        float[] shape = new float[sum.length];
        for (int i = 0; i < sum.length; i++) {
            shape[i] = sum[i] / total;
        }
        return shape;
    }

    /** An empty running sum. */
    public static float[] sum() {
        return new float[Nutrients.all().size()];
    }

    /**
     * A total spread by a shape. A share that rounds to nothing is dropped: a dish sweetened with a
     * spoon of honey is not a mineral source.
     */
    public static NutritionValues values(float[] shape, float total) {
        if (shape == null || total <= 0) {
            return NutritionValues.EMPTY;
        }
        List<Nutrient> all = Nutrients.all();
        Map<Nutrient, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < shape.length; i++) {
            int v = Math.round(total * shape[i]);
            if (v > 0) {
                out.put(all.get(i), v);
            }
        }
        return NutritionValues.of(out);
    }
}
