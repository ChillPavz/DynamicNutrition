package com.chillpavz.dynamicnutrition.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntSupplier;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.platform.Services;

/**
 * Where the nutrient strip goes in the right-hand HUD column, so it never lands on another mod's
 * bar. Every band and loader asks this class; only the drawing stays in {@code NutritionHud}.
 *
 * <h2>Two kinds of loader</h2>
 * <b>NeoForge, and Forge 1.20.1, keep a shared counter</b> ({@code rightHeight}): every row drawn in
 * the right column starts at {@code guiHeight - rightHeight} and adds ten to it. The strip is
 * registered after the air layer, sits on top of whatever the counter says is there, and raises the
 * counter by its own height, so mods that read it stack above us and mods drawn before us stay below.
 * No per-mod knowledge is needed there, and none is used.
 *
 * <p><b>Fabric, and Forge 1.21.x, have no counter</b> (Forge 1.21.x ships {@code ForgeGui} in its
 * sources jar only; the compiled jar has none). Each HUD mod mixes into vanilla at a fixed row, so
 * the strip has to know, per mod, how many rows that mod adds above the food bar. Those are the
 * {@link #RULES}, each read from the mod's own jar. A mod whose bar cannot be asked whether it is on
 * counts as on, because its bar is on by default and a gap reads better than an overlap.
 */
public final class HudPlacement {

    /** Vanilla's row pitch in the right column: nine-pixel icons, one pixel apart. */
    public static final int ROW = 10;

    /** The food bar's top, counted up from the bottom of the screen. */
    public static final int FOOD_ROW_UP = 39;

    /** The strip's height; must match art/gen_art.py, as NutritionHud's other geometry does. */
    public static final int STRIP_H = 5;

    /**
     * The gap below the strip.
     *
     * <p>Vanilla's rows are ten pixels apart and its icons are nine tall, so ONE pixel is the
     * spacing the hotbar, the hunger bar and the bubbles all use between themselves. Two reads
     * better here, because the strip is a different kind of thing from the row below it rather than
     * another row of the same gauge, and at one pixel it looked joined to the hunger bar.
     */
    public static final int ROW_GAP = 2;

    /**
     * Mods that draw a row above the food bar at a FIXED position on loaders with no counter.
     * Read from the jars (13 Oct 2026); the maintainer notes list the bytecode each one rests on.
     * Order does not matter: the rows add up, so two of these mods together leave the strip above
     * both, at worst with one empty row when they overlap each other.
     */
    static final Rule[] RULES = {
        // Tough As Nails (GlitchCore's AIR event): thirst on the air row, bubbles one row higher.
        new Rule("toughasnails", 1, "toughasnails.init.ModConfig", "thirst", "enableThirst"),
        // Dehydration: thirst at height - 49, and the bubbles' heart-row count raised by one.
        new Rule("dehydration", 1, null, null, null),
        // Homeostatic: its water bar one row above food and bubbles, from its own counter.
        new Rule("homeostatic", 1, null, null, null),
        // Survive: thirst and energy, two rows above food (measured on NeoForge, where it does not
        // raise the counter either; see correctedCounter).
        new Rule("survive", 2, null, null, null),
    };

    private static volatile int[] activeRules;

    private static final Loaded SURVIVE = new Loaded("survive");

    private HudPlacement() {
    }

    /**
     * The strip's top edge on a loader with a counter, before the player's own offset: its bottom
     * {@link #ROW_GAP} above the top of the highest row already drawn, which is
     * {@code guiHeight - rightHeight + ROW} because each row added ten after starting at the old value.
     */
    public static int topFromCounter(int guiHeight, int rightHeight) {
        return guiHeight - rightHeight + ROW - ROW_GAP - STRIP_H;
    }

