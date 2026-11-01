package com.chillpavz.dynamicnutrition.nutrition;

import com.chillpavz.dynamicnutrition.config.NutritionConfig;

/**
 * When a body short of carbohydrates turns to fat for fuel.
 *
 * <p>It takes a Minecraft day of low carbohydrates with fat to spare, so it is a diet a player
 * commits to rather than a switch. Eating real carbohydrates ends it, and so does running out of
 * fat. While it lasts, decay moves from carbohydrates onto fat ({@link ActivityDecay}), their
 * red-line debuff lifts, and meals keep the player full for longer ({@link #SATIETY}).
 *
 * <p>The whole state is one count of seconds, in the player's saved data.
 */
public final class Ketosis {

    /** One Minecraft day. */
    public static final int ENTER_SECONDS = 1200;

    /** A meal's saturation while in ketosis. */
    public static final float SATIETY = 1.15F;

    /** Fat it takes to start: well above fat's red line, so it never starts on empty. */
    private static final float FAT_TO_START = 20.0F;

    /** How far above their red line carbohydrates must climb to end it. */
    private static final float CARBS_TO_END = 10.0F;

    private Ketosis() {
    }

    public static boolean active(int seconds) {
        return seconds >= ENTER_SECONDS;
    }

    /** The count after one more second at these levels. */
    public static int next(int seconds, float carbs, float carbsRedLine, float fat, float fatRedLine) {
        if (!NutritionConfig.ketosisEnabled) {
            return 0;
        }
        if (active(seconds)) {
            return carbs >= carbsRedLine + CARBS_TO_END || fat < fatRedLine ? 0 : seconds;
        }
        return carbs < carbsRedLine && fat >= FAT_TO_START ? seconds + 1 : 0;
    }
}
