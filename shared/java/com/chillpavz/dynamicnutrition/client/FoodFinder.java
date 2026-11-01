package com.chillpavz.dynamicnutrition.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.DynamicNutrition;
import com.chillpavz.dynamicnutrition.nutrition.FoodRanking;
import com.chillpavz.dynamicnutrition.nutrition.MealHistory;
import com.chillpavz.dynamicnutrition.nutrition.Nutrient;
import com.chillpavz.dynamicnutrition.nutrition.Nutrients;
import com.chillpavz.dynamicnutrition.nutrition.NutritionValues;
import com.chillpavz.dynamicnutrition.platform.Services;
import com.chillpavz.dynamicnutrition.player.PlayerNutrition;

/**
 * The food finder, once for every band: which foods have what, and which the player should eat next.
 *
 * <p>With a nutrient picked the list is that nutrient, most first. With none, which is how the
 * question mark opens it, the list is what this player should eat next.
 *
 * <p>Everything a band draws differently goes through {@link Painter}, implemented by that band's
 * {@code FoodFinderScreen}, which also forwards input. The list comes from the synced table, so it
 * covers what the tooltips cover, and it is rebuilt only when a filter, the table or the bars
 * change: a pack can hold thousands of foods, and this runs inside a frame.
 */
public final class FoodFinder {

    /** The drawing a band has to supply. Colours are ARGB. */
    public interface Painter {
        void panel(int x, int y, int w, int h);

        void text(Component text, int x, int y, int colour);

        void item(ItemStack stack, int x, int y);

        void fill(int x0, int y0, int x1, int y1, int colour);

        void tooltip(List<Component> lines, int x, int y);
    }

    private record Row(Item item, ItemStack stack, Component name, String search, int[] values,
                       float score) {
    }

    private static final String KEY = "screen." + Constants.MOD_ID + ".finder.";

    private static final int MAX_W = 270;
    private static final int MAX_H = 240;
    private static final int PAD = 8;
    private static final int GAP = 4;
    private static final int CHIP = 20;
    private static final int CONTROL_H = 16;
    private static final int ROW_H = 18;
    private static final int ICON = 16;
    private static final int PIP_W = 3;
    private static final int PIP_GAP = 1;
    private static final int PIP_H = 14;
    private static final int SCROLLBAR_W = 3;

    private static final int TEXT = 0xFF3F3F3F;
    private static final int HOVER = 0x30000000;
    private static final int SELECTED = 0x60FFFFFF;
    private static final int PIP_TRACK = 0x30000000;

    private final Consumer<AbstractWidget> widgets;
    private Nutrient nutrient;
    private String search = "";

    private int left;
    private int top;
    private int width;
    private int height;
    private int chipsY;
    private int listY;
    private int visibleRows;
    private int scroll;
    private EditBox searchBox;

    private final List<Row> rows = new ArrayList<>();
    private int builtGeneration = -1;
    private int builtLevels;
    private boolean stale = true;

    /**
     * @param nutrient the bar the player came from, or null for every nutrient
     * @param widgets  adds a widget to the screen
     */
    public FoodFinder(Nutrient nutrient, Consumer<AbstractWidget> widgets) {
        this.nutrient = nutrient;
        this.widgets = widgets;
    }

