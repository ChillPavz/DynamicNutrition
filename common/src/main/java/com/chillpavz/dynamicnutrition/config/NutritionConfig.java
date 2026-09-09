package com.chillpavz.dynamicnutrition.config;

/**
 * The mod's tunables, as plain fields.
 *
 * <p><b>This class knows nothing about any config library, and that is the point.</b> The
 * {@code common} module cannot see cloth-config, so the annotated data class lives in each loader
 * module and pushes its values in here. Everything else in the mod reads only this, so there is
 * exactly one place a value can come from however it was set.
 *
 * <p>It also means the mod works with no config library at all: cloth is optional on both loaders,
 * because it is the one dependency that is not ready on the day a Minecraft version ships, and a
 * required dependency would cost the day-zero update.
 */
public final class NutritionConfig {

    // ---------------------------------------------------------------- HUD

    /**
     * Whether the compact nutrient strip is drawn beside the hunger bar.
     *
     * <p>Nothing else in this field has a HUD at all, which is most of the argument for having one:
     * it is the only part of a nutrition mod that shows up in a screenshot.
     */
    public static boolean hudEnabled = true;

    /**
     * Horizontal nudge, in pixels, for players whose HUD is already crowded.
     *
     * <p>The range is deliberately narrow and the renderer clamps on top of it. The first version
     * allowed plus or minus two hundred, which is wider than the space the strip lives in, so a
     * player exploring the config pushed it off the screen and it simply disappeared.
     */
    public static int hudOffsetX = 0;

    /** Vertical nudge, in pixels. Negative is up. Clamped to the screen, like the horizontal one. */
    public static int hudOffsetY = 0;

    // ---------------------------------------------------------------- inventory button

    /**
     * Whether the small button appears in the inventory.
     *
     * <p>Secondary to the keybind by design, so switching it off costs nothing. See
     * {@code NutritionButton} for why: a colliding inventory button is the most reported GUI
     * problem for mods of this kind.
     */
    public static boolean buttonEnabled = true;

    /**
     * Horizontal nudge from the default corner, in pixels.
     *
     * <p>Ranged and clamped to the inventory panel, which is 176 by 166. A button nudged outside it
     * would draw over the player's inventory or off the screen entirely.
     */
    public static int buttonOffsetX = 0;

    /** Vertical nudge from the default corner, in pixels. Clamped to the panel. */
    public static int buttonOffsetY = 0;

    // ---------------------------------------------------------------- tooltips

    /** Whether food tooltips carry a nutrient line. */
    public static boolean tooltipEnabled = true;

    // ---------------------------------------------------------------- variety

    /**
     * Whether eating the same food over and over is worth less.
     *
     * <p>The single most-wanted mechanic in this space, and the fusion nobody has shipped: Spice of
     * Life does repetition without nutrients and every nutrition mod does nutrients without
     * repetition. It deliberately affects NUTRITION only and never hunger, so it composes with
     * vanilla and with any other food mod instead of fighting them.
     */
    public static boolean varietyEnabled = true;

    /**
     * How many recent meals are remembered.
     *
     * <p>Eight, with one repeat free, is what the balance simulation settled on: a rotation of eight
     * or more foods is untouched, six costs about a fifth, three costs a third, and one food forever
     * lands on the floor.
     */
    public static int varietyWindow = 8;

    /** Repeats beyond the first inside the window, each costing this much of the meal. */
    public static int varietyPenaltyPercent = 20;

    /**
     * The least a meal can ever be worth, as a percentage.
     *
     * <p><b>Never zero, and the audit asserts it.</b> The same complaint has followed mods of this
     * kind for years: a mechanic that can reduce a meal to nothing reads as the mod being broken,
     * and a player whose only food source is one crop would be locked out entirely.
     */
    public static int varietyFloorPercent = 30;

    // ---------------------------------------------------------------- gameplay

    /** Whether the status effects are applied. */
    public static boolean effectsEnabled = true;

    /**
     * Hearts taken away by the Malnourished effect, and given by Well Nourished.
     *
     * <p>Hearts rather than health points, because that is the unit a player sees. Zero switches
     * the health part off while leaving the rest of the effect, which is the setting somebody who
     * finds a permanent maximum-health change too harsh actually wants; five is there for the
     * opposite kind of player and is still survivable, since it leaves ten health.
     *
     * <p>These are the reason the health modifier is applied by hand rather than attached to the
     * {@code MobEffect}: a modifier baked into the effect is fixed when the class loads and cannot
     * follow a config value. See {@code NutritionEffects}.
     */
    public static int malnourishedHeartsLost = 1;

    /**
     * Two by default, against one lost when malnourished.
     *
     * <p>Asymmetric on purpose. The penalty is a nudge and the reward is the reason to bother, and
     * the two are not doing the same job.
     */
    public static int wellNourishedHeartsGained = 2;

    /** Whether a status change is announced on the action bar. */
    public static boolean announceStatusChanges = true;

    private NutritionConfig() {
    }
}
