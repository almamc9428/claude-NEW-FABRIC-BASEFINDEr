package com.example.myfabricmod.module.render;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * HUD – shows FPS, coordinates, health and armor above the vanilla hotbar.
 * Each element can be toggled independently. Rendering is done in HudOverlay.
 */
public final class HudModule extends ClientModule {
    public final BooleanSetting showFps    = addSetting(new BooleanSetting("FPS",         true));
    public final BooleanSetting showCoords = addSetting(new BooleanSetting("Coordinates", true));
    public final BooleanSetting showHealth = addSetting(new BooleanSetting("Health",      true));
    public final BooleanSetting showArmor  = addSetting(new BooleanSetting("Armor",       true));
    public final NumberSetting  opacity    = addSetting(new NumberSetting( "Opacity %",   80, 10, 100, 5));

    public HudModule() {
        super("HUD", "Shows stats on screen", Category.RENDER);
    }

    @Override
    public void onTick(Minecraft mc) {
        // Rendering happens in HudOverlay.
    }
}
