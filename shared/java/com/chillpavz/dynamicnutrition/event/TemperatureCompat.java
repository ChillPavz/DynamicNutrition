package com.chillpavz.dynamicnutrition.event;

import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.platform.Services;

/**
 * Body temperature from a survival mod, by each mod's public API and by reflection, because every
 * one of them is optional. They all count clothing, so warm and cool armour work without being named.
 * Anything unexpected turns this off with one log line, and the biome decides instead.
 *
 * <p>Read in the jars: Tough As Nails 9.2 and 10.1, Cold Sweat 2.4.3, Survive 10.2, Thermoo 4.8 (behind
 * Frostiful and Scorchful, and Thermoo alone has nothing warming or cooling a player, so it needs
 * one of them), Legendary Survival Overhaul 2.4.7.
 */
public final class TemperatureCompat {

    public static final int NEUTRAL = 0;
    public static final int COLD = 1;
    public static final int HOT = 2;

    /** Cold Sweat's body temperature runs from -100 to 100, where its damage starts. */
    private static final double COLD_SWEAT_LIMIT = 50.0;

    /** A share of Thermoo's limit, which is where freezing or overheating does its worst. */
    private static final float THERMOO_LIMIT = 0.25F;

    private interface Reader {
        int read(Player player) throws ReflectiveOperationException;
    }

    private static boolean looked;
    private static Reader reader;
    private static String source;

    private TemperatureCompat() {
    }

    /** Finds the mod, once. Called at server start so the log says which one is in charge. */
    public static void find() {
        if (looked) {
            return;
        }
        looked = true;
        try {
            if (Services.PLATFORM.isModLoaded("toughasnails")) {
                source = "Tough As Nails";
                reader = toughAsNails();
            } else if (Services.PLATFORM.isModLoaded("cold_sweat")) {
                source = "Cold Sweat";
                reader = coldSweat();
            } else if (Services.PLATFORM.isModLoaded("survive")) {
                source = "Survive";
                reader = survive();
            } else if (Services.PLATFORM.isModLoaded("legendarysurvivaloverhaul")) {
                source = "Legendary Survival Overhaul";
                reader = legendarySurvival();
            } else if (Services.PLATFORM.isModLoaded("frostiful") || Services.PLATFORM.isModLoaded("scorchful")) {
                source = "Thermoo";
                reader = thermoo();
            }
            if (reader != null) {
                Constants.LOG.info("Body temperature comes from {}", source);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Constants.LOG.warn("{} has no temperature API here, using biomes: {}", source, e.toString());
            reader = null;
        }
    }

    /** {@link #COLD}, {@link #HOT} or {@link #NEUTRAL}, or null when the biome should decide. */
    static Integer body(Player player) {
        find();
        if (reader == null) {
            return null;
        }
        try {
            int body = reader.read(player);
            return body < 0 ? null : body;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Constants.LOG.warn("Could not read {}'s temperature, using biomes: {}", source, e.toString());
            reader = null;
            return null;
        }
    }

    private static int byName(Object level) {
        return switch (((Enum<?>) level).name()) {
            case "ICY", "COLD", "FROSTBITE" -> COLD;
            case "HOT", "HEAT_STROKE" -> HOT;
            default -> NEUTRAL;
        };
    }

    private static Reader toughAsNails() throws ReflectiveOperationException {
        Class<?> helper = Class.forName("toughasnails.api.temperature.TemperatureHelper");
        Method enabled = helper.getMethod("isTemperatureEnabled");
        Method level = helper.getMethod("getTemperatureForPlayer", Player.class);
        return player -> (boolean) enabled.invoke(null) ? byName(level.invoke(null, player)) : -1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Reader coldSweat() throws ReflectiveOperationException {
        Class<?> temperature = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
        Class trait = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Trait");
        Object body = Enum.valueOf(trait, "BODY");
        Method get = temperature.getMethod("get", LivingEntity.class, trait);
        return player -> {
            double value = (double) get.invoke(null, player, body);
            return value <= -COLD_SWEAT_LIMIT ? COLD : value >= COLD_SWEAT_LIMIT ? HOT : NEUTRAL;
        };
    }

    private static Reader survive() throws ReflectiveOperationException {
        Class<?> needs = Class.forName("com.stereowalker.survive.api.needs.PlayerNeeds");
        Method api = needs.getMethod("api");
        Method temperature = needs.getMethod("getTemperature", LivingEntity.class);
        Method level = Class.forName("com.stereowalker.survive.needs.TemperatureData")
                .getMethod("getTemperatureLevel");
        Class<?> util = Class.forName("com.stereowalker.survive.needs.TemperatureUtil");
        Method cold = util.getMethod("firstCold", Player.class);
        Method heat = util.getMethod("firstHeat", Player.class);
        return player -> {
            double value = (double) level.invoke(temperature.invoke(api.invoke(null), player));
            return value <= (double) cold.invoke(null, player) ? COLD
                    : value >= (double) heat.invoke(null, player) ? HOT : NEUTRAL;
        };
    }

    private static Reader legendarySurvival() throws ReflectiveOperationException {
        String root = "sfiomn.legendarysurvivaloverhaul.";
        Method holder;
        Method current;
        try {
            holder = Class.forName(root + "util.AttachmentUtil").getMethod("getTempAttachment", Player.class);
            current = Class.forName(root + "common.attachments.temperature.TemperatureAttachment")
                    .getMethod("getTemperatureLevel");
        } catch (ClassNotFoundException e) {
            // 1.20.1 keeps it in a capability rather than an attachment.
            holder = Class.forName(root + "util.CapabilityUtil").getMethod("getTempCapability", Player.class);
            current = Class.forName(root + "common.capabilities.temperature.TemperatureCapability")
                    .getMethod("getTemperatureLevel");
        }
        Method level = Class.forName(root + "api.temperature.TemperatureUtil")
                .getMethod("getTemperatureEnum", float.class);
        Method read = holder;
        Method value = current;
        return player -> byName(level.invoke(null, value.invoke(read.invoke(null, player))));
    }

    /** The interface moved at Thermoo 10, which also dropped the scale, so it is worked out here. */
    private static Reader thermoo() throws ReflectiveOperationException {
        Class<?> aware;
        try {
            aware = Class.forName("com.github.thedeathlycow.thermoo.api.core.v2.TemperatureAware");
        } catch (ClassNotFoundException e) {
            aware = Class.forName("com.github.thedeathlycow.thermoo.api.temperature.TemperatureAware");
        }
        Method now = aware.getMethod("thermoo$getTemperature");
        Method min = aware.getMethod("thermoo$getMinTemperature");
        Method max = aware.getMethod("thermoo$getMaxTemperature");
        return player -> {
            int value = (int) now.invoke(player);
            int limit = value < 0 ? -(int) min.invoke(player) : (int) max.invoke(player);
            float scale = limit == 0 ? 0.0F : (float) value / limit;
            return scale <= -THERMOO_LIMIT ? COLD : scale >= THERMOO_LIMIT ? HOT : NEUTRAL;
        };
    }
}
