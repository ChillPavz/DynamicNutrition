package com.chillpavz.dynamicnutrition.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whether a player's arm is swinging. One jar serves 26.1 to 26.3, and 26.3 replaced the public
 * field with a method, so neither can be compiled against. Nothing here is obfuscated at 26.x, so
 * the names are the same at run time. With neither present, work simply stops counting.
 */
final class SwingCheck {

    private static final MethodHandle READ = find();

    private SwingCheck() {
    }

    private static MethodHandle find() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            return lookup.findVirtual(LivingEntity.class, "isSwinging", MethodType.methodType(boolean.class));
        } catch (ReflectiveOperationException e) {
            try {
                return lookup.findGetter(LivingEntity.class, "swinging", boolean.class);
            } catch (ReflectiveOperationException gone) {
                return null;
            }
        }
    }

    static boolean swinging(ServerPlayer player) {
        if (READ == null) {
            return false;
        }
        try {
            return (boolean) READ.invoke((LivingEntity) player);
        } catch (Throwable e) {
            return false;
        }
    }
}
