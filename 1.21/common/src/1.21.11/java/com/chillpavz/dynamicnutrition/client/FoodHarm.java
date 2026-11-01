package com.chillpavz.dynamicnutrition.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

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
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) {
            return false;
        }
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
                for (MobEffectInstance instance : apply.effects()) {
                    if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
