package com.example.myfabricmod.render;

import com.example.myfabricmod.MyFabricModClient;
import com.example.myfabricmod.module.render.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the mod HUD (FPS, XYZ, health, armor) in the top-left corner.
 * Called every frame from a HudElementRegistry element.
 * Uses the game's default font (Minecraft's own bitmap font).
 */
public final class HudOverlay {
    private static final int PADDING = 4;
    private static final int LINE_H  = 10;

    public static void render(GuiGraphics graphics) {
        HudModule hud = (HudModule) MyFabricModClient.modules().find(HudModule.class);
        if (hud == null || !hud.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return; // don't overlap an open screen

        Player player = mc.player;
        int alpha = (int)(hud.opacity.get() * 2.55); // 0–100 → 0–255
        int bg    = (alpha << 24) | 0x000000;        // semi-transparent black
        int text  = 0xFFFFFFFF;

        int x = PADDING;
        int y = PADDING;
        int lineCount = 0;
        if (hud.showFps.get())    lineCount++;
        if (hud.showCoords.get()) lineCount++;
        if (hud.showHealth.get()) lineCount++;
        if (hud.showArmor.get())  lineCount++;

        // background slab
        int bgW = 96;
        graphics.fill(x - 2, y - 2, x + bgW, y + lineCount * LINE_H + 2, bg);

        if (hud.showFps.get()) {
            int fps = mc.getFps();
            graphics.drawString(mc.font, "FPS: " + fps, x, y, text, false);
            y += LINE_H;
        }
        if (hud.showCoords.get()) {
            int bx = (int) Math.floor(player.getX());
            int by = (int) Math.floor(player.getY());
            int bz = (int) Math.floor(player.getZ());
            graphics.drawString(mc.font, "XYZ: " + bx + " " + by + " " + bz, x, y, text, false);
            y += LINE_H;
        }
        if (hud.showHealth.get()) {
            int hp = (int) Math.ceil(player.getHealth());
            graphics.drawString(mc.font, "HP: " + hp, x, y, text, false);
            y += LINE_H;
        }
        if (hud.showArmor.get()) {
            int armor = player.getArmorValue();
            graphics.drawString(mc.font, "Armor: " + armor, x, y, text, false);
        }
    }
}
