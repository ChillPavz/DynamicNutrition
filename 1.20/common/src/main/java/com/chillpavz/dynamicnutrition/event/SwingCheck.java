package com.chillpavz.dynamicnutrition.event;

import net.minecraft.server.level.ServerPlayer;

/** Whether a player's arm is swinging. A per-band class because 26.3 changed how this is read. */
final class SwingCheck {

    private SwingCheck() {
    }

    static boolean swinging(ServerPlayer player) {
        return player.swinging;
    }
}
