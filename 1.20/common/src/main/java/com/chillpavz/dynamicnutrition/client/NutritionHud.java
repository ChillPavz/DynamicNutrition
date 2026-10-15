package com.chillpavz.dynamicnutrition.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.config.NutritionConfig;
import com.chillpavz.dynamicnutrition.nutrition.Nutrient;
import com.chillpavz.dynamicnutrition.nutrition.Nutrients;
import com.chillpavz.dynamicnutrition.platform.Services;
import com.chillpavz.dynamicnutrition.player.PlayerNutrition;

/**
 * A compact nutrient strip beside the hunger bar.
 *
 * <p><b>Nothing else in this field has a HUD at all</b>, which is most of the argument for having
 * one: a nutrition mod is otherwise invisible until somebody opens a screen, and a screen does not
 * appear in a screenshot of the game being played.
 *
 * <h2>It is drawn from a texture, not from rectangles</h2>
 * The first version filled five plain coloured rectangles. It worked and it read as placeholder,
 * because vanilla's own hearts, drumsticks and bubbles all carry an outline and a bevel and a flat
 * block of colour next to them looks like something failed to load. The sheet is the vanilla
 * experience bar background redrawn with five ten-pixel sections, recoloured once to a neutral dark
 * for the empty track and once per nutrient for the fill.
 *
 * <p><b>Ten pixels per section is the same decision as the ten segments on the screen's bars.</b> A
 * nutrient runs 0 to 100, so one pixel is worth exactly ten points, and a player reading the strip
 * gets a real number rather than an impression.
 *
 * <p>Both loaders hand the same two arguments to {@link #render}, so this is the whole
 * implementation and each loader module contributes only its registration call.
 */
public final class NutritionHud {

    private static final ResourceLocation STRIP =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/strip.png");

    // Must match art/gen_art.py. The generator asserts the source art has this exact geometry, so
    // the two cannot drift without the art build failing first.
    private static final int STRIP_W = 56;
    private static final int STRIP_H = HudPlacement.STRIP_H;
    private static final int SECTION_W = 10;
    private static final int SECTION_PITCH = 11;
    private static final int SECTION_X0 = 1;
    private static final int TEX_W = 56;
    private static final int TEX_H = 30;

    /** Vanilla's own right-hand HUD column: the food bar's right edge. */
    private static final int BAR_RIGHT_FROM_CENTRE = 91;

    /**
     * How to ask whether the player has hidden the HUD with F1, resolved once.
     *
     * <p><b>The two versions in this jar's range answer it differently</b>, which is why this is
     * reflective rather than a call. At 26.1 it is the public field {@code Options.hideGui}. At 26.2
     * that field is gone: the old {@code Gui} class was renamed {@code Hud}, a new {@code Gui} was
     * introduced to own it, and the state became {@code Hud.isHidden()}. One jar cannot name both.
     *
     * <p>Resolved once and cached, and if neither shape is found the answer is "not hidden", so the
     * worst case is the strip staying visible rather than the HUD throwing every frame.
     */
    private static volatile BooleanSupplier hiddenTest;

    private NutritionHud() {
    }

