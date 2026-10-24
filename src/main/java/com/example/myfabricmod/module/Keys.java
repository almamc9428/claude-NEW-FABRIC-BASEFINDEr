package com.example.myfabricmod.module;

import java.util.Locale;

/** Turns display names like "Aim Assist" into config keys like "aim_assist". */
final class Keys {
    private Keys() {
    }

    static String of(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
