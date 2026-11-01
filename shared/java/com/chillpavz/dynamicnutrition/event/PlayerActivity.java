package com.chillpavz.dynamicnutrition.event;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import com.chillpavz.dynamicnutrition.nutrition.ActivityDecay;

/**
 * What a player is doing right now, read from the game for {@link ActivityDecay}. Vehicles count for
 * nothing on purpose: rowing and riding burn no food in vanilla, so there is no decay to steer.
 */
final class PlayerActivity {

    /** A fight or a job lasts this long after the last blow or swing: five seconds. */
    private static final int RECENT_TICKS = 100;

    /** Vanilla's own snow line: below this a biome's rain falls as snow. */
    private static final float COLD_BELOW = 0.15F;

    /** Deserts, savannas, badlands and the Nether sit at 1.2 and up; jungles stay under this. */
    private static final float HOT_FROM = 1.2F;

    /** Vanilla regenerates health from the food bar at this level or above. */
    private static final int HEALS_FROM_FOOD = 18;

    private static final Map<ServerPlayer, Integer> LAST_SWING = new WeakHashMap<>();

    private PlayerActivity() {
    }

    /** Once a tick: a swing lasts a few ticks, and the decay check only looks once a second. */
    static void tick(ServerPlayer player) {
        if (player.swinging) {
            LAST_SWING.put(player, player.tickCount);
        }
    }

    static int of(ServerPlayer player) {
        int activity = 0;
        int now = player.tickCount;
        boolean fighting = recent(now, player.getLastHurtMobTimestamp())
                || recent(now, player.getLastHurtByMobTimestamp());
        if (fighting) {
            activity |= ActivityDecay.FIGHTING;
        } else if (recent(now, LAST_SWING.getOrDefault(player, 0))) {
            activity |= ActivityDecay.LABOUR;
        }
        if (player.isInWater()) {
            activity |= ActivityDecay.SWIMMING;
        } else if (player.isSprinting()) {
            activity |= ActivityDecay.RUNNING;
        }
        if (player.isHurt() && player.getFoodData().getFoodLevel() >= HEALS_FROM_FOOD) {
            activity |= ActivityDecay.HEALING;
        }
        return activity | climate(player);
    }

    /** Tough As Nails knows the body's temperature, armour included, so it decides when present. */
    private static int climate(ServerPlayer player) {
        String body = TemperatureCompat.level(player);
        if (body != null) {
            return switch (body) {
                case "ICY", "COLD" -> ActivityDecay.COLD;
                case "HOT" -> ActivityDecay.HOT;
                default -> 0;
            };
        }
        if (player.getTicksFrozen() > 0) {
            return ActivityDecay.COLD;
        }
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        // Under a roof or in a cave the biome's weather is not reaching the player. The Nether has
        // no sky at all, and is hot everywhere.
        if (level.dimensionType().hasSkyLight() && !level.canSeeSky(pos)) {
            return 0;
        }
        float temperature = level.getBiome(pos).value().getBaseTemperature();
        if (temperature < COLD_BELOW) {
            return ActivityDecay.COLD;
        }
        return temperature >= HOT_FROM ? ActivityDecay.HOT : 0;
    }

    private static boolean recent(int now, int stamp) {
        return stamp > 0 && now - stamp < RECENT_TICKS;
    }
}
