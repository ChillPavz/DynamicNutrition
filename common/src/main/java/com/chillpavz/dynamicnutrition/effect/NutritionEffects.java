package com.chillpavz.dynamicnutrition.effect;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.config.NutritionConfig;
import com.chillpavz.dynamicnutrition.player.NutrientStatus;

/**
 * The two status effects, and the rule for when a player has one.
 *
 * <h2>Why there are two and not three</h2>
 * v1 has three states and the middle one, {@link NutrientStatus#SAFE}, is deliberately nothing:
 * adequate but not rewarded. An effect for "you are fine" would sit permanently in every player's
 * effect list saying nothing.
 *
 * <h2>Three rules, each one a bug somebody else shipped</h2>
 * <ul>
 *   <li><b>Every modifier's SIGN is declared in one place</b>, in the constants below, and asserted
 *       by the audit script. A malnutrition modifier written without its minus sign hands a
 *       starving player <b>extra attack damage and extra maximum health</b>, and nothing about that
 *       is visible in a log, so it can and does survive whole releases.</li>
 *   <li><b>Modifiers are applied on the TRANSITION, never on a tick.</b> A {@code max_health}
 *       modifier reapplied every tick resets the player's health and damages them, which is a
 *       shipped bug in this space. Here vanilla does the applying: a modifier attached to a
 *       {@code MobEffect} is
 *       added when the effect is added and removed when it is removed, so the only way to get this
 *       wrong would be to add the effect repeatedly, which {@link #apply} explicitly does not.</li>
 *   <li><b>Every modifier has its own id.</b> Two nutrients sharing a {@code max_health} modifier
 *       id means only one of them ever applies, silently.</li>
 * </ul>
 *
 * <p>The numbers are mild on purpose. A nutrition mod that is too harsh is uninstalled and one that
 * is too lenient is pointless, and only one of those two is recoverable.
 */
public final class NutritionEffects {

    // ---------------------------------------------------------------- the numbers
    // THE SIGN LIVES HERE AND NOWHERE ELSE. Everything below reads these constants verbatim; no
    // call site negates, doubles or conditions them. The audit asserts that every MALNOURISHED_
    // constant is negative and every NOURISHED_ one is positive.

    public static final double MALNOURISHED_SPEED = -0.10;
    public static final double MALNOURISHED_DAMAGE = -0.15;

    public static final double NOURISHED_SPEED = 0.05;
    public static final double NOURISHED_DAMAGE = 0.05;

    /** Vanilla's own unit: one heart is two points of health. */
    public static final double HEALTH_PER_HEART = 2.0;

    /** Nobody may configure themselves down to no maximum health. */
    public static final int MAX_HEARTS = 5;

    // ---------------------------------------------------------------- the effects

    public static final ResourceKey<MobEffect> MALNOURISHED_KEY = key("malnourished");
    public static final ResourceKey<MobEffect> WELL_NOURISHED_KEY = key("well_nourished");

