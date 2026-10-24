package com.example.myfabricmod.module;

/** A value a module exposes in the menu (a number or an on/off switch). */
public abstract class Setting {
    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String key() {
        return Keys.of(name);
    }

    /** Value as text, for saving to the config file. */
    public abstract String serialize();

    /** Restores the value from text read from the config file. */
    public abstract void deserialize(String text);
}
