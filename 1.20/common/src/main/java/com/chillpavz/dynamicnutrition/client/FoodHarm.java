package com.chillpavz.dynamicnutrition.client;

import com.mojang.datafixers.util.Pair;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * Whether eating a food can give a harmful effect, for the food finder's red names. This is where
 * a food keeps its effects on this band. A food that cannot be read counts as harmless: the name
 * is only a hint, and the finder must keep working whatever a mod's food looks like.
 */
final class FoodHarm {

    private static boolean failureLogged;

    private FoodHarm() {
    }

    static boolean isHarmful(ItemStack stack) {
        try {
            return read(stack);
        } catch (RuntimeException | LinkageError e) {
            if (!failureLogged) {
                failureLogged = true;
                Constants.LOG.warn("Could not read a food's effects: {}", e.toString());
            }
            return false;
        }
    }

    private static boolean read(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null) {
            return false;
        }
        for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
            if (effect.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }
}
