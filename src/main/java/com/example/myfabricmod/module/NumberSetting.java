package com.example.myfabricmod.module;

import java.util.Locale;

/** A number that is changed in steps with the - and + buttons in the menu. */
public final class NumberSetting extends Setting {
    private final double min;
    private final double max;
    private final double step;
    private double value;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    public double get() {
        return value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public void set(double newValue) {
        double stepped = Math.round((newValue - min) / step) * step + min;
        this.value = Math.max(min, Math.min(max, stepped));
    }

    public void increase() {
        set(value + step);
    }

    public void decrease() {
        set(value - step);
    }

    /** Text shown between the - and + buttons. */
    public String display() {
        if (step >= 1.0) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public String serialize() {
        return Double.toString(value);
    }

    @Override
    public void deserialize(String text) {
        set(Double.parseDouble(text.trim()));
    }
}