    /** Called from the screen's init, and so again on every resize. */
    public void layout(int screenW, int screenH, Font font) {
        width = Math.min(MAX_W, screenW - 2 * PAD);
        int headerH = PAD + font.lineHeight + GAP + CHIP + GAP;
        visibleRows = Math.max(1, (Math.min(MAX_H, screenH - 2 * PAD) - headerH - PAD) / ROW_H);
        // The panel fits its rows, so no strip of empty panel is left under the list.
        height = headerH + visibleRows * ROW_H + PAD;
        left = (screenW - width) / 2;
        top = (screenH - height) / 2;
        chipsY = top + PAD + font.lineHeight + GAP;
        listY = chipsY + CHIP + GAP;

        // One row: the search, taking what the nutrients leave of it, then the nutrients.
        searchBox = new EditBox(font, left + PAD, chipsY + (CHIP - CONTROL_H) / 2,
                chipX(0) - GAP - (left + PAD), CONTROL_H, Component.translatable(KEY + "search"));
        searchBox.setHint(Component.translatable(KEY + "search"));
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value.toLowerCase(Locale.ROOT);
            stale = true;
        });
        widgets.accept(searchBox);
        scroll = Math.min(scroll, maxScroll());
    }

    /** The content's top left corner, where a screen puts its back arrow. */
    public int left() {
        return left + PAD;
    }

    public int top() {
        return top + PAD;
    }

    public int right() {
        return left + width - PAD;
    }

    /** Back to what this player should eat next, from the star. */
    public void showBest() {
        nutrient = null;
        stale = true;
        scroll = 0;
    }

    public boolean showingBest() {
        return nutrient == null;
    }

    /** True while the player is typing, so a key that would close the screen goes to the box. */
    public boolean typing() {
        return searchBox != null && searchBox.isFocused();
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows);
    }

    private PlayerNutrition nutrition() {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : Services.STORAGE.get(player);
    }

    /** A fingerprint of the bars, so "best" reorders when the player eats and at no other time. */
    private static int levelsHash(PlayerNutrition nutrition) {
        int hash = nutrition.meals().hashCode();
        for (Nutrient n : Nutrients.all()) {
            hash = 31 * hash + nutrition.display(n);
        }
        return hash;
    }

    private void rebuildIfStale() {
        PlayerNutrition nutrition = nutrition();
        int generation = DynamicNutrition.table().generation();
        int levels = nutrition == null ? 0 : levelsHash(nutrition);
        if (!stale && generation == builtGeneration && levels == builtLevels) {
            return;
        }
        stale = false;
        builtGeneration = generation;
        builtLevels = levels;
        rows.clear();

        List<Nutrient> all = List.copyOf(Nutrients.all());
        float[] current = new float[all.size()];
        float[] targets = new float[all.size()];
        for (int i = 0; i < all.size(); i++) {
            current[i] = nutrition == null ? 0.0F : nutrition.display(all.get(i));
            targets[i] = all.get(i).targetLow();
        }
        for (Map.Entry<Item, NutritionValues> entry : DynamicNutrition.table().snapshot().entrySet()) {
            Item item = entry.getKey();
            NutritionValues values = entry.getValue();
            if (values.isEmpty() || (nutrient != null && values.get(nutrient) <= 0)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            Component name = stack.getHoverName();
            String lower = name.getString().toLowerCase(Locale.ROOT);
            if (!search.isEmpty() && !lower.contains(search)) {
                continue;
            }
            int[] amounts = new int[all.size()];
            for (int i = 0; i < amounts.length; i++) {
                amounts[i] = values.get(all.get(i));
            }
            float multiplier = nutrition == null ? 1.0F : MealHistory.multiplier(nutrition.meals(), item);
            rows.add(new Row(item, stack, name, lower, amounts,
                    FoodRanking.score(current, targets, amounts, multiplier, PlayerNutrition.MAX)));
        }
        rows.sort(comparator(all));
        scroll = Math.min(scroll, maxScroll());
    }

    private Comparator<Row> comparator(List<Nutrient> all) {
        Comparator<Row> byName = Comparator.comparing(Row::search);
        if (nutrient == null) {
            return Comparator.comparingDouble((Row r) -> -r.score()).thenComparing(byName);
        }
        int index = all.indexOf(nutrient);
        return Comparator.comparingInt((Row r) -> -r.values()[index]).thenComparing(byName);
    }

    private static int colour(Nutrient nutrient) {
        return switch (nutrient.name()) {
            case "carbohydrates" -> 0xFFCC6600;
            case "protein" -> 0xFFB20000;
            case "fat" -> 0xFFD8D800;
            case "vitamins" -> 0xFF00C800;
            case "minerals" -> 0xFF0051F2;
            default -> 0xFF808080;
        };
    }

    /** The nutrients sit at the right of the row, the last one against the panel's margin. */
    private int chipX(int index) {
        return left + width - PAD - (Nutrients.count() - index) * CHIP - (Nutrients.count() - 1 - index) * GAP;
    }

    private int chipAt(double mouseX, double mouseY) {
        if (mouseY < chipsY || mouseY >= chipsY + CHIP) {
            return -1;
        }
        for (int i = 0; i < Nutrients.count(); i++) {
            if (mouseX >= chipX(i) && mouseX < chipX(i) + CHIP) {
                return i;
            }
        }
        return -1;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < left + PAD || mouseX >= left + width - PAD || mouseY < listY) {
            return -1;
        }
        int index = ((int) mouseY - listY) / ROW_H;
        return index < visibleRows && scroll + index < rows.size() ? scroll + index : -1;
    }

    /** The whole screen. Bands call this where their panel has to be drawn. */
    public void draw(Painter paint, Font font, Component title, int mouseX, int mouseY) {
        rebuildIfStale();
        paint.panel(left, top, width, height);
        paint.text(title, left + (width - font.width(title)) / 2, top + PAD, TEXT);

        PlayerNutrition nutrition = nutrition();
        int index = 0;
        int hoveredChip = chipAt(mouseX, mouseY);
        for (Nutrient n : Nutrients.all()) {
            drawChip(paint, n, index, nutrition, hoveredChip == index);
            index++;
        }

        int hoveredRow = rowAt(mouseX, mouseY);
        for (int i = 0; i < visibleRows && scroll + i < rows.size(); i++) {
            drawRow(paint, font, rows.get(scroll + i), listY + i * ROW_H, scroll + i == hoveredRow);
        }
        if (rows.isEmpty()) {
            Component none = Component.translatable(KEY + "none");
            paint.text(none, left + (width - font.width(none)) / 2, listY + GAP, TEXT);
        }
        drawScrollbar(paint);

        if (hoveredRow >= 0) {
            paint.tooltip(Screen.getTooltipFromItem(Minecraft.getInstance(), rows.get(hoveredRow).stack()),
                    mouseX, mouseY);
        } else if (hoveredChip >= 0) {
            Nutrient n = List.copyOf(Nutrients.all()).get(hoveredChip);
            paint.tooltip(List.of(Component.translatable(n.translationKey())), mouseX, mouseY);
        }
    }

    private void drawChip(Painter paint, Nutrient n, int index, PlayerNutrition nutrition, boolean hovered) {
        int x = chipX(index);
        if (n == nutrient) {
            paint.fill(x, chipsY, x + CHIP, chipsY + CHIP, SELECTED);
        } else if (hovered) {
            paint.fill(x, chipsY, x + CHIP, chipsY + CHIP, HOVER);
        }
        paint.item(NutrientIcons.of(n), x + (CHIP - ICON) / 2, chipsY);
        int level = nutrition == null ? 0 : nutrition.display(n);
        int filled = Math.round((CHIP - 2) * Math.min(level, PlayerNutrition.MAX) / PlayerNutrition.MAX);
        paint.fill(x + 1, chipsY + CHIP - 3, x + CHIP - 1, chipsY + CHIP - 1, PIP_TRACK);
        paint.fill(x + 1, chipsY + CHIP - 3, x + 1 + filled, chipsY + CHIP - 1, colour(n));
    }

    private void drawRow(Painter paint, Font font, Row row, int y, boolean hovered) {
        int right = left + width - PAD - SCROLLBAR_W - GAP;
        if (hovered) {
            paint.fill(left + PAD, y, right, y + ROW_H, HOVER);
        }
        paint.item(row.stack(), left + PAD, y + 1);

        List<Nutrient> all = List.copyOf(Nutrients.all());
        int pips = all.size() * (PIP_W + PIP_GAP);
        int pipX = right - pips;
        for (int i = 0; i < all.size(); i++) {
            int x = pipX + i * (PIP_W + PIP_GAP);
            int bottom = y + (ROW_H + PIP_H) / 2;
            int tall = Math.min(PIP_H, Math.round(PIP_H * row.values()[i] / (float) PIP_FULL));
            paint.fill(x, bottom - PIP_H, x + PIP_W, bottom, PIP_TRACK);
            paint.fill(x, bottom - tall, x + PIP_W, bottom, colour(all.get(i)));
        }

        int amountW = 0;
        if (nutrient != null) {
            Component amount = Component.literal(String.valueOf(row.values()[all.indexOf(nutrient)]));
            amountW = font.width(amount) + GAP;
            paint.text(amount, pipX - amountW, y + (ROW_H - font.lineHeight) / 2, TEXT);
        }
        int nameX = left + PAD + ICON + GAP;
        int room = pipX - amountW - GAP - nameX;
        paint.text(Component.literal(font.plainSubstrByWidth(row.name().getString(), room)), nameX,
                y + (ROW_H - font.lineHeight) / 2, TEXT);
    }

    /** A food's bar is full here, about the most one food gives of one nutrient in the vanilla table. */
    private static final int PIP_FULL = 32;

    private void drawScrollbar(Painter paint) {
        int x = left + width - PAD - SCROLLBAR_W;
        int trackH = visibleRows * ROW_H;
        paint.fill(x, listY, x + SCROLLBAR_W, listY + trackH, PIP_TRACK);
        if (rows.size() > visibleRows) {
            int thumbH = Math.max(8, trackH * visibleRows / rows.size());
            int thumbY = listY + (trackH - thumbH) * scroll / maxScroll();
            paint.fill(x, thumbY, x + SCROLLBAR_W, thumbY + thumbH, 0xFF606060);
        }
    }

    /** A click on a nutrient chip picks that nutrient, and again clears it. */
    public boolean click(double mouseX, double mouseY) {
        int chip = chipAt(mouseX, mouseY);
        if (chip < 0) {
            return false;
        }
        Nutrient picked = List.copyOf(Nutrients.all()).get(chip);
        nutrient = picked == nutrient ? null : picked;
        stale = true;
        scroll = 0;
        return true;
    }

    public void scrolled(double amount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(amount)));
    }
}
