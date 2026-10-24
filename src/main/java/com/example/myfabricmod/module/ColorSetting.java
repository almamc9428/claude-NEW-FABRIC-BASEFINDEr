package com.example.myfabricmod.module;

import java.util.Locale;

/**
 * An ARGB color stored as a packed int. Serialized as hex (e.g. "FF4488FF").
 * In the menu it shows as a small colored swatch; full color picker is a future feature.
 */
public final class ColorSetting extends Setting {
    private int argb;

    public ColorSetting(String name, int argb) {
        super(name);
        this.argb = argb;
    }

    /** Packed ARGB (alpha in the highest byte). */
    public int get() { return argb; }

    public void set(int argb) { this.argb = argb; }

    public int alpha() { return (argb >> 24) & 0xFF; }
    public int red()   { return (argb >> 16) & 0xFF; }
    public int green() { return (argb >>  8) & 0xFF; }
    public int blue()  { return  argb        & 0xFF; }

    @Override
    public String serialize() {
        return String.format(Locale.ROOT, "%08X", argb & 0xFFFFFFFFL);
    }

    @Override
    public void deserialize(String text) {
        argb = (int) Long.parseLong(text.trim(), 16);
    }
}