    /**
     * The counter as the strip should read it: what the loader holds, corrected for mods that draw
     * further up than they claim. Measured in game (14 Oct 2026, NeoForge 1.21.1):
     * <ul>
     * <li><b>Iron's Spells</b>, mana bar on its default "Hunger" anchor: the bar's top is 8 pixels
     * (plus its own Y offset) above the counter it read, but it raises the counter by only 10, so it
     * reaches 8 pixels into the next slot. Counted only while its own test says the bar shows.</li>
     * <li><b>Survive</b>: draws its thirst and energy rows above the food bar and does not raise the
     * counter at all, so the strip stays above food plus two rows while Survive is loaded.</li>
     * </ul>
     */
    public static int correctedCounter(int rightHeight, Object player) {
        int corrected = rightHeight + IronsSpells.overhang(player);
        if (SURVIVE.loaded()) {
            corrected = Math.max(corrected, FOOD_ROW_UP + ROW * 3);
        }
        return corrected;
    }

    /** The counter after the strip: the next row's icons end one pixel above the strip's top. */
    public static int counterAfter(int rightHeight) {
        return rightHeight + STRIP_H + ROW_GAP;
    }

    /**
     * The strip's top edge on a loader with no counter, before the player's own offset. Starts at
     * the food bar, one row up for the air bubbles while they show, one per known mod's bar, and
     * AsteorBar's own counter when that mod has replaced the vanilla bars.
     */
    public static int topFixed(int guiHeight, boolean airShowing) {
        int up = FOOD_ROW_UP + ROW * (fixedRows() + (airShowing ? 1 : 0));
        up = Math.max(up, AsteorBar.rightUp());
        return guiHeight - up - ROW_GAP - STRIP_H;
    }

    /** How many rows the loaded mods in {@link #RULES} add above the food bar right now. */
    static int fixedRows() {
        int[] loaded = activeRules;
        if (loaded == null) {
            loaded = loadedRules();
            activeRules = loaded;
        }
        int rows = 0;
        for (int index : loaded) {
            if (RULES[index].drawsNow()) {
                rows += RULES[index].rows;
            }
        }
        return rows;
    }

    private static int[] loadedRules() {
        int count = 0;
        int[] found = new int[RULES.length];
        for (int i = 0; i < RULES.length; i++) {
            try {
                if (Services.PLATFORM.isModLoaded(RULES[i].modId)) {
                    found[count++] = i;
                    Constants.LOG.info("Nutrient strip: {} is loaded, so the strip sits one row higher "
                            + "while its bar is on.", RULES[i].modId);
                }
            } catch (Throwable t) {
                // A loader that cannot answer: leave the rule out rather than fail on a render path.
            }
        }
        int[] loaded = new int[count];
        System.arraycopy(found, 0, loaded, 0, count);
        return loaded;
    }

    /**
     * One mod's row. When the mod has a switch for its bar ({@code configClass.holder.flag}, a
     * static holder object with a boolean field), that switch is read every frame; when it has none
     * or the switch cannot be found, the bar counts as on.
     */
    static final class Rule {
        final String modId;
        final int rows;
        private final String configClass;
        private final String holder;
        private final String flag;
        private volatile Field holderField;
        private volatile Field flagField;
        private volatile boolean resolved;

        Rule(String modId, int rows, String configClass, String holder, String flag) {
            this.modId = modId;
            this.rows = rows;
            this.configClass = configClass;
            this.holder = holder;
            this.flag = flag;
        }

        boolean drawsNow() {
            if (configClass == null) {
                return true;
            }
            if (!resolved) {
                resolve();
            }
            Field h = holderField;
            Field f = flagField;
            if (h == null || f == null) {
                return true;
            }
            try {
                Object config = h.get(null);
                return config == null || f.getBoolean(config);
            } catch (Throwable t) {
                return true;
            }
        }

        private synchronized void resolve() {
            if (resolved) {
                return;
            }
            try {
                Field h = Class.forName(configClass).getField(holder);
                holderField = h;
                flagField = h.getType().getField(flag);
            } catch (Throwable t) {
                Constants.LOG.warn("Nutrient strip: could not read {}'s bar switch, so its bar is "
                        + "counted as always on.", modId);
            }
            resolved = true;
        }
    }

