package com.chillpavz.dynamicnutrition.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.nutrition.Nutrient;

/**
 * This band's half of the food finder: its drawing calls and its input events. Everything else is
 * in {@link FoodFinder}.
 */
public class FoodFinderScreen extends Screen {

    private static final ResourceLocation PANEL =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/sprites/panel.png");

    /** The panel sprite's size and nine slice border. These MUST match panel.png.mcmeta. */
    private static final int PANEL_SPRITE = 8;
    private static final int PANEL_BORDER = 3;

    private final Screen parent;
    private final FoodFinder finder;

    /** @param nutrient the bar the player came from, or null for every nutrient */
    public FoodFinderScreen(Screen parent, Nutrient nutrient) {
        super(Component.translatable("screen." + Constants.MOD_ID + ".finder.title"));
        this.parent = parent;
        this.finder = new FoodFinder(nutrient, this::addRenderableWidget);
    }

    @Override
    protected void init() {
        super.init();
        finder.layout(this.width, this.height, this.font);
        addRenderableWidget(IconButton.back(finder.left(), finder.top(), b -> this.onClose()));
        addRenderableWidget(IconButton.star(finder.right(), finder.top(),
                Component.translatable("screen." + Constants.MOD_ID + ".finder.best"),
                finder::showingBest, b -> finder.showBest()));
    }

    private FoodFinder.Painter painter(GuiGraphics gfx) {
        return new FoodFinder.Painter() {
            @Override
            public void panel(int x, int y, int w, int h) {
                GuiBlit.nineSlice(gfx, PANEL, x, y, w, h, PANEL_SPRITE, PANEL_SPRITE, PANEL_BORDER);
            }

            @Override
            public void text(Component text, int x, int y, int colour) {
                gfx.drawString(font, text, x, y, colour, false);
            }

            @Override
            public void item(ItemStack stack, int x, int y) {
                gfx.renderItem(stack, x, y);
            }

            @Override
            public void fill(int x0, int y0, int x1, int y1, int colour) {
                gfx.fill(x0, y0, x1, y1, colour);
            }

            @Override
            public void tooltip(List<Component> lines, int x, int y) {
                setTooltipForNextRenderPass(lines.stream().map(Component::getVisualOrderText).toList());
            }
        };
    }

    /** The list FIRST, then super, which draws the widgets: as on {@code NutritionScreen}. */
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        finder.draw(painter(gfx), this.font, this.title, mouseX, mouseY);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return finder.click(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        finder.scrolled(amount);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!finder.typing() && this.minecraft != null
                && (this.minecraft.options.keyInventory.matches(keyCode, scanCode)
                || DynamicNutritionKeys.OPEN_SCREEN.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    /** A list to read, so it should not freeze the world. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
