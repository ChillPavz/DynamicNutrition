package com.chillpavz.dynamicnutrition.command;

import com.chillpavz.dynamicnutrition.DynamicNutrition;
import com.chillpavz.dynamicnutrition.nutrition.NutritionOrigin;
import com.chillpavz.dynamicnutrition.nutrition.NutritionTable;
import com.chillpavz.dynamicnutrition.nutrition.Nutrient;
import com.chillpavz.dynamicnutrition.nutrition.Nutrients;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 * The second file of the export: every item that is not food but passed something on to a food, with
 * the share of each nutrient it passes (in percent) and where that came from. Wheat, sugar or a milk
 * bucket decide the shape of hundreds of dishes and never appear in the food list, so without this a
 * pack author cannot see why a dish came out the way it did.
 */
final class IngredientExport {

    private IngredientExport() {
    }

    /** Writes {@code <export>-ingredients.csv} next to the food export. Returns the rows written. */
    static int write(Path foodsFile, Collection<Item> foods) throws IOException {
        NutritionTable table = DynamicNutrition.table();
        Map<String, Item> found = new TreeMap<>();
        Deque<Item> todo = new ArrayDeque<>();
        for (Item food : foods) {
            todo.addAll(table.origin(food).from());
        }
        while (!todo.isEmpty()) {
            Item item = todo.poll();
            if (NutritionTable.isEdible(item)) {
                continue;
            }
            if (found.put(String.valueOf(BuiltInRegistries.ITEM.getKey(item)), item) == null) {
                // An ingredient's own ingredients: flour is wheat, a dough is flour and water.
                todo.addAll(table.origin(item).from());
            }
        }

        String name = foodsFile.getFileName().toString().replace(".csv", "-ingredients.csv");
        int written = 0;
        try (Writer w = Files.newBufferedWriter(foodsFile.resolveSibling(name), StandardCharsets.UTF_8)) {
            w.write("item");
            for (Nutrient nutrient : Nutrients.all()) {
                w.write("," + nutrient.name());
            }
            w.write(",source,derived_from" + System.lineSeparator());
            for (Map.Entry<String, Item> entry : found.entrySet()) {
                float[] shape = table.shape(entry.getValue());
                NutritionOrigin origin = table.origin(entry.getValue());
                StringBuilder row = new StringBuilder(entry.getKey());
                for (int i = 0; i < Nutrients.all().size(); i++) {
                    row.append(',').append(shape == null ? 0 : Math.round(shape[i] * 100));
                }
                row.append(',').append(origin.source().name().toLowerCase());
                row.append(',');
                for (int i = 0; i < origin.from().size(); i++) {
                    if (i > 0) {
                        row.append(' ');
                    }
                    row.append(BuiltInRegistries.ITEM.getKey(origin.from().get(i)));
                }
                w.write(row.append(System.lineSeparator()).toString());
                written++;
            }
        }
        return written;
    }
}
