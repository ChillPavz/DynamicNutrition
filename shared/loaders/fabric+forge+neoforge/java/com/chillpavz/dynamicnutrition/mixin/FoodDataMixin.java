package com.chillpavz.dynamicnutrition.mixin;

import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.chillpavz.dynamicnutrition.nutrition.Satiety;

/**
 * The hunger drain setting. Changes the argument and nothing else, so it stacks with the other mods
 * that mixin here instead of replacing them.
 *
 * <p><b>EVERY STATIC MEMBER OF A MIXIN MUST BE PRIVATE, AND NOTHING OUTSIDE MAY REFERENCE THIS
 * CLASS.</b> See {@code ItemStackMixin}.
 */
@Mixin(FoodData.class)
public class FoodDataMixin {

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0,
            require = 1)
    private float dynamicnutrition$scaleExhaustion(float exhaustion) {
        return Satiety.scaleExhaustion(exhaustion);
    }
}
