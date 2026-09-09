package com.chillpavz.dynamicnutrition.effect;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric registration for the two status effects. */
public final class FabricNutritionEffects {

    private FabricNutritionEffects() {
    }

    public static void register() {
        // registerForHolder rather than register: a MobEffectInstance needs a Holder, and taking it
        // straight from the registration is one call instead of a registration plus a lookup.
        NutritionEffects.bind(
                Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                        NutritionEffects.MALNOURISHED_KEY, NutritionEffects.MALNOURISHED),
                Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                        NutritionEffects.WELL_NOURISHED_KEY, NutritionEffects.WELL_NOURISHED));
    }
}
