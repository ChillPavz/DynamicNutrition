package com.chillpavz.dynamicnutrition.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * The two small icon buttons on the mod's screens, the back arrow and the question mark, both drawn
 * from a sheet of two frames side by side, normal then hover, with a 1px gap.
 *
 * <p>Icon only: no button frame, because a bevel around a ten by eight icon would leave no room for
 * it. Vanilla's own recipe book button is built the same way. Both frames keep the dark outline, so
 * contrast never drops on hover; only the interior lights up.
 */
final class IconButton extends Button {

    private final ResourceLocation sheet;
    private BooleanSupplier lit = () -> false;

    private IconButton(int x, int y, int w, int h, ResourceLocation sheet, Component tip, Button.OnPress onPress) {
        super(x, y, w, h, tip, onPress, DEFAULT_NARRATION);
        this.sheet = sheet;
        setTooltip(Tooltip.create(tip));
    }

    private static final int ARROW_H = 8;
    private static final int HELP_H = 9;
    private static final int STAR_W = 11;
    private static final int STAR_H = 10;
    private static final int ARROW_W = 10;
    private static final int HELP_W = 7;
    private static final ResourceLocation ARROWS =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/arrows.png");
    private static final ResourceLocation HELP =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/help.png");
    private static final ResourceLocation STAR =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/star.png");

    static IconButton back(int x, int y, Button.OnPress onPress) {
        return new IconButton(x, y, ARROW_W, ARROW_H, ARROWS, Component.translatable("gui.back"), onPress);
    }

    /** Placed by its right edge, so the panel's right margin matches the arrow's left one. */
    static IconButton help(int right, int y, Component tip, Button.OnPress onPress) {
        return new IconButton(right - HELP_W, y - 1, HELP_W, HELP_H, HELP, tip, onPress);
    }

    /** Stays lit while {@code lit} holds, so the star also says what the list is showing. */
    static IconButton star(int right, int y, Component tip, BooleanSupplier lit, Button.OnPress onPress) {
        IconButton button = new IconButton(right - STAR_W, y - 1, STAR_W, STAR_H, STAR, tip, onPress);
        button.lit = lit;
        return button;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int u = isHovered() || lit.getAsBoolean() ? getWidth() + 1 : 0;
        GuiBlit.texture(gfx, sheet, getX(), getY(), u, 0, getWidth(), getHeight(), getWidth() * 2 + 1, getHeight());
    }
}