    /**
     * The effect objects themselves, built here so both loaders register the same thing.
     *
     * <p>{@code MobEffect}'s constructor is protected, hence {@link NutritionMobEffect}.
     */
    public static final MobEffect MALNOURISHED =
            new NutritionMobEffect(MobEffectCategory.HARMFUL, 0xB23A3A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("malnourished_speed"),
                            MALNOURISHED_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("malnourished_damage"),
                            MALNOURISHED_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public static final MobEffect WELL_NOURISHED =
            new NutritionMobEffect(MobEffectCategory.BENEFICIAL, 0x63B23A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("nourished_speed"),
                            NOURISHED_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("nourished_damage"),
                            NOURISHED_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    /**
     * The maximum-health modifiers, applied BY HAND rather than attached to the effects.
     *
     * <p>A modifier attached to a {@code MobEffect} is fixed when the class loads, so it cannot
     * follow a config value, and the number of hearts is configurable. The two ids stay distinct
     * per status for the same reason the baked ones do: two effects sharing a {@code max_health}
     * id means only one of them ever applies.
     */
    private static final Identifier MALNOURISHED_HEALTH_ID = id("malnourished_health");
    private static final Identifier NOURISHED_HEALTH_ID = id("nourished_health");

    private static volatile Holder<MobEffect> malnourished;
    private static volatile Holder<MobEffect> wellNourished;

    private NutritionEffects() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    private static ResourceKey<MobEffect> key(String path) {
        return ResourceKey.create(Registries.MOB_EFFECT, id(path));
    }

    /**
     * Each loader registers the two effects its own way and hands the resulting holders back.
     *
     * <p>A {@code MobEffectInstance} needs a {@code Holder}, which only exists once the thing is
     * registered, so the common code cannot build one for itself.
     */
    public static void bind(Holder<MobEffect> malnourishedHolder,
                            Holder<MobEffect> wellNourishedHolder) {
        malnourished = malnourishedHolder;
        wellNourished = wellNourishedHolder;
    }

    /** The effect for a status, or null where the status deliberately has none. */
    public static Holder<MobEffect> forStatus(NutrientStatus status) {
        return switch (status) {
            case MALNOURISHED -> malnourished;
            case ON_TARGET -> wellNourished;
            case SAFE -> null;
        };
    }

    /**
     * Bring the player's effects into line with their status.
     *
     * <p><b>Idempotent, and that is the whole point.</b> It removes only what it put there, and it
     * adds only what is missing, so calling it every second re-applies no attribute modifier. It
     * also self-heals after a death, where vanilla clears every effect but the player's nutrient
     * levels survive, so the status is unchanged and the effect has to come back.
     */
    /** Take both effects off the player, for when the feature is switched off in the config. */
    public static void clear(ServerPlayer player) {
        setHealthModifier(player, NutrientStatus.SAFE, 0.0);
        if (malnourished == null || wellNourished == null) {
            return;
        }
        for (Holder<MobEffect> ours : List.of(malnourished, wellNourished)) {
            if (player.hasEffect(ours)) {
                player.removeEffect(ours);
            }
        }
    }

    /**
     * Hearts from the config, in health points, signed.
     *
     * <p>THE SIGN LIVES HERE AND NOWHERE ELSE, exactly as it does for the two baked modifiers.
     * A malnutrition penalty that comes out positive is a buff for starving, and it is invisible
     * in a log, so it survives releases.
     * Clamped as well, so a hand-edited config file cannot take a player's health to zero.
     */
    private static double healthFor(NutrientStatus status) {
        int hearts = switch (status) {
            case MALNOURISHED -> NutritionConfig.malnourishedHeartsLost;
            case ON_TARGET -> NutritionConfig.wellNourishedHeartsGained;
            case SAFE -> 0;
        };
        double points = Math.max(0, Math.min(MAX_HEARTS, hearts)) * HEALTH_PER_HEART;
        return status == NutrientStatus.MALNOURISHED ? -points : points;
    }

    /**
     * Put exactly one of our health modifiers on the player, or none at all.
     *
     * <p><b>Only when it actually changes.</b> A {@code max_health} modifier reapplied every tick
     * resets the player's health and hurts them. This runs on the same
     * one second cadence as the status check, so the comparison below is the whole reason it stays
     * a transition rather than becoming a tick.
     *
     * <p>TRANSIENT, so nothing of ours is ever written into the player's saved attributes. A
     * permanent modifier would outlive the mod being removed.
     */
    private static void setHealthModifier(ServerPlayer player, NutrientStatus status,
                                          double amount) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        Identifier wanted = null;
        if (status == NutrientStatus.MALNOURISHED) {
            wanted = MALNOURISHED_HEALTH_ID;
        } else if (status == NutrientStatus.ON_TARGET) {
            wanted = NOURISHED_HEALTH_ID;
        }

        for (Identifier other : List.of(MALNOURISHED_HEALTH_ID, NOURISHED_HEALTH_ID)) {
            if (!other.equals(wanted) && attribute.getModifier(other) != null) {
                attribute.removeModifier(other);
            }
        }
        if (wanted == null || amount == 0.0) {
            if (wanted != null && attribute.getModifier(wanted) != null) {
                attribute.removeModifier(wanted);
            }
            return;
        }
        AttributeModifier existing = attribute.getModifier(wanted);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        attribute.addOrUpdateTransientModifier(new AttributeModifier(wanted, amount,
                AttributeModifier.Operation.ADD_VALUE));
    }

    public static void apply(ServerPlayer player, NutrientStatus status) {
        if (malnourished == null || wellNourished == null) {
            // Registration has not happened, which on a correctly built jar cannot occur. Degrade
            // to doing nothing rather than throwing on a player tick.
            return;
        }
        Holder<MobEffect> want = forStatus(status);
        for (Holder<MobEffect> ours : List.of(malnourished, wellNourished)) {
            if (ours != want && player.hasEffect(ours)) {
                player.removeEffect(ours);
            }
        }
        if (want != null && !player.hasEffect(want)) {
            // Infinite, amplifier 0, ambient, NO particles, icon shown. A permanent status that
            // trailed particles would look like a potion the player could not get rid of.
            player.addEffect(new MobEffectInstance(want, MobEffectInstance.INFINITE_DURATION, 0,
                    true, false, true));
        }
        setHealthModifier(player, status, healthFor(status));
    }
}
