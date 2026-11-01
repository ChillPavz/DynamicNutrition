package com.chillpavz.dynamicnutrition.event;

import net.minecraft.server.level.ServerPlayer;

import com.chillpavz.dynamicnutrition.nutrition.ActivityDecay;

/** What a player is doing right now, read from the game for {@link ActivityDecay}. */
final class PlayerActivity {

    /** A fight lasts this long after the last blow in either direction: five seconds. */
    private static final int FIGHT_TICKS = 100;

    /** Vanilla's own snow line: below this a biome's rain falls as snow. */
    private static final float COLD_BELOW = 0.15F;

    private PlayerActivity() {
    }

    static int of(ServerPlayer player) {
        int activity = 0;
        if (player.isSprinting()) {
            activity |= ActivityDecay.RUNNING;
        }
        int now = player.tickCount;
        if (recent(now, player.getLastHurtMobTimestamp()) || recent(now, player.getLastHurtByMobTimestamp())) {
            activity |= ActivityDecay.FIGHTING;
        }
        if (player.level().getBiome(player.blockPosition()).value().getBaseTemperature() < COLD_BELOW) {
            activity |= ActivityDecay.COLD;
        }
        return activity;
    }

    private static boolean recent(int now, int stamp) {
        return stamp > 0 && now - stamp < FIGHT_TICKS;
    }
}