    private static boolean hudHidden(Minecraft client) {
        BooleanSupplier test = hiddenTest;
        if (test == null) {
            test = resolveHiddenTest(client);
            hiddenTest = test;
        }
        try {
            return test.getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    private static BooleanSupplier resolveHiddenTest(Minecraft client) {
        try {                                   // 26.2: Minecraft.gui.hud.isHidden()
            Field hudField = client.gui.getClass().getField("hud");
            Object hud = hudField.get(client.gui);
            Method isHidden = hud.getClass().getMethod("isHidden");
            return () -> {
                try {
                    return Boolean.TRUE.equals(isHidden.invoke(hudField.get(
                            Minecraft.getInstance().gui)));
                } catch (Throwable t) {
                    return false;
                }
            };
        } catch (Throwable ignored) {
            // Not 26.2. Fall through.
        }
        try {                                   // 26.1: Options.hideGui
            Field hideGui = client.options.getClass().getField("hideGui");
            return () -> {
                try {
                    return hideGui.getBoolean(Minecraft.getInstance().options);
                } catch (Throwable t) {
                    return false;
                }
            };
        } catch (Throwable ignored) {
            // Neither shape. Degrade to always visible rather than failing on a render path.
        }
        Constants.LOG.warn("Could not find the hidden HUD flag on this Minecraft version, so the "
                + "nutrient strip will stay visible when the HUD is hidden with F1.");
        return () -> false;
    }

    /**
     * The strip on a loader with NO right-column counter (Fabric, and Forge 1.21.x): vanilla's fixed
     * rows, the bubbles while they show, and the rows of the known HUD mods. See {@link HudPlacement}.
     */
    public static void render(GuiGraphics gfx, float partialTick) {
        LocalPlayer player = visiblePlayer();
        if (player != null) {
            // The same test vanilla uses for the bubble row: eyes in water, or short of air.
            boolean air = player.isEyeInFluid(FluidTags.WATER)
                    || player.getAirSupply() < player.getMaxAirSupply();
            draw(gfx, player, HudPlacement.topFixed(gfx.guiHeight(), air));
        }
    }

    /**
     * The strip on a loader that KEEPS the right-column counter (NeoForge, and Forge 1.20.1): on top
     * of whatever the column already holds, and the counter comes back raised past the strip, so a
     * mod drawn after it stacks above it. Unchanged when the strip is not drawn.
     */
    public static int renderOnCounter(GuiGraphics gfx, int rightHeight) {
        LocalPlayer player = visiblePlayer();
        if (player == null) {
            return rightHeight;
        }
        int column = HudPlacement.correctedCounter(rightHeight, player);
        draw(gfx, player, HudPlacement.topFromCounter(gfx.guiHeight(), column));
        return HudPlacement.counterAfter(column);
    }

    /** The player whose strip is drawn, or null when nothing is drawn at all. */
    private static LocalPlayer visiblePlayer() {
        if (!NutritionConfig.hudEnabled) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return null;
        }
        // F1. A hidden HUD DOES still visit third party elements, on both 26.1 and 26.2, so this
        // has to be tested rather than assumed. See hudHidden.
        if (hudHidden(client)) {
            return null;
        }
        // Match the hunger bar: it is not drawn in creative or spectator, so neither is this.
        if (player.isCreative() || player.isSpectator()) {
            return null;
        }
        return player;
    }

    private static void draw(GuiGraphics gfx, LocalPlayer player, int top) {
        PlayerNutrition nutrition = Services.STORAGE.get(player);

        int right = gfx.guiWidth() / 2 + BAR_RIGHT_FROM_CENTRE + NutritionConfig.hudOffsetX;
        // CLAMPED, never hidden. An offset that pushed the strip off the edge used to make it
        // vanish, so a player nudging the config lost the feature and had no way to tell whether
        // they had broken it or switched it off.
        int left = clamp(right - STRIP_W, 0, gfx.guiWidth() - STRIP_W);
        int y = clamp(top + NutritionConfig.hudOffsetY, 0, gfx.guiHeight() - STRIP_H);

        GuiBlit.texture(gfx, STRIP, left, y, 0, 0, STRIP_W, STRIP_H,
                TEX_W, TEX_H);

        int index = 0;
        for (Nutrient nutrient : Nutrients.all()) {
            int sectionX = SECTION_X0 + index * SECTION_PITCH;
            int filled = Math.round(SECTION_W * Math.min(nutrition.get(nutrient), PlayerNutrition.MAX)
                    / PlayerNutrition.MAX);
            if (filled > 0) {
                // The filled blocks carry the same frame as the empty one, so a clipped fill can
                // never disturb the border; the generator asserts that.
                GuiBlit.texture(gfx, STRIP, left + sectionX, y,
                        sectionX, (index + 1) * STRIP_H, filled, STRIP_H, TEX_W, TEX_H);
            }
            index++;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
