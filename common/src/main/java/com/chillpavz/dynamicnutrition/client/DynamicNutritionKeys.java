package com.chillpavz.dynamicnutrition.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * The keybind, built here so both loaders register the same object.
 *
 * <p><b>The KEYBIND is the primary way in and the inventory button is secondary.</b> That is not a
 * style choice: an inventory button colliding with another mod's is far and away the most reported
 * GUI problem for mods of this kind, and the usual answer to such a report is "there is a config
 * option to move it, and the keybind opens it anyway". Leading with the key makes a collision
 * cosmetic rather than a lockout.
 *
 * <p>N is the default because it is what players of similar mods already press.
 */
public final class DynamicNutritionKeys {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    public static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key." + Constants.MOD_ID + ".open_screen",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            CATEGORY);

    private DynamicNutritionKeys() {
    }
}
