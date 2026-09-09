package com.chillpavz.dynamicnutrition.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A {@link MobEffect} this mod can construct, whose name can say WHICH nutrient is the problem.
 *
 * <p>Two reasons this class exists rather than a bare {@code MobEffect}:
 * <ol>
 *   <li>{@code MobEffect}'s constructors are protected.</li>
 *   <li><b>"Where is this debuff coming from" is the most human complaint in this whole field.</b>
 *       The same report turns up again and again: somebody spends days wondering whether they are
 *       cursed before working out that they gave all their meat to their dogs. Naming the effect
 *       "Malnourished" is most of the answer; naming the nutrient in it is the rest.</li>
 * </ol>
 *
 * <p>The detail is supplied by the CLIENT, because it depends on the viewing player's own values and
 * the effect object is a single shared registry entry. On a server no supplier is installed and the
 * name is the plain translated one, which is also what happens if the supplier throws.
 */
public class NutritionMobEffect extends MobEffect {

    /** Installed by the client wiring only. Never on a server. */
    public interface DetailSupplier {
        Component detailFor(MobEffect effect);
    }

    private static volatile DetailSupplier details;

    public NutritionMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static void setDetailSupplier(DetailSupplier supplier) {
        details = supplier;
    }

    @Override
    public Component getDisplayName() {
        Component base = super.getDisplayName();
        DetailSupplier supplier = details;
        if (supplier == null) {
            return base;
        }
        try {
            Component detail = supplier.detailFor(this);
            return detail == null ? base : Component.empty().append(base).append(detail);
        } catch (Throwable t) {
            // A name is drawn every frame the inventory is open. Whatever went wrong, the answer is
            // the plain name, never an exception out of a render path.
            return base;
        }
    }
}
