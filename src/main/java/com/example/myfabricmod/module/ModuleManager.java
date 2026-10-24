package com.example.myfabricmod.module;

import com.example.myfabricmod.MyFabricModClient;
import com.example.myfabricmod.module.combat.AimAssist;
import com.example.myfabricmod.module.combat.CrystalMacro;
import com.example.myfabricmod.module.render.BlockEsp;
import com.example.myfabricmod.module.render.HudModule;
import com.example.myfabricmod.module.render.PlayerEsp;
import com.example.myfabricmod.module.render.StorageEsp;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Owns all modules, runs them every tick and saves their settings.
 * Modules always start switched off; only their settings are remembered.
 */
public final class ModuleManager {
    private final List<ClientModule> modules = new ArrayList<>();
    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("my-fabric-mod.properties");

    public ModuleManager() {
        // Combat
        register(new AimAssist());
        register(new CrystalMacro());

        // Render
        register(new PlayerEsp());
        register(new StorageEsp());
        register(new BlockEsp());
        register(new HudModule());

        load();
    }

    private void register(ClientModule module) {
        modules.add(module);
    }

    public List<ClientModule> all() {
        return Collections.unmodifiableList(modules);
    }

    /** Returns the first registered module of the given class, or null. */
    @SuppressWarnings("unchecked")
    public <T extends ClientModule> T find(Class<T> clazz) {
        for (ClientModule m : modules) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }

    public List<ClientModule> inCategory(Category category) {
        List<ClientModule> result = new ArrayList<>();
        for (ClientModule module : modules) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        for (ClientModule module : modules) {
            if (!module.isEnabled()) {
                continue;
            }
            try {
                module.onTick(mc);
            } catch (Throwable error) {
                // A broken module is switched off instead of crashing the game.
                MyFabricModClient.LOGGER.error("Module '{}' crashed and was switched off", module.name(), error);
                module.setEnabled(false);
            }
        }
    }

    private void load() {
        if (!Files.exists(configFile)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configFile)) {
            properties.load(reader);
        } catch (IOException | RuntimeException error) {
            MyFabricModClient.LOGGER.warn("Could not read {}", configFile, error);
            return;
        }
        for (ClientModule module : modules) {
            for (Setting setting : module.settings()) {
                String saved = properties.getProperty(module.key() + "." + setting.key());
                if (saved == null) {
                    continue;
                }
                try {
                    setting.deserialize(saved);
                } catch (RuntimeException ignored) {
                    // Bad value in the file: keep the default.
                }
            }
        }
    }

    public void save() {
        Properties properties = new Properties();
        for (ClientModule module : modules) {
            for (Setting setting : module.settings()) {
                properties.setProperty(module.key() + "." + setting.key(), setting.serialize());
            }
        }
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                properties.store(writer, "My Fabric Mod settings");
            }
        } catch (IOException error) {
            MyFabricModClient.LOGGER.warn("Could not save {}", configFile, error);
        }
    }
}