    /** Whether one mod is loaded, asked once. A loader that cannot answer counts as not loaded. */
    static final class Loaded {
        private final String modId;
        private volatile Boolean answer;

        Loaded(String modId) {
            this.modId = modId;
        }

        boolean loaded() {
            Boolean a = answer;
            if (a == null) {
                try {
                    a = Services.PLATFORM.isModLoaded(modId);
                } catch (Throwable t) {
                    a = false;
                }
                answer = a;
            }
            return a;
        }
    }

    /**
     * Iron's Spells' mana bar on a loader with a counter (see {@link #correctedCounter}). Everything
     * is read from the mod itself: whether the bar shows ({@code ManaBarOverlay.shouldShowManaBar}),
     * its anchor and its Y offset ({@code ClientConfigs}). Anything that cannot be read means no
     * correction, and the strip sits where the counter says.
     */
    static final class IronsSpells {
        static final int OVERHANG = 8;
        private static final Loaded LOADED = new Loaded("irons_spellbooks");
        private static volatile Method shows;
        private static volatile Object anchor;
        private static volatile Object yOffset;
        private static volatile boolean resolved;

        private IronsSpells() {
        }

        static int overhang(Object player) {
            if (!LOADED.loaded()) {
                return 0;
            }
            if (!resolved) {
                resolve();
            }
            try {
                if (shows == null || !Boolean.TRUE.equals(shows.invoke(null, player))) {
                    return 0;
                }
                Object where = anchor.getClass().getMethod("get").invoke(anchor);
                if (!"Hunger".equals(String.valueOf(where))) {
                    return 0;
                }
                Object offset = yOffset.getClass().getMethod("get").invoke(yOffset);
                return Math.max(0, OVERHANG + ((Number) offset).intValue());
            } catch (Throwable t) {
                return 0;
            }
        }

        private static synchronized void resolve() {
            if (resolved) {
                return;
            }
            try {
                for (Method m : Class.forName("io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay")
                        .getMethods()) {
                    if (m.getName().equals("shouldShowManaBar") && m.getParameterCount() == 1) {
                        shows = m;
                    }
                }
                Class<?> configs = Class.forName("io.redspace.ironsspellbooks.config.ClientConfigs");
                anchor = configs.getField("MANA_BAR_ANCHOR").get(null);
                yOffset = configs.getField("MANA_BAR_Y_OFFSET").get(null);
            } catch (Throwable t) {
                shows = null;
                Constants.LOG.warn("Nutrient strip: could not read Iron's Spells' mana bar settings, so "
                        + "the strip may overlap that bar.");
            }
            resolved = true;
        }
    }

    /**
     * AsteorBar replaces the vanilla bars with its own, laid out from its own public counter
     * ({@code Overlays.rightHeight}, pixels up from the bottom like vanilla's). On a loader with no
     * counter of its own that is the only way to know how high its right column reaches.
     */
    static final class AsteorBar {
        private static volatile IntSupplier counter;

        private AsteorBar() {
        }

        static int rightUp() {
            IntSupplier c = counter;
            if (c == null) {
                c = resolve();
                counter = c;
            }
            try {
                return c.getAsInt();
            } catch (Throwable t) {
                return 0;
            }
        }

        private static IntSupplier resolve() {
            try {
                if (!Services.PLATFORM.isModLoaded("asteorbar")) {
                    return () -> 0;
                }
                Field right = Class.forName("com.afoxxvi.asteorbar.overlay.Overlays")
                        .getField("rightHeight");
                Constants.LOG.info("Nutrient strip: AsteorBar is loaded, so the strip sits above its "
                        + "right-hand bars.");
                return () -> {
                    try {
                        return right.getInt(null);
                    } catch (Throwable t) {
                        return 0;
                    }
                };
            } catch (Throwable t) {
                return () -> 0;
            }
        }
    }
}
