package com.chillpavz.dynamicnutrition.event;

import java.lang.reflect.Method;

import net.minecraft.world.entity.player.Player;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.platform.Services;

/**
 * Tough As Nails' body temperature, by its public API and by reflection, because that mod is
 * optional. Checked against 9.2.0.171 (1.20.1) and 10.1.0.13 (1.21.1) on each of their loaders.
 * Anything unexpected turns this off with one log line, and the biome decides instead.
 */
final class TemperatureCompat {

    private static final String HELPER = "toughasnails.api.temperature.TemperatureHelper";

    private static boolean looked;
    private static Method forPlayer;
    private static Method enabled;

    private TemperatureCompat() {
    }

    /** The player's temperature level by name, or null when the mod is absent or has it switched off. */
    static String level(Player player) {
        if (!looked) {
            looked = true;
            find();
        }
        if (forPlayer == null) {
            return null;
        }
        try {
            if (!(boolean) enabled.invoke(null)) {
                return null;
            }
            return ((Enum<?>) forPlayer.invoke(null, player)).name();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Constants.LOG.warn("Could not read Tough As Nails' temperature, using biomes: {}", e.toString());
            forPlayer = null;
            return null;
        }
    }

    private static void find() {
        if (!Services.PLATFORM.isModLoaded("toughasnails")) {
            return;
        }
        try {
            Class<?> helper = Class.forName(HELPER);
            enabled = helper.getMethod("isTemperatureEnabled");
            forPlayer = helper.getMethod("getTemperatureForPlayer", Player.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            Constants.LOG.warn("Tough As Nails has no temperature API here, using biomes: {}", e.toString());
            forPlayer = null;
        }
    }
}
