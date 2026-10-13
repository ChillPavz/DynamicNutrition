package com.chillpavz.dynamicnutrition.nutrition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The tag rules of stage four, as plain tag ids, so every band reads the same table. Each band's
 * {@code FoodTagHeuristic} only turns the ids into its own {@code TagKey}s.
 *
 * <p>A rule lists every tag that means the same thing, newest convention first:
 * <ul>
 *   <li>{@link #c} the {@code c:} taxonomy of 1.21 and later ({@code c:foods/fruit}), which Fabric
 *       API and NeoForge both ship. The build checks these against the loaders' own tags.</li>
 *   <li>{@link #legacy} the older names mods still use: the plural {@code c:} umbrellas of Fabric's
 *       first convention ({@code c:fruits}, {@code c:vegetables}, {@code c:nuts}), Croptopia's
 *       {@code c:grain}, and Forge's {@code forge:} tags on 1.20.1. No loader defines them, so a mod
 *       that tags its items with one creates it. Croptopia tags its raw crops ONLY this way, which
 *       is why every one of them used to resolve to nothing.</li>
 * </ul>
 *
 * <p>Order matters: the first matching rule wins, so the specific tags come before the broad ones.
 * A food carrying several tags gets the most specific reading rather than a union, because a union
 * would drift towards "everything feeds everything".
 */
public final class FoodTagRules {

    /** One reading: any of these tags gives these nutrients. */
    public record Rule(List<String> tags, List<Nutrient> nutrients) {
    }

    /** A tag of the current {@code c:} convention, by path. */
    private static String c(String path) {
        return "c:" + path;
    }

    /** An older name for the same thing, in full ({@code c:fruits}, {@code forge:crops/rice}). */
    private static String legacy(String id) {
        return id;
    }

    private static Rule rule(List<Nutrient> nutrients, String... tags) {
        return new Rule(List.of(tags), nutrients);
    }

    private static final List<Nutrient> PROTEIN_FAT = List.of(Nutrients.PROTEIN, Nutrients.FAT);
    private static final List<Nutrient> PROTEIN_MINERALS = List.of(Nutrients.PROTEIN, Nutrients.MINERALS);
    private static final List<Nutrient> CARBS = List.of(Nutrients.CARBOHYDRATES);
    private static final List<Nutrient> CARBS_VITAMINS = List.of(Nutrients.CARBOHYDRATES, Nutrients.VITAMINS);
    private static final List<Nutrient> CARBS_FAT = List.of(Nutrients.CARBOHYDRATES, Nutrients.FAT);
    private static final List<Nutrient> VITAMINS_MINERALS = List.of(Nutrients.VITAMINS, Nutrients.MINERALS);
    private static final List<Nutrient> FAT_MINERALS = List.of(Nutrients.FAT, Nutrients.MINERALS);

    public static final List<Rule> RULES = List.of(
            // Specific first.
            rule(PROTEIN_FAT, c("foods/cooked_meat"), legacy("forge:cooked_meats")),
            rule(PROTEIN_FAT, c("foods/raw_meat"), legacy("forge:raw_meats"), legacy("c:raw_meats")),
            rule(PROTEIN_MINERALS, c("foods/cooked_fish"), legacy("forge:cooked_fishes"),
                    legacy("c:cooked_fishes")),
            rule(PROTEIN_MINERALS, c("foods/raw_fish"), legacy("forge:raw_fishes"), legacy("c:raw_fishes")),
            // A crop's own identity before the generic labels: corn is tagged c:foods/vegetable as
            // well as c:crops/grain, and rice c:seeds as well as c:crops/rice. Read as a vegetable or
            // a seed (fat and minerals), corn and rice dishes lost their carbohydrate.
            rule(CARBS, c("crops/grain"), legacy("c:grain"), legacy("c:grains"), legacy("forge:grain"),
                    legacy("forge:grains")),
            rule(CARBS, c("crops/rice"), legacy("forge:crops/rice"), legacy("forge:grain/rice")),
            rule(CARBS, c("crops/corn"), legacy("forge:crops/corn"), legacy("forge:grain/corn")),
            rule(CARBS, c("seeds/corn"), legacy("forge:seeds/corn")),
            // Soybeans are tagged c:seeds too, and soy is a protein food.
            rule(PROTEIN_FAT, c("crops/soybeans"), legacy("forge:crops/soybean")),
            // Nuts before fruit: Croptopia files almonds under c:fruits as well as c:nuts.
            // No loader has a c:foods subtag for nuts (checked against NeoForge 26.2 and the 1.21 jars).
            rule(FAT_MINERALS, legacy("c:nuts"), legacy("forge:nuts")),
            rule(CARBS_VITAMINS, c("foods/berry"), legacy("c:berries"), legacy("forge:berries")),
            rule(CARBS_VITAMINS, c("foods/fruit"), legacy("c:fruits"), legacy("forge:fruits")),
            rule(VITAMINS_MINERALS, c("foods/vegetable"), legacy("c:vegetables"), legacy("forge:vegetables")),
            rule(CARBS, c("foods/candy")),
            rule(CARBS_FAT, c("foods/cookie")),
            rule(CARBS_FAT, c("foods/pie")),
            rule(CARBS, c("foods/bread"), legacy("forge:bread")),
            rule(CARBS, c("foods/dough"), legacy("forge:dough"), legacy("c:doughs")),
            // A golden food is supernatural rather than nutritious; give it the micros, which is
            // what the vanilla table does for the golden apple and carrot.
            rule(VITAMINS_MINERALS, c("foods/golden")),
            // Soup is usually resolved by the recipe walk before it reaches here. When it is not,
            // it is at least a mixed dish rather than a single macronutrient.
            rule(List.of(Nutrients.CARBOHYDRATES, Nutrients.VITAMINS, Nutrients.MINERALS), c("foods/soup")),
            // Non-food ingredients that the recipe walk needs to be able to resolve.
            rule(PROTEIN_FAT, c("eggs"), legacy("forge:eggs")),
            rule(PROTEIN_FAT, c("drinks/milk"), legacy("forge:milk"), legacy("c:milk"), legacy("c:milks")),
            // Juice is fruit with the fibre taken out: sugar and vitamin C. Juices are often made
            // from a fluid (grapes stomped into a basin, then bottled), which leaves the recipe walk
            // nothing to follow, so the tag is the only thing that can place them.
            rule(CARBS_VITAMINS, c("drinks/juice"), legacy("c:juices"), legacy("forge:juices")),
            rule(CARBS, c("drinks/honey")),
            rule(CARBS, c("crops/wheat"), legacy("forge:crops/wheat")),
            rule(List.of(Nutrients.CARBOHYDRATES, Nutrients.MINERALS), c("crops/potato"),
                    legacy("forge:crops/potato")),
            rule(VITAMINS_MINERALS, c("crops/beetroot"), legacy("forge:crops/beetroot")),
            rule(List.of(Nutrients.VITAMINS), c("crops/carrot"), legacy("forge:crops/carrot")),
            rule(CARBS_VITAMINS, c("crops/melon")),
            rule(CARBS_VITAMINS, c("crops/pumpkin")),
            rule(CARBS, c("crops/sugar_cane")),
            rule(CARBS_FAT, c("crops/cocoa_bean")),
            rule(FAT_MINERALS, c("seeds"), legacy("forge:seeds")));

    /**
     * Broadest possible net, LAST, after the subtag names and the item's own name: anything a mod
     * bothered to call food is at least a carbohydrate source, which beats resolving to nothing. As a
     * rule in the list it came first for every food tagged only {@code c:foods}, and ham, roast
     * chicken, crab, shrimp, cheese and salads were all pure carbohydrate.
     */
    public static final List<String> ANY_FOOD = List.of(c("foods"));

    /** Namespaces whose tag NAMES are read when no rule matched. */
    private static final Set<String> NAMED_ROOTS = Set.of("c", "forge");

    /**
     * Folder tags whose last segment names a food ({@code c:fruits/banana}, {@code forge:crops/rice}).
     * Only these: {@code c:dyes/orange} is not a fruit.
     */
    private static final Set<String> FOOD_FOLDERS = Set.of("foods", "drinks", "crops", "fruits", "fruit",
            "vegetables", "vegetable", "nuts", "grains", "grain", "berries", "juices", "jams", "meats",
            "fishes", "cooked_meats", "raw_meats", "cooked_fishes", "raw_fishes");

    private FoodTagRules() {
    }

    /**
     * No rule matched, so read the names of the item's own convention tags. Mods add tags no rule
     * can list: subtags ({@code c:foods/raw_dragon_meat}, {@code c:fruits/banana}) and flat
     * per-item tags ({@code c:bananas}, {@code c:beef_jerkies}, Croptopia's whole catalogue), and
     * often nothing else. The name still says what the food is.
     *
     * <p>In order, each group sorted so an item with two such tags gets the same answer on every
     * start (a holder's tags come in no particular order):
     * <ol>
     *   <li>{@code c:foods/*} and {@code c:drinks/*}, read from the last word back, as always.</li>
     *   <li>Other folder tags under a {@link #FOOD_FOLDERS} root, by their last segment.</li>
     *   <li>Flat tags ({@code c:bananas}), by their LAST word only: {@code c:orange_dyes} is a dye,
     *       and reading further back would make it a fruit.</li>
     * </ol>
     */
    public static Set<Nutrient> fromTagNames(List<String> tagIds) {
        List<String> food = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        List<String> flat = new ArrayList<>();
        for (String id : tagIds) {
            int colon = id.indexOf(':');
            if (colon < 0 || !NAMED_ROOTS.contains(id.substring(0, colon))) {
                continue;
            }
            String path = id.substring(colon + 1);
            int slash = path.indexOf('/');
            if (id.startsWith("c:foods/") || id.startsWith("c:drinks/")) {
                food.add(path.substring(slash + 1));
            } else if (slash > 0) {
                if (FOOD_FOLDERS.contains(path.substring(0, slash))) {
                    folders.add(path.substring(path.lastIndexOf('/') + 1));
                }
            } else {
                flat.add(path);
            }
        }
        food.sort(null);
        folders.sort(null);
        flat.sort(null);
        for (String path : food) {
            Set<Nutrient> found = FoodWords.forTagPath(path);
            if (!found.isEmpty()) {
                return found;
            }
        }
        for (String path : folders) {
            Set<Nutrient> found = FoodWords.forTagPath(path);
            if (!found.isEmpty()) {
                return found;
            }
        }
        for (String path : flat) {
            Set<Nutrient> found = FoodWords.forLastWord(path);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return Set.of();
    }

    /** Number of rules, so an audit can assert the table has not been silently emptied. */
    public static int ruleCount() {
        return RULES.size();
    }
}
