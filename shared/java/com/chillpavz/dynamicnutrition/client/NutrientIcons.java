package com.chillpavz.dynamicnutrition.client;

import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.chillpavz.dynamicnutrition.nutrition.Nutrient;

/** The item shown beside each nutrient on the food finder. */
final class NutrientIcons {

    private NutrientIcons() {
    }

    static ItemStack of(Nutrient nutrient) {
        Item item = switch (nutrient.name()) {
            case "carbohydrates" -> Items.BREAD;
            case "protein" -> Items.COOKED_BEEF;
            case "fat" -> Items.COOKED_PORKCHOP;
            case "vitamins" -> Items.CARROT;
            case "minerals" -> Items.DRIED_KELP;
            default -> Items.APPLE;
        };
        return new ItemStack(item);
    }
}
