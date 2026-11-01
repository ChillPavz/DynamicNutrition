package com.chillpavz.dynamicnutrition.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * Every textured draw the mod makes, in one place, so the screen, the button and the HUD strip
 * never name a {@code GuiGraphics} overload themselves.
 *
 * <p>On this band {@code blitSprite} takes a GUI sprite by name, and a float UV {@code blit} draws a
 * region of a standalone texture, which is also how the panel's nine slice is drawn. The seam exists
 * so that the newer bands, where every one of these calls changed shape, only ever had to change
 * this file.
 */
public final class GuiBlit {

    /** Set after the first failed sprite draw, so a broken lookup logs once rather than per frame. */
    private static volatile boolean spriteFailureLogged;

    private GuiBlit() {
    }

    /**
     * A region of a standalone texture at its own size, the same arguments as vanilla's ten argument
     * {@code blit}: {@code (u, v)} is the region's top left in pixels on a sheet of
     * {@code texW} by {@code texH}.
     */
    public static void texture(GuiGraphics gfx, ResourceLocation texture, int x, int y,
                               int u, int v, int w, int h, int texW, int texH) {
        gfx.blit(texture, x, y, (float) u, (float) v, w, h, texW, texH);
    }

    /** A whole GUI sprite stretched to the given size. Missing sprites draw as vanilla's missing texture. */
    public static void sprite(GuiGraphics gfx, ResourceLocation sprite, int x, int y, int w, int h) {
        draw(gfx, sprite, x, y, w, h);
    }

    /**
     * A nine slice of a small standalone texture ({@code spriteW} by {@code spriteH}, border
     * {@code border}), stretched to the given size. Vanilla's sprite nine slice on this band TILES,
     * one immediate draw per tile, which took the screen from 1200 fps to 42, so it is not used.
     * The border is clamped to half the drawn size.
     *
     * <p><b>The edges and the centre are STRETCHED.</b> That is only pixel identical for a texture
     * whose edges and centre are one colour along their length, which is true of the panel this is
     * used for. Do not reuse it for a patterned border.
     */
    public static void nineSlice(GuiGraphics gfx, ResourceLocation texture, int x, int y, int w,
                                 int h, int spriteW, int spriteH, int border) {
        if (w <= 0 || h <= 0) {
            return;
        }
        try {
            int bx = Math.min(border, w / 2);
            int by = Math.min(border, h / 2);
            int[] dx = {x, x + bx, x + w - bx, x + w};
            int[] dy = {y, y + by, y + h - by, y + h};
            int[] sx = {0, bx, spriteW - bx, spriteW};
            int[] sy = {0, by, spriteH - by, spriteH};
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    int dw = dx[i + 1] - dx[i];
                    int dh = dy[j + 1] - dy[j];
                    int sw = sx[i + 1] - sx[i];
                    int sh = sy[j + 1] - sy[j];
                    if (dw <= 0 || dh <= 0 || sw <= 0 || sh <= 0) {
                        continue;
                    }
                    gfx.blit(texture, dx[i], dy[j], dw, dh, (float) sx[i], (float) sy[j], sw, sh,
                            spriteW, spriteH);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            if (!spriteFailureLogged) {
                spriteFailureLogged = true;
                Constants.LOG.error("Could not draw {}, so it will not be drawn", texture, e);
            }
        }
    }

    /**
     * A sprite draw that cannot take the frame down. A missing sprite NAME is not a failure: the
     * atlas answers with its missing texture, as vanilla's draw would. A throw means the lookup
     * itself broke, and then the draw is skipped: an absent icon is a cosmetic fault, a throw on a
     * render path is a crash.
     */
    private static void draw(GuiGraphics gfx, ResourceLocation sprite, int x, int y, int w, int h) {
        try {
            gfx.blitSprite(sprite, x, y, w, h);
        } catch (RuntimeException | LinkageError e) {
            if (!spriteFailureLogged) {
                spriteFailureLogged = true;
                Constants.LOG.error("Could not draw the GUI sprite {}, so it will not be drawn",
                        sprite, e);
            }
        }
    }
}
