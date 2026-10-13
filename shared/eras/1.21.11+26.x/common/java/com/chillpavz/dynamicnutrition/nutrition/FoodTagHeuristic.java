package com.chillpavz.dynamicnutrition.nutrition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Stage four of the resolution pipeline: guess a nutrient set from the convention food tags.
 *
 * <p><b>This stage is the reason the mod works with food mods nobody has heard of.</b> The other
 * three stages need somebody to have done something: an explicit entry, a nutrient tag, or a recipe
 * whose ingredients resolve. Every nutrition mod surveyed leaves an unknown food with NO nutrients
 * at all. The usual answer is a hand-written compatibility file per mod, which is exactly why that
 * approach ends up supporting a list of food mods rather than all of them.
 *
 * <p>The rules themselves are band-neutral tag ids in {@link FoodTagRules}, shared by every band;
 * this class only turns them into this version's tag keys, once.
 */
public final class FoodTagHeuristic {

    private record Rule(List<TagKey<Item>> tags, List<Nutrient> nutrients) {
    }

    private static TagKey<Item> key(String id) {
        return TagKey.create(Registries.ITEM, Identifier.parse(id));
    }

    private static List<TagKey<Item>> keys(List<String> ids) {
        List<TagKey<Item>> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            out.add(key(id));
        }
        return List.copyOf(out);
    }

    private static final List<Rule> RULES = FoodTagRules.RULES.stream()
            .map(rule -> new Rule(keys(rule.tags()), rule.nutrients()))
            .toList();

    private static final List<TagKey<Item>> ANY_FOOD = keys(FoodTagRules.ANY_FOOD);

    private FoodTagHeuristic() {
    }

    /**
     * The nutrient set implied by this item's tags, or empty if none of the rules match.
     *
     * <p>Uses the registry holder rather than a stack, because these are item tags and a stack's
     * components are irrelevant to them.
     */
    public static Set<Nutrient> nutrientsFor(Item item) {
        var holder = item.builtInRegistryHolder();
        for (Rule rule : RULES) {
            for (TagKey<Item> tag : rule.tags()) {
                if (holder.is(tag)) {
                    return new LinkedHashSet<>(rule.nutrients());
                }
            }
        }
        Set<Nutrient> named = FoodTagRules.fromTagNames(holder.tags().map(tag -> tag.location().getNamespace()
                + ":" + tag.location().getPath()).toList());
        if (named.isEmpty() && ANY_FOOD.stream().anyMatch(holder::is)) {
            named = FoodWords.forTagPath(BuiltInRegistries.ITEM.getKey(item).getPath());
            if (named.isEmpty()) {
                named = new LinkedHashSet<>(List.of(Nutrients.CARBOHYDRATES));
            }
        }
        return named;
    }

    /** Number of rules, so an audit can assert the table has not been silently emptied. */
    public static int ruleCount() {
        return RULES.size();
    }
}
