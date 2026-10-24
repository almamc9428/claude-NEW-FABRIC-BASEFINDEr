package com.example.myfabricmod.module.render;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.ColorSetting;
import net.minecraft.client.Minecraft;

/**
 * ESP for players – draws a bounding box and optional tracer line around every other player,
 * even through walls. Rendering is done in EspOverlay via HudElementRegistry.
 */
public final class PlayerEsp extends ClientModule {
    public final ColorSetting color    = addSetting(new ColorSetting("Color",   0xFF44CCFF));
    public final BooleanSetting tracers = addSetting(new BooleanSetting("Tracers", false));

    public PlayerEsp() {
        super("Player ESP", "Highlights players through walls", Category.RENDER);
    }

    @Override
    public void onTick(Minecraft mc) {
        // Rendering happens in EspOverlay; nothing to do here.
    }
}
