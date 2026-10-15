package com.chillpavz.dynamicnutrition.client;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * NeoForge's right-hand HUD counter ({@code rightHeight}): how far up the right column is already
 * taken. Vanilla's food and air layers, and every HUD mod that plays by NeoForge's rules, start
 * their row at {@code guiHeight - rightHeight} and raise it by ten, so reading it and raising it is
 * how the strip stacks above all of them without knowing any of them. See {@link HudPlacement}.
 *
 * <p><b>Reflective, and resolved once</b>, because the counter moved: at 26.2 the old {@code Gui}
 * class became {@code Hud} and is reached through {@code Minecraft.gui.hud}, while every earlier
 * NeoForge keeps it on {@code Minecraft.gui} itself. One 26.x jar has to find both. If neither is
 * found the strip falls back to the fixed rows, the way it is placed on Fabric.
 */
public final class RightColumn {

    private static volatile Field owner;          // Minecraft.gui.hud at 26.2+, else null
    private static volatile Field counter;
    private static volatile boolean resolved;

    private RightColumn() {
    }

    /** Whether the counter was found; when false, use the fixed placement instead. */
    public static boolean available() {
        if (!resolved) {
            resolve();
        }
        return counter != null;
    }

    public static int get() {
        try {
            return counter.getInt(target());
        } catch (Throwable t) {
            return HudPlacement.FOOD_ROW_UP + HudPlacement.ROW;
        }
    }

    public static void set(int value) {
        try {
            counter.setInt(target(), value);
        } catch (Throwable t) {
            // The strip is drawn already; a mod above it just will not know it is there.
        }
    }

    private static Object target() throws IllegalAccessException {
        Object gui = Minecraft.getInstance().gui;
        Field hud = owner;
        return hud == null ? gui : hud.get(gui);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        Object gui = Minecraft.getInstance().gui;
        try {
            Field hud = gui.getClass().getField("hud");
            counter = hud.getType().getField("rightHeight");
            owner = hud;
        } catch (Throwable notHud) {
            try {
                counter = gui.getClass().getField("rightHeight");
            } catch (Throwable neither) {
                Constants.LOG.warn("Could not find NeoForge's right-hand HUD counter, so the nutrient "
                        + "strip is placed by fixed rows and may overlap another mod's bar.");
            }
        }
        resolved = true;
    }
}
