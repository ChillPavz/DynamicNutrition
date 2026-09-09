package com.chillpavz.dynamicnutrition.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.effect.NutritionEffects;
import com.chillpavz.dynamicnutrition.effect.NutritionMobEffect;
import com.chillpavz.dynamicnutrition.nutrition.Nutrient;
import com.chillpavz.dynamicnutrition.platform.Services;
import com.chillpavz.dynamicnutrition.player.PlayerNutrition;

/**
 * Puts the deficient nutrients into the malnourished effect's displayed name.
 *
 * <p>Client only, and in its own class for that reason: it touches {@code Minecraft}, and both
 * loaders reach it from wiring that only runs on the client dist.
 *
 * <p>Reading the LOCAL player is correct rather than a shortcut. The effect object is one shared
 * registry entry, so it has no player of its own, and the only place a name is ever drawn is the
 * viewing player's own inventory and HUD.
 */
public final class NutritionEffectDetails {

    private NutritionEffectDetails() {
    }

    /** Called once from each loader's client init. */
    public static void install() {
        NutritionMobEffect.setDetailSupplier(NutritionEffectDetails::detailFor);
    }

    private static Component detailFor(MobEffect effect) {
        if (effect != NutritionEffects.MALNOURISHED) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return null;
        }
        PlayerNutrition nutrition = Services.STORAGE.get(client.player);
        List<Nutrient> low = nutrition.deficient();
        if (low.isEmpty()) {
            return null;
        }
        MutableComponent names = Component.empty();
        for (int i = 0; i < low.size(); i++) {
            if (i > 0) {
                names.append(Component.literal(", "));
            }
            names.append(Component.translatable(low.get(i).translationKey())
                    .withColor(low.get(i).textColor()));
        }
        return Component.translatable("effect." + Constants.MOD_ID + ".detail", names);
    }
}
