package com.chillpavz.dynamicnutrition.nutrition;

import com.chillpavz.dynamicnutrition.Constants;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fluids a recipe takes, for recipes that keep them apart from their item ingredients.
 *
 * <p>Create's processing recipes (mixing, filling, compacting) and every add-on built on them hold
 * milk, cream, honey and chocolate as fluid ingredients, outside {@code getIngredients()}. Walked
 * from its items alone, an ice cream made of sugar and milk was sugar and nothing else. Each fluid
 * stands in the walk as its bucket, which is where vanilla already keeps milk's values.
 *
 * <p>Everything here is reflection on names that loaders and mods do not remap: Create's
 * {@code getFluidIngredients()}, then {@code getMatchingFluidStacks()} (Forge and Fabric) or
 * {@code getFluids()} (NeoForge's {@code SizedFluidIngredient}), then each stack's {@code getFluid()}.
 * A recipe without them, or a shape this does not know, gives no fluids; nothing here throws.
 */
public final class RecipeFluids {

    private static final Method NONE;
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
    private static volatile boolean warned;

    static {
        try {
            NONE = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private RecipeFluids() {
    }

    /**
     * The fluid objects a recipe takes, the first matching fluid of each fluid ingredient, in order.
     * Empty for a recipe with no fluid ingredients. The caller casts each to its band's fluid type.
     */
    public static List<Object> fluidsOf(Object recipe) {
        if (recipe == null) {
            return Collections.emptyList();
        }
        try {
            Method list = lookup(recipe.getClass(), "getFluidIngredients");
            if (list == NONE) {
                return Collections.emptyList();
            }
            Object ingredients = list.invoke(recipe);
            if (!(ingredients instanceof Collection<?> all) || all.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> fluids = new ArrayList<>();
            for (Object ingredient : all) {
                Object fluid = firstFluid(ingredient);
                if (fluid != null) {
                    fluids.add(fluid);
                }
            }
            return fluids;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Constants.LOG.warn("Could not read the fluid ingredients of {}; recipes of that kind are walked "
                        + "from their items only", recipe.getClass().getName(), t);
            }
            return Collections.emptyList();
        }
    }

    /**
     * The fluid objects a recipe makes ({@code getFluidResults()}), in order. A mixer that makes cream
     * frosting makes a fluid, not an item, so its bucket is only found as that recipe's output if the
     * index files the recipe under the fluid's bucket too. Empty when there are none.
     */
    public static List<Object> fluidResultsOf(Object recipe) {
        if (recipe == null) {
            return Collections.emptyList();
        }
        try {
            Method list = lookup(recipe.getClass(), "getFluidResults");
            if (list == NONE) {
                return Collections.emptyList();
            }
            Object results = list.invoke(recipe);
            if (!(results instanceof Collection<?> all) || all.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> fluids = new ArrayList<>();
            for (Object stack : all) {
                if (stack == null) {
                    continue;
                }
                Method fluid = lookup(stack.getClass(), "getFluid");
                Object f = fluid == NONE ? null : fluid.invoke(stack);
                if (f != null) {
                    fluids.add(f);
                }
            }
            return fluids;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Constants.LOG.warn("Could not read the fluid results of {}; recipes of that kind are indexed "
                        + "by their item results only", recipe.getClass().getName(), t);
            }
            return Collections.emptyList();
        }
    }

    private static Object firstFluid(Object ingredient) throws Exception {
        if (ingredient == null) {
            return null;
        }
        Method matching = lookup(ingredient.getClass(), "getMatchingFluidStacks");
        if (matching == NONE) {
            matching = lookup(ingredient.getClass(), "getFluids");
        }
        if (matching == NONE) {
            return null;
        }
        Object stacks = matching.invoke(ingredient);
        Object first = null;
        if (stacks instanceof List<?> l && !l.isEmpty()) {
            first = l.get(0);
        } else if (stacks instanceof Object[] a && a.length > 0) {
            first = a[0];
        }
        if (first == null) {
            return null;
        }
        Method fluid = lookup(first.getClass(), "getFluid");
        return fluid == NONE ? null : fluid.invoke(first);
    }

    /** A public no-argument method by name, cached per class and name; {@link #NONE} when there is none. */
    private static Method lookup(Class<?> type, String name) {
        return METHODS.computeIfAbsent(type.getName() + "#" + name, k -> {
            Method m;
            try {
                m = type.getMethod(name);
            } catch (NoSuchMethodException | SecurityException e) {
                return NONE;
            }
            try {
                // A public method found on a non-public class (Create's tag-based fluid ingredient
                // is a nested class) cannot be invoked through that class without this.
                m.setAccessible(true);
            } catch (RuntimeException ignored) {
                // A module that does not open the package: invoke may still work, if not the
                // caller's catch turns it into no fluids.
            }
            return m;
        });
    }
}
