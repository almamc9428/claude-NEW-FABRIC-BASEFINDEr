# My Fabric Mod

A client-side Fabric mod for Minecraft Java Edition 1.21.11 with a module menu.

## Using it
- **Right Shift** opens the menu by default, **Esc** (or the ×) closes it.
- To change the open key go to **Options > Controls > Key Binds**, find the **My Fabric Mod** section and rebind **Open menu**.
- Click a category on the left to see its modules.
- Click a module's title row to switch it **ON/OFF**. Modules always start OFF when the game starts.
- Number settings use the **−** and **+** buttons, on/off settings are flipped with a click.
- Color settings show a colored swatch (editing colors by typing them in is coming next).
- The mouse wheel scrolls long lists, the search bar finds modules by name across all categories.
- Settings are saved to `config/my-fabric-mod.properties` in your Minecraft folder.

---

## Modules

### Combat

**Aim Assist** – turns your view onto the closest visible player in range.
| Setting | Default | Range | Notes |
|---|---|---|---|
| Range | 6.0 | 1–12 | Blocks |
| Speed % | 100 | 5–100 | 100 = hard-lock, lower = smooth |
| Visible only | ON | – | Skip players behind walls |

**Crystal Macro** – needs an end crystal in your **main hand**, obsidian in your **hotbar**, hold right click.
- Looking at obsidian/bedrock → places a crystal on top instantly.
- Looking at any other block → places obsidian then a crystal in the same tick.

| Setting | Default | Notes |
|---|---|---|
| Delay (ticks) | 0 | Pause between placements |
| Break crystals | OFF | Also hits the crystal you are looking at (spam loop) |
| Debug messages | ON | Shows server refusals above hotbar |

---

### Render

**Player ESP** – highlights every other player through walls with a 2D box.
| Setting | Default |
|---|---|
| Color | Cyan (0xFF44CCFF) |
| Tracers | OFF |

**Storage ESP** – highlights storage block entities in loaded chunks.
| Setting | Default |
|---|---|
| Chests | ON |
| Barrels | ON |
| Hoppers | ON |
| Ender Chests | ON |
| Shulker Boxes | ON |
| Color | Orange (0xFFFFAA00) |
| Tracers | OFF |

**Block ESP** – highlights every occurrence of one chosen block type in loaded chunks.
The default target block is `minecraft:diamond_ore`. To change it: open the mod config file
(`config/my-fabric-mod.properties`) and edit the `block_esp.target_block` key (e.g.
`block_esp.target_block=minecraft:ancient_debris`), then reload the world.
| Setting | Default |
|---|---|
| Color | Green (0xFF00FFAA) |
| Tracers | OFF |

**HUD** – draws FPS, coordinates, health and armor in the top-left corner.
| Setting | Default | Notes |
|---|---|---|
| FPS | ON | – |
| Coordinates | ON | Block X Y Z |
| Health | ON | Rounded up |
| Armor | ON | Armor value |
| Opacity % | 80 | Background transparency |

---

## Requirements
- Minecraft 1.21.11 with Fabric Loader 0.18 or newer and Fabric API 0.141.1+1.21.11
- JDK 21 to build it yourself

## Build
Run `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows). Output: `build/libs/my-fabric-mod-1.0.0.jar`.

### Build without installing anything (GitHub Actions)
Push this folder to a GitHub repository. The workflow in `.github/workflows/build.yml` builds the mod on every push.  
Open the **Actions** tab → latest run → download the `my-fabric-mod` artifact.

## Adding a module
1. Create a class that extends `ClientModule` (see `module/combat/AimAssist.java` for an example).
2. Add settings with `addSetting(new NumberSetting(...))`, `addSetting(new BooleanSetting(...))` or `addSetting(new ColorSetting(...))`.
3. Put the logic in `onTick`, which runs every game tick while the module is ON. For rendering modules, add your drawing code to `EspOverlay` or `HudOverlay` and call `modules().find(YourModule.class)` to check its settings.
4. Register it in the `ModuleManager` constructor. The menu picks it up automatically.

## Code layout
```
MyFabricModClient.java        entry point, keybind, events, HUD hooks
module/
  ClientModule.java           base class for every feature
  Setting.java / BooleanSetting.java / NumberSetting.java / ColorSetting.java
  ModuleManager.java          registers all modules, tick loop, save/load
  combat/AimAssist.java
  combat/CrystalMacro.java
  render/PlayerEsp.java
  render/StorageEsp.java
  render/BlockEsp.java
  render/HudModule.java
render/
  EspRenderer.java            world→screen projection (captures matrices each frame)
  EspOverlay.java             draws ESP boxes and tracer lines on the HUD canvas
  HudOverlay.java             draws the info HUD (FPS / XYZ / HP / armor)
gui/ClickGuiScreen.java       the menu opened with Right Shift
```
