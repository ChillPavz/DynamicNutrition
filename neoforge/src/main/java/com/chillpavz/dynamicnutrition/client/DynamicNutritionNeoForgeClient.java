package com.chillpavz.dynamicnutrition.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.chillpavz.dynamicnutrition.Constants;
import com.chillpavz.dynamicnutrition.config.ClothCompat;

/**
 * Client wiring for NeoForge. Only ever touched from the client dist, so nothing here can load on a
 * dedicated server.
 */
public final class DynamicNutritionNeoForgeClient {

    private DynamicNutritionNeoForgeClient() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        NutritionEffectDetails.install();

        // Keybind registration is a MOD bus event; the tick is a GAME bus event. Same split as
        // everywhere else in this mod, and getting it wrong fails silently.
        modBus.addListener(RegisterKeyMappingsEvent.class, event ->
                event.register(DynamicNutritionKeys.OPEN_SCREEN));

        // Also a MOD bus event. Registered above the food level layer so the strip draws over the
        // same region the hunger bar owns rather than under it.
        modBus.addListener(RegisterGuiLayersEvent.class, event ->
                event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                        Identifier.fromNamespaceAndPath(Constants.MOD_ID, "nutrients"),
                        NutritionHud::render));

        // Init.Post is a GAME bus event and fires again whenever the screen re-inits, which the
        // inventory does when the recipe book is toggled and the panel moves.
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> {
            if (!NutritionButton.enabled()
                    || !(event.getScreen() instanceof InventoryScreen inventory)) {
                return;
            }
            event.addListener(new NutritionButton(
                    NutritionButton.x(inventory.leftPos),
                    NutritionButton.y(inventory.topPos), inventory));
        });

        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, event ->
                FoodTooltip.appendTo(event.getItemStack(), event.getToolTip(),
                        event.getFlags().isAdvanced(), event.getEntity()));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            Minecraft client = Minecraft.getInstance();
            // No "is a screen open" guard: Minecraft exposes no current-screen accessor at 26.2 and
            // vanilla only dispatches keybinds while no screen is open.
            while (DynamicNutritionKeys.OPEN_SCREEN.consumeClick()) {
                if (client.player != null) {
                    client.setScreenAndShow(new NutritionScreen(null));
                }
            }
        });
        // The mods-list Config button. Registered only when cloth is actually present, because a
        // factory that always returns null gives the player an enabled button that does nothing.
        if (ClothCompat.isAvailable()) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> {
                Screen screen = ClothCompat.screen(parent);
                return screen == null ? parent : screen;
            });
        }

        Constants.LOG.debug("Client keybind and screen registered");
    }
}
