package com.example.myfabricmod.module;

/** An on/off switch shown in the menu. */
public final class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean value) {
        super(name);
        this.value = value;
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public String serialize() {
        return Boolean.toString(value);
    }

    @Override
    public void deserialize(String text) {
        value = Boolean.parseBoolean(text.trim());
    }
}
