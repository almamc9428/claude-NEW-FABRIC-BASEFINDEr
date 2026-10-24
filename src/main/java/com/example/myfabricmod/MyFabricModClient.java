package com.example.myfabricmod;

import com.example.myfabricmod.gui.ClickGuiScreen;
import com.example.myfabricmod.module.ModuleManager;
import com.example.myfabricmod.render.EspOverlay;
import com.example.myfabricmod.render.EspRenderer;
import com.example.myfabricmod.render.HudOverlay;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyFabricModClient implements ClientModInitializer {
    public static final String MOD_ID = "my-fabric-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModuleManager modules;
    private static KeyMapping openMenuKey;

    /** All modules. Available once the game has started. */
    public static ModuleManager modules() {
        return modules;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();

        // ── Keybind: open menu ──────────────────────────────────────────────────
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key." + MOD_ID + ".open_menu",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        // ── Capture view/projection matrices for ESP projection ─────────────────
        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            EspRenderer.get().update(
                ctx.viewMatrix(),
                ctx.cullProjectionMatrix(),
                ctx.camera().getPosition()
            );
        });

        // ── ESP overlay (drawn after vanilla chat so it's on top of everything) ─
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "esp_overlay"),
            (graphics, delta) -> EspOverlay.render(graphics, delta.getGameTimeDeltaPartialTick(true))
        );

        // ── Info HUD (FPS, coords, health, armor) ───────────────────────────────
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.INFO_BAR,
            Identifier.fromNamespaceAndPath(MOD_ID, "hud_overlay"),
            (graphics, delta) -> HudOverlay.render(graphics)
        );

        // ── Module tick + menu open ─────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            modules.tick(client);

            while (openMenuKey.consumeClick()) {
                if (client.screen == null && client.level != null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
        });

        LOGGER.info("My Fabric Mod loaded. Right Shift opens the menu by default.");
    }
}
