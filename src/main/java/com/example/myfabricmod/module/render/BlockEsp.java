package com.example.myfabricmod.module.render;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Block ESP – highlights every occurrence of a specific block type within the loaded chunks.
 * The target block is set by its namespace:path identifier (e.g. "minecraft:diamond_ore").
 */
public final class BlockEsp extends ClientModule {
    /** The block to search for. Also acts as the "name" for the EspOverlay lookup. */
    public volatile Identifier targetBlock = Identifier.withDefaultNamespace("diamond_ore");
    public final ColorSetting color     = addSetting(new ColorSetting("Color",   0xFF00FFAA));
    public final BooleanSetting tracers = addSetting(new BooleanSetting("Tracers", false));

    public BlockEsp() {
        super("Block ESP", "Highlights a chosen block through walls", Category.RENDER);
    }

    @Override
    public void onTick(Minecraft mc) {
        // Rendering happens in EspOverlay.
    }
}
