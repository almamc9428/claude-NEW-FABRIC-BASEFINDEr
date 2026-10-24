package com.example.myfabricmod.module;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every feature. Extend it, add settings with {@link #addSetting(Setting)} and
 * put the logic in {@link #onTick(Minecraft)}, then register the module in {@link ModuleManager}.
 */
public abstract class ClientModule {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    protected ClientModule(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected final <T extends Setting> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final Category category() {
        return category;
    }

    public final String key() {
        return Keys.of(name);
    }

    public final List<Setting> settings() {
        return Collections.unmodifiableList(settings);
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Called once per game tick while the module is on and the player is in a world. */
    public abstract void onTick(Minecraft mc);
}
