package com.chillpavz.dynamicnutrition.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import com.chillpavz.dynamicnutrition.DynamicNutrition;
import com.chillpavz.dynamicnutrition.config.NutritionConfig;
import com.chillpavz.dynamicnutrition.nutrition.MealHistory;
import com.chillpavz.dynamicnutrition.nutrition.NutritionValues;
import com.chillpavz.dynamicnutrition.platform.Services;

/**
 * What a meal would really give this player, for the ghost marks on the strip and the bars. Read
 * from the synced table every frame, which is a map lookup, and after the repeat penalty, so the
 * mark lands where the bar will.
 */
public final class MealPreview {

    private MealPreview() {
    }

    /** The food in the main hand, then the off hand. Empty when neither is one, or it is switched off. */
    public static NutritionValues held(Player player) {
        if (!NutritionConfig.mealPreviewEnabled || player == null) {
            return NutritionValues.EMPTY;
        }
        NutritionValues main = of(player, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        return main.isEmpty() ? of(player, player.getItemInHand(InteractionHand.OFF_HAND).getItem()) : main;
    }

    public static NutritionValues of(Player player, Item item) {
        NutritionValues values = DynamicNutrition.table().cached(item);
        if (values == null || values.isEmpty()) {
            return NutritionValues.EMPTY;
        }
        return values.scaled(MealHistory.multiplier(Services.STORAGE.get(player).meals(), item));
    }
}
