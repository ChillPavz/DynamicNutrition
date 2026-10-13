package com.chillpavz.dynamicnutrition.nutrition;

/**
 * Which item stands for a recipe ingredient that accepts several, ranked from names alone so every
 * band picks the same one.
 *
 * <p>A tag's first member is whatever mod sorted first. In one food pack {@code #c:eggs} began with
 * a vegan mod's silken tofu, and every dough made with "an egg" stopped being a protein source; in
 * another an onion tag began with {@code croptopia:onion_seed}, and onion dishes took the seed. So:
 * vanilla's item first (the egg), then an edible member (the onion itself, the dish's own mod
 * first), then anything that is not a seed, and a seed only when nothing else is offered.
 */
public final class IngredientPick {

    /** The best rank there is: a caller may stop looking once a member scores it. */
    public static final int BEST = 0;

    private IngredientPick() {
    }

    /**
     * Lower is better; equal ranks keep the ingredient's own order.
     *
     * @param namespace the member's namespace
     * @param path      the member's path
     * @param edible    whether a player can eat the member
     * @param own       the namespace of the item the recipe makes
     */
    public static int rank(String namespace, String path, boolean edible, String own) {
        boolean seed = isSeed(path);
        boolean vanilla = namespace.equals("minecraft");
        if (vanilla && !seed) {
            return BEST;
        }
        if (edible) {
            return namespace.equals(own) ? 1 : 2;
        }
        if (!seed) {
            return namespace.equals(own) ? 3 : 4;
        }
        return vanilla ? 5 : 6;
    }

    /** A seed by name: {@code onion_seed}, {@code wheat_seeds}, Pam's {@code onionseeditem}. */
    static boolean isSeed(String path) {
        for (String word : path.split("_")) {
            if (word.equals("seed") || word.equals("seeds")
                    || word.endsWith("seeditem") || word.endsWith("seedsitem")) {
                return true;
            }
        }
        return false;
    }
}
