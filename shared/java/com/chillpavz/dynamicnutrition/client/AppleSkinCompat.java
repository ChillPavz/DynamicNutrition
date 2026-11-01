package com.chillpavz.dynamicnutrition.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.DynamicNutrition;
import com.chillpavz.dynamicnutrition.nutrition.Ketosis;
import com.chillpavz.dynamicnutrition.nutrition.Nutrients;
import com.chillpavz.dynamicnutrition.nutrition.NutritionValues;
import com.chillpavz.dynamicnutrition.nutrition.Satiety;
import com.chillpavz.dynamicnutrition.platform.Services;

/**
 * Makes AppleSkin's saturation preview show what a meal will really give, satiety and ketosis
 * included. Through its public {@code FoodValuesEvent}, by reflection, because AppleSkin is optional
 * and the event has three shapes: a Fabric {@code EVENT} field or a loader's event bus, holding
 * {@code FoodValues} up to 1.20.1 and a {@code FoodProperties} after it. Read in the 2.5 and 3.0 jars.
 * Anything unexpected turns this off with one log line, and the preview just stays vanilla's.
 */
final class AppleSkinCompat {

    private static final String EVENT = "squeek.appleskin.api.event.FoodValuesEvent";

    private static boolean registered;
    private static boolean failed;

    private AppleSkinCompat() {
    }

    /** Safe to call often; only the first call does anything. */
    static void register() {
        if (registered || !Services.PLATFORM.isModLoaded("appleskin")) {
            return;
        }
        registered = true;
        try {
            Class<?> event = Class.forName(EVENT);
            Field fabric = null;
            for (Field field : event.getFields()) {
                if (field.getName().equals("EVENT")) {
                    fabric = field;
                }
            }
            if (fabric != null) {
                registerFabric(fabric);
            } else {
                registerBus(event);
            }
            Constants.LOG.info("AppleSkin's preview will show this mod's saturation");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Constants.LOG.warn("Could not reach AppleSkin's food event: {}", e.toString());
        }
    }

    private static void registerFabric(Field field) throws ReflectiveOperationException {
        Class<?> handler = Class.forName("squeek.appleskin.api.handler.EventHandler");
        Object listener = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[] {handler},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("hashCode") ? System.identityHashCode(proxy)
                                : method.getName().equals("equals") ? proxy == args[0] : "AppleSkinCompat";
                    }
                    apply(args[0]);
                    return null;
                });
        Class.forName("net.fabricmc.fabric.api.event.Event").getMethod("register", Object.class)
                .invoke(field.get(null), listener);
    }

    private static void registerBus(Class<?> event) throws ReflectiveOperationException {
        Consumer<Object> listener = AppleSkinCompat::apply;
        Object bus;
        try {
            bus = Class.forName("net.neoforged.neoforge.common.NeoForge").getField("EVENT_BUS").get(null);
        } catch (ClassNotFoundException e) {
            bus = Class.forName("net.minecraftforge.common.MinecraftForge").getField("EVENT_BUS").get(null);
            Class<?> priority = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object normal = Enum.valueOf((Class) priority, "NORMAL");
            Class<?> face = Class.forName("net.minecraftforge.eventbus.api.IEventBus");
            face.getMethod("addListener", priority, boolean.class, Class.class, Consumer.class)
                    .invoke(bus, normal, false, event, listener);
            return;
        }
        Class.forName("net.neoforged.bus.api.IEventBus").getMethod("addListener", Class.class, Consumer.class)
                .invoke(bus, event, listener);
    }

    /** The saturation multiplier this player gets from this food. */
    private static float factor(Player player, ItemStack stack) {
        NutritionValues values = DynamicNutrition.table().cached(stack.getItem());
        if (values == null || values.isEmpty()) {
            return 1.0F;
        }
        float factor = Satiety.factor(values.get(Nutrients.CARBOHYDRATES), values.get(Nutrients.PROTEIN),
                values.get(Nutrients.FAT), values.total());
        return Satiety.scaled(Services.STORAGE.get(player).inKetosis() ? factor * Ketosis.SATIETY : factor);
    }

    private static void apply(Object event) {
        if (failed) {
            return;
        }
        try {
            Class<?> type = event.getClass();
            // Older AppleSkin on Fabric leaves the player out, and this only ever runs on the client.
            Player player = (Player) type.getField("player").get(event);
            if (player == null) {
                player = Minecraft.getInstance().player;
            }
            if (player == null) {
                return;
            }
            float factor = factor(player, (ItemStack) type.getField("itemStack").get(event));
            if (factor == 1.0F) {
                return;
            }
            for (Field field : type.getFields()) {
                if (field.getName().startsWith("modifiedFood")) {
                    Object food = field.get(event);
                    if (food != null) {
                        field.set(event, field.getType().isRecord() ? record(food, factor) : values(food, factor));
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            Constants.LOG.warn("Could not adjust AppleSkin's preview: {}", e.toString());
        }
    }

    /** {@code FoodValues}, up to 1.20.1: hunger and a saturation modifier. */
    private static Object values(Object food, float factor) throws ReflectiveOperationException {
        Class<?> type = food.getClass();
        int hunger = type.getField("hunger").getInt(food);
        float saturation = type.getField("saturationModifier").getFloat(food);
        return type.getConstructor(int.class, float.class).newInstance(hunger, saturation * factor);
    }

    /** {@code FoodProperties}: a record whose first float is the saturation. */
    private static Object record(Object food, float factor) throws ReflectiveOperationException {
        RecordComponent[] parts = food.getClass().getRecordComponents();
        Object[] args = new Object[parts.length];
        Class<?>[] types = new Class<?>[parts.length];
        boolean done = false;
        for (int i = 0; i < parts.length; i++) {
            Method accessor = parts[i].getAccessor();
            accessor.setAccessible(true);
            args[i] = accessor.invoke(food);
            types[i] = parts[i].getType();
            if (!done && types[i] == float.class) {
                args[i] = (float) args[i] * factor;
                done = true;
            }
        }
        Constructor<?> constructor = food.getClass().getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }
}
