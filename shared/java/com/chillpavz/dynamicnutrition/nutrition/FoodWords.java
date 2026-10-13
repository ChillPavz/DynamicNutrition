package com.chillpavz.dynamicnutrition.nutrition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reading a food from its NAMES, for the foods nothing else can place. Pure string work, so it is the
 * same in every band.
 *
 * <p>Two readings, both last resorts:
 * <ul>
 *   <li><b>A tag's own name.</b> Mods add their own subtags under the convention roots:
 *       {@code c:foods/raw_dragon_meat}, {@code c:foods/shulker_meat}. No rule can list them, but
 *       the last word says plainly what the food is. {@link #forTagPath} reads it.</li>
 *   <li><b>An item's own name.</b> A food that is another food in a different container carries
 *       that food's name with a word added: Miner's Delight makes {@code mushroom_stew_cup} out of
 *       {@code mushroom_stew} through its own data map, with no recipe and no tag to follow.
 *       {@link #stems} lists the names it could be a variant of.</li>
 * </ul>
 *
 * <p>Words are matched whole, never as substrings, so {@code nut} cannot match {@code nutrition}
 * and {@code ham} cannot match {@code hamburger}. The first word that means something wins, read
 * from the END of the name, because English puts the noun last: {@code cheese_bread} is bread.
 */
public final class FoodWords {

    private record Word(Set<String> words, List<Nutrient> nutrients) {
    }

    private static Word word(List<Nutrient> nutrients, String... words) {
        return new Word(Set.of(words), nutrients);
    }

    private static final List<Word> WORDS = List.of(
            word(List.of(Nutrients.PROTEIN, Nutrients.MINERALS),
                    "fish", "cod", "salmon", "tuna", "squid", "tentacle", "tentacles", "shrimp",
                    "crab", "seafood", "fillet", "fillets", "calamari", "clam", "clams", "oyster",
                    "oysters", "mussel", "mussels", "scallop", "scallops", "lobster", "prawn", "prawns",
                    "octopus", "fugu", "anchovy", "sardine", "eel", "roe", "caviar", "sushi", "maki",
                    "nigiri", "sashimi", "otoro", "onigiri", "snail", "escargot", "perch", "pike",
                    "carp", "trout", "bass", "herring", "mackerel", "halibut", "sturgeon", "tilapia",
                    "minnow", "piranha", "lutefisk", "clawster", "uni", "filet", "algae",
                    // Species the fishing mods add, and the parts their invented fish are named by.
                    "bream", "snapper", "koi", "chub", "arowana", "arapaima", "barbel", "knifejaw",
                    "bitterling", "bluegill", "coelacanth", "dab", "dace", "goby", "snakehead", "guppy",
                    "loach", "marlin", "flounder", "smelt", "seahorse", "darter", "char", "pollock",
                    "haddock", "bonito", "barracuda", "grouper", "walleye", "shark", "sharks", "gill",
                    "fin", "fins", "ceviche",
                    // Offal: protein, with the iron and other minerals organ meat is known for.
                    "liver", "kidney", "heart", "lungs", "lung", "stomach", "intestines", "tongue",
                    "gizzard", "blood", "brain", "nose", "snout"),
            word(List.of(Nutrients.PROTEIN, Nutrients.FAT),
                    "meat", "meats", "beef", "pork", "porkchop", "mutton", "chicken", "steak",
                    "bacon", "ham", "sausage", "sausages", "jerky", "egg", "eggs", "cheese", "milk",
                    "shank", "chops", "chop", "ribs", "drumstick", "patty", "lamb", "rabbit", "leg",
                    "legs", "gristle", "tripe", "custard", "tofu", "soy", "soymilk", "soybean",
                    "soybeans", "omelet", "omelette", "meatball", "burger", "hamburger",
                    "cheeseburger", "kebab", "skewer", "nugget", "poultry", "roast", "meatloaf",
                    "charcuterie", "kroket", "croquette", "frikandel", "bitterballen", "venison",
                    "turkey", "duck", "goose", "boar", "tail", "yogurt", "yoghurt", "natto", "nattou",
                    "flesh", "pepperoni", "salami", "pastrami", "brisket", "carnitas", "gyro",
                    "dumpling", "wonton", "mince", "rib", "bison", "moose", "camel", "frog", "wing",
                    "wings", "stroganoff", "fondue", "maggot", "karaage", "tonkatsu", "wurst", "haggis",
                    "tartare", "kabob", "kabobs", "hotdog"),
            word(List.of(Nutrients.CARBOHYDRATES, Nutrients.VITAMINS),
                    "juice", "fruit", "fruits", "berry", "berries", "grape", "grapes", "apple",
                    "apples", "cherry", "cherries", "pineapple", "melon", "hamimelon", "durian",
                    "banana", "orange", "lemon", "lime", "peach", "pear", "plum", "mango", "kiwi",
                    "date", "fig", "apricot", "strawberry", "raspberry", "blueberry", "blackberry",
                    "cranberry", "elderberry", "currant", "grapefruit", "kumquat", "nectarine",
                    "persimmon", "starfruit", "dragonfruit", "cantaloupe", "honeydew", "watermelon",
                    "raisin", "saguaro", "pomegranate", "papaya", "guava", "lychee", "passionfruit",
                    "tangerine", "mandarin", "citrus", "jam", "smoothie", "lemonade", "limeade",
                    "wolfberries", "goji", "ketchup", "compote", "marmalade", "sorbet", "nectar", "pomelo",
                    "chutney", "puree"),
            word(List.of(Nutrients.VITAMINS, Nutrients.MINERALS),
                    "vegetable", "vegetables", "salad", "greens", "mushroom", "mushrooms", "cabbage",
                    "carrot", "carrots", "beetroot", "tomato", "tomatoes", "onion", "onions", "lettuce",
                    "spinach", "kale", "cauliflower", "broccoli", "eggplant", "cucumber", "zucchini",
                    "pepper", "peppers", "garlic", "ginger", "asparagus", "artichoke", "celery",
                    "leek", "radish", "rutabaga", "turnip", "squash", "greenbean", "bellpepper",
                    "chilepepper", "tomatillo", "rhubarb", "okra", "pea", "peas", "basil", "herb",
                    "herbs", "seaweed", "kelp", "chive", "chives", "cilantro", "dill", "lemongrass",
                    "mint", "oregano", "parsley", "rosemary", "sage", "thyme", "leaf", "leaves",
                    "pickle", "pickles", "kimchi", "sauerkraut", "bok", "choy", "fungus", "truffle",
                    "sprout", "sprouts", "shoot", "shoots", "root", "roots", "lichen", "capsicum",
                    "aloe", "yucca", "cactus", "salsa", "umeboshi", "tsukemono", "wheatgrass"),
            word(List.of(Nutrients.CARBOHYDRATES, Nutrients.MINERALS),
                    "potato", "potatoes", "sweetpotato", "yam", "cassava", "taro", "fries", "chips",
                    "hashbrown", "hashbrowns"),
            word(List.of(Nutrients.FAT, Nutrients.MINERALS),
                    "nut", "nuts", "seed", "seeds", "peanut", "peanuts", "acorn", "acorns", "almond",
                    "walnut", "pecan", "cashew", "hazelnut", "pistachio", "chestnut", "macadamia",
                    "coconut", "trailmix"),
            // Fats, and the foods that are almost only fat.
            word(List.of(Nutrients.FAT),
                    "butter", "lard", "oil", "mayonnaise", "mayo", "ghee", "tallow", "suet", "cream",
                    "avocado"),
            // Legumes are a protein food with their starch; so are the bread-and-filling dishes.
            word(List.of(Nutrients.PROTEIN, Nutrients.CARBOHYDRATES),
                    "bean", "beans", "blackbean", "lentil", "lentils", "chickpea", "chickpeas",
                    "hummus", "falafel", "lasagna", "lasagne", "pizza", "taco", "tacos", "burrito",
                    "sandwich", "sandwiches", "wrap", "quiche", "pasty", "calzone", "ravioli",
                    "bolognese", "feijoada", "khinkali", "pierogi", "empanada", "bruschetta", "miso",
                    "lasanga", "panini", "bunwich", "perogies", "pierogies"),
            word(List.of(Nutrients.CARBOHYDRATES, Nutrients.FAT),
                    "cookie", "cookies", "cake", "cakes", "pie", "pies", "chocolate", "brownie",
                    "brownies", "muffin", "muffins", "donut", "donuts", "doughnut", "waffle", "waffles",
                    "tart", "tarts", "pancake", "pancakes", "croissant", "danish", "baklava", "fudge",
                    "toffee", "pastry", "pastries", "churro", "churros", "eclair", "macaron", "dessert",
                    "pudding", "trifle", "mousse", "tiramisu", "cheesecake", "cupcake", "biscuit",
                    "biscuits", "scone", "scones", "cobbler", "crumble", "strudel", "turnover",
                    "fritter", "fritters", "oliebollen", "stroopwafel", "speculaas", "gingerbread",
                    "shortbread", "praline", "nougat", "brittle", "truffles", "icecream", "sundae",
                    "milkshake", "latte", "cappuccino", "mocha", "cocoa", "gateau", "galette",
                    "sable", "malasada", "oreo", "cone", "canjica", "crisp", "tempura", "gelato",
                    "blondie", "blondies", "pavlova", "meringue", "puff", "puffs", "tarte"),
            word(List.of(Nutrients.CARBOHYDRATES, Nutrients.VITAMINS, Nutrients.MINERALS),
                    "soup", "stew", "bisque", "borscht", "goulash", "chowder", "broth", "stock",
                    "gruel", "congee", "ramen", "pho", "curry", "chili", "gumbo", "minestrone",
                    "okroshka", "ukha", "acquacotta", "stracciatella", "hodgepodge", "fricassee",
                    "casserole", "stirfry", "paella", "jambalaya", "stamppot", "ratatouille", "gazpacho",
                    "bouillabaisse", "moqueca", "malatang", "hotpot", "hotdish"),
            // Alcohol is sugar to the body, which is all a wine or a beer is here.
            word(List.of(Nutrients.CARBOHYDRATES),
                    "bread", "dough", "pasta", "noodle", "noodles", "rice", "grain", "grains", "corn",
                    "toast", "popcorn", "cereal", "porridge", "oatmeal", "risotto", "nachos", "jello",
                    "popsicle", "barley", "oat", "oats", "wheat", "rye", "flour", "millet", "quinoa",
                    "tortilla", "crepe", "candy", "sugar", "pretzel", "cracker", "crackers", "bagel",
                    "bun", "buns", "loaf", "pita", "macaroni", "spaghetti", "lollipop", "gumball",
                    "licorice", "marshmallow", "gelatin", "jelly", "syrup", "molasses", "sprinkles",
                    "frosting", "icing", "caramel", "soda", "cola", "honey", "muesli", "granola",
                    "mochi", "dango", "wine", "mead", "beer", "ale", "lager", "stout", "cider",
                    "champagne", "sake", "vodka", "whiskey", "whisky", "rum", "gin", "brandy", "liquor",
                    "cocktail", "mojito", "kvass", "sangria", "punch", "pilsner", "porter", "hop",
                    "hops", "maize", "polenta", "baguette", "cornet", "gummy", "gumdrop", "sweets",
                    "treat", "floss", "boba", "roll", "rolls", "soba", "udon", "crouton", "croutons",
                    "crumbs", "stuffing", "kykeon", "brew", "taffy"),
            // Tea and coffee are water and a little of the leaf or the bean: minerals, nothing else.
            word(List.of(Nutrients.MINERALS),
                    "tea", "coffee", "espresso", "matcha", "kombucha", "americano", "mate", "hibiscus",
                    "rosehip", "chamomile",
                    // Spices: eaten for flavour, they bring trace minerals and nothing else.
                    "spice", "spices", "cinnamon", "nutmeg", "paprika", "mustard", "turmeric",
                    "saffron", "clove", "cloves", "cumin", "salt", "vanilla", "peppercorn"));

    /**
     * Words that say what PART of a food this is, not what food: {@code durian_flesh} is durian.
     * Read from the end, such a word is set aside and the earlier words are tried first; only when
     * none of them means anything does the part word itself count ({@code cooked_flesh} is meat).
     */
    private static final Set<String> PART_WORDS = Set.of("flesh", "tail", "root", "roots", "leaf",
            "leaves", "heart", "shoot", "shoots", "stock", "oil", "cream", "punch", "chips");

    /**
     * Endings a compound word ({@code applefritteritem}, {@code cottagepie}) may be read by: words of
     * four letters or more, plus these short ones, so {@code graham} is not ham, {@code goat} is
     * not an oat, and {@code cloudfin} is a fish.
     */
    private static final Set<String> SHORT_SUFFIXES = Set.of("pie", "jam", "tea", "bun", "egg", "fin");

    /**
     * Words that name a DISH rather than what is in it. A dish word found first is joined by the
     * nearest earlier ingredient word: {@code fish_soup} is soup AND fish, so it has protein.
     * Only the nearest one, so a long name does not collect every nutrient there is.
     */
    private static final Set<String> DISH_WORDS = Set.of("soup", "stew", "chowder", "broth", "bisque",
            "pie", "pies", "tart", "sandwich", "skewer", "burger", "bun", "salad", "roll", "rolls",
            "wrap", "taco", "pizza", "tea", "curry", "omelet", "omelette", "pasta", "noodles",
            "risotto", "porridge", "smoothie", "milkshake", "cake", "muffin", "bread", "toast",
            "dumpling", "casserole", "fritter", "fritters", "jam", "jelly", "pudding", "loaf");

    /**
     * A first word that says the thing was an animal: {@code cooked_camel}, {@code raw_strider}. When
     * no word of the name means anything, such a food is meat.
     */
    private static final Set<String> ANIMAL_PREFIXES = Set.of("raw", "cooked", "grilled", "roasted",
            "fried", "smoked", "seared", "braised");

    /** Compound words that END with a food word but are not that food. */
    private static final Set<String> NOT_COMPOUNDS = Set.of("slime", "slimeball", "slimeballs", "spear",
            "update", "candidate", "basalt");

    private FoodWords() {
    }

    /**
     * The nutrient set a convention subtag's name implies, or empty.
     *
     * @param path the part after the root, {@code raw_dragon_meat} for {@code c:foods/raw_dragon_meat}
     */
    public static Set<Nutrient> forTagPath(String path) {
        String[] parts = path.split("[_/]");
        Set<Nutrient> part = Set.of();
        for (int i = parts.length - 1; i >= 0; i--) {
            Set<Nutrient> found = forWord(parts[i]);
            if (found.isEmpty()) {
                continue;
            }
            if (PART_WORDS.contains(parts[i])) {
                if (part.isEmpty()) {
                    part = found;
                }
                continue;
            }
            if (DISH_WORDS.contains(parts[i])) {
                Set<Nutrient> dish = new LinkedHashSet<>(found);
                for (int k = i - 1; k >= 0; k--) {
                    Set<Nutrient> ingredient = forWord(parts[k]);
                    if (!ingredient.isEmpty() && !DISH_WORDS.contains(parts[k])) {
                        dish.addAll(ingredient);
                        break;
                    }
                }
                return dish;
            }
            return found;
        }
        if (!part.isEmpty()) {
            return new LinkedHashSet<>(part);
        }
        if (parts.length > 1 && ANIMAL_PREFIXES.contains(parts[0])) {
            return new LinkedHashSet<>(List.of(Nutrients.PROTEIN, Nutrients.FAT));
        }
        return Set.of();
    }

    /**
     * The nutrient set the LAST word of a name implies, or empty. For flat tag names
     * ({@code c:bananas}, {@code c:beef_jerkies}), where reading further back would be wrong:
     * {@code c:orange_dyes} is a dye, not an orange.
     */
    public static Set<Nutrient> forLastWord(String path) {
        String[] parts = path.split("[_/]");
        return parts.length == 0 ? Set.of() : forWord(parts[parts.length - 1]);
    }

    /**
     * One word, then its singular: Croptopia names its tags and items in the plural
     * ({@code bananas}, {@code jerkies}, {@code tomatoes}), and the list holds the singular.
     */
    private static Set<Nutrient> forWord(String part) {
        Set<Nutrient> exact = exactWord(part);
        if (!exact.isEmpty() || NOT_COMPOUNDS.contains(part)) {
            return exact;
        }
        // Pam's HarvestCraft names its items as one word with "item" on the end:
        // applefritteritem is an apple fritter.
        String word = part.length() > 6 && part.endsWith("item") ? part.substring(0, part.length() - 4) : part;
        if (!word.equals(part)) {
            exact = exactWord(word);
            if (!exact.isEmpty()) {
                return exact;
            }
        }
        // A compound, read by its longest known ending: cottagepie is a pie, lanternfish a fish.
        for (int start = 1; start <= word.length() - 3; start++) {
            String tail = word.substring(start);
            if (tail.length() < 4 && !SHORT_SUFFIXES.contains(tail)) {
                break;
            }
            if (NOT_COMPOUNDS.contains(tail)) {
                continue;
            }
            Set<Nutrient> found = exactWord(tail);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return Set.of();
    }

    private static Set<Nutrient> exactWord(String part) {
        for (String candidate : singulars(part)) {
            for (Word word : WORDS) {
                if (word.words().contains(candidate)) {
                    return new LinkedHashSet<>(word.nutrients());
                }
            }
        }
        return Set.of();
    }

    private static List<String> singulars(String part) {
        List<String> out = new ArrayList<>(3);
        out.add(part);
        if (part.length() > 4 && part.endsWith("ies")) {
            out.add(part.substring(0, part.length() - 3) + "y");
        }
        if (part.length() > 3 && part.endsWith("es")) {
            out.add(part.substring(0, part.length() - 2));
        }
        if (part.length() > 3 && part.endsWith("s") && !part.endsWith("ss")) {
            out.add(part.substring(0, part.length() - 1));
        }
        return out;
    }

    /**
     * Words that only name a container or a serving when they LEAD a name: {@code plate_of_stuffed_hoglin}
     * is a serving of {@code stuffed_hoglin}.
     */
    private static final List<String> SERVED_IN = List.of("plate_of_", "bowl_of_", "cup_of_", "mug_of_",
            "glass_of_", "bottle_of_", "jar_of_", "slice_of_", "piece_of_", "serving_of_");

    /**
     * The names this item could be a serving or a variant of, in order:
     * <ol>
     *   <li>{@code <name>_block}: the feast block a serving comes from ({@code dragon_meat_stew} from
     *       {@code dragon_meat_stew_block}), handed out by right-clicking with a bowl, no recipe.</li>
     *   <li>The name with trailing words dropped, longest first: {@code mushroom_stew_cup} gives
     *       {@code mushroom_stew}, then {@code mushroom}. A trailing word is usually the container or the
     *       portion.</li>
     *   <li>The name without a leading container ({@link #SERVED_IN}), then that with trailing words
     *       dropped.</li>
     * </ol>
     * Other leading words are never dropped: they change what the food IS ({@code cooked_beef} is not
     * {@code beef} for this purpose, and the recipe walk already finds it).
     */
    public static List<String> stems(String path) {
        List<String> stems = new ArrayList<>();
        stems.add(path + "_block");
        addTrailing(stems, path);
        for (String lead : SERVED_IN) {
            if (path.startsWith(lead) && path.length() > lead.length()) {
                String rest = path.substring(lead.length());
                stems.add(rest);
                addTrailing(stems, rest);
            }
        }
        return stems;
    }

    private static void addTrailing(List<String> stems, String path) {
        int cut = path.lastIndexOf('_');
        while (cut > 0) {
            stems.add(path.substring(0, cut));
            cut = path.lastIndexOf('_', cut - 1);
        }
    }

    /** Words that only name a container or a portion when they END a name. */
    private static final Set<String> SERVING_WORDS = Set.of("cup", "bowl", "bottle", "glass", "mug",
            "jar", "plate", "slice", "slices", "piece", "pieces", "serving", "side", "portion");

    /**
     * Whether {@code name} is {@code stem} served differently, so the stem's real values carry over:
     * {@code mushroom_stew_cup} is mushroom stew, but {@code beef_omelet} is not beef. Only container
     * and portion words may lie between them, at the end ({@link #SERVING_WORDS}) or the start
     * ({@code plate_of_}, {@code bowl_of_}...).
     */
    public static boolean isServing(String name, String stem) {
        if (name.startsWith(stem + "_")) {
            for (String word : name.substring(stem.length() + 1).split("_")) {
                if (!SERVING_WORDS.contains(word)) {
                    return false;
                }
            }
            return true;
        }
        for (String lead : SERVED_IN) {
            if (name.equals(lead + stem)) {
                return true;
            }
        }
        return false;
    }

    /** Number of word groups, so an audit can assert the table has not been silently emptied. */
    public static int groupCount() {
        return WORDS.size();
    }
}
