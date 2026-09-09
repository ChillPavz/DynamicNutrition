package com.chillpavz.dynamicnutrition.effect;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.chillpavz.dynamicnutrition.Constants;

/** NeoForge registration for the two status effects. Registries go on the MOD bus. */
public final class NeoForgeNutritionEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Constants.MOD_ID);

    private static final Supplier<MobEffect> MALNOURISHED =
            EFFECTS.register("malnourished", () -> NutritionEffects.MALNOURISHED);
    private static final Supplier<MobEffect> WELL_NOURISHED =
            EFFECTS.register("well_nourished", () -> NutritionEffects.WELL_NOURISHED);

    private NeoForgeNutritionEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    /**
     * Hand the holders to the common code, once the registry has been filled.
     *
     * <p>Separate from {@link #register} because the holders do not exist until registration has
     * actually run, and touching them earlier would resolve to nothing.
     */
    public static void bind() {
        MALNOURISHED.get();
        WELL_NOURISHED.get();
        NutritionEffects.bind(
                BuiltInRegistries.MOB_EFFECT.getOrThrow(NutritionEffects.MALNOURISHED_KEY),
                BuiltInRegistries.MOB_EFFECT.getOrThrow(NutritionEffects.WELL_NOURISHED_KEY));
    }
}
