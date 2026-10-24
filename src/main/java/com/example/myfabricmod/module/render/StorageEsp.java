package com.example.myfabricmod.module.render;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.ColorSetting;
import net.minecraft.client.Minecraft;

/**
 * Storage ESP – draws boxes and optional tracers around storage block entities
 * (chests, barrels, hoppers, ender chests, shulker boxes).
 */
public final class StorageEsp extends ClientModule {
    public final BooleanSetting chests     = addSetting(new BooleanSetting("Chests",       true));
    public final BooleanSetting barrels    = addSetting(new BooleanSetting("Barrels",      true));
    public final BooleanSetting hoppers    = addSetting(new BooleanSetting("Hoppers",      true));
    public final BooleanSetting enderChests = addSetting(new BooleanSetting("Ender Chests", true));
    public final BooleanSetting shulkers   = addSetting(new BooleanSetting("Shulker Boxes", true));
    public final ColorSetting color        = addSetting(new ColorSetting("Color",          0xFFFFAA00));
    public final BooleanSetting tracers    = addSetting(new BooleanSetting("Tracers",      false));

    public StorageEsp() {
        super("Storage ESP", "Highlights chests and other storage blocks", Category.RENDER);
    }

    @Override
    public void onTick(Minecraft mc) {
        // Rendering happens in EspOverlay.
    }
}
