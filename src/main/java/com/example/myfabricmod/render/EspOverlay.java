package com.example.myfabricmod.render;

import com.example.myfabricmod.MyFabricModClient;
import com.example.myfabricmod.module.render.BlockEsp;
import com.example.myfabricmod.module.render.PlayerEsp;
import com.example.myfabricmod.module.render.StorageEsp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.Map;

/**
 * Draws ESP boxes and tracer lines over the game view for Player ESP, Storage ESP and Block ESP.
 * Called every frame from a HudElementRegistry element (registered in MyFabricModClient).
 *
 * <p>All drawing is 2D on the HUD canvas, using world→screen projection from {@link EspRenderer}.
 */
public final class EspOverlay {

    public static void render(GuiGraphics graphics, float partialTick) {
        EspRenderer esp = EspRenderer.get();
        if (!esp.isReady()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer localPlayer = mc.player;
        if (level == null || localPlayer == null) return;

        PlayerEsp playerEsp   = (PlayerEsp)   MyFabricModClient.modules().find(PlayerEsp.class);
        StorageEsp storageEsp = (StorageEsp)  MyFabricModClient.modules().find(StorageEsp.class);
        BlockEsp blockEsp     = (BlockEsp)    MyFabricModClient.modules().find(BlockEsp.class);

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        float cx = guiW / 2.0f;
        float cy = guiH * 0.85f; // tracer origin near crosshair / hotbar

        // ── Player ESP ─────────────────────────────────────────────────────────
        if (playerEsp != null && playerEsp.isEnabled()) {
            int color = playerEsp.color.get();
            boolean tracers = playerEsp.tracers.get();
            for (Player player : level.players()) {
                if (player == localPlayer) continue;
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                double h  = player.getBbHeight();
                double w  = player.getBbWidth() / 2.0;
                AABB box = new AABB(px - w, py, pz - w, px + w, py + h, pz + w);
                drawBox(graphics, esp, box, color);
                if (tracers) {
                    drawTracer(graphics, esp, cx, cy, px, py + h / 2, pz, color);
                }
            }
        }

        // ── Storage ESP ────────────────────────────────────────────────────────
        if (storageEsp != null && storageEsp.isEnabled()) {
            int color   = storageEsp.color.get();
            boolean tracers = storageEsp.tracers.get();

            // Iterate loaded chunks for block entities
            for (LevelChunk chunk : loadedChunks(level)) {
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    BlockPos pos = entry.getKey();
                    if (!isStorageTarget(storageEsp, be)) continue;

                    AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                       pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                    drawBox(graphics, esp, box, color);
                    if (tracers) {
                        drawTracer(graphics, esp, cx, cy,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color);
                    }
                }
            }
        }

        // ── Block ESP ──────────────────────────────────────────────────────────
        if (blockEsp != null && blockEsp.isEnabled() && blockEsp.targetBlock != null) {
            Block target = BuiltInRegistries.BLOCK.get(blockEsp.targetBlock).orElse(null);
            if (target != null && target != Blocks.AIR) {
                int color   = blockEsp.color.get();
                boolean tracers = blockEsp.tracers.get();
                for (LevelChunk chunk : loadedChunks(level)) {
                    for (BlockPos pos : blockPositionsInChunk(level, chunk, target)) {
                        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                           pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                        drawBox(graphics, esp, box, color);
                        if (tracers) {
                            drawTracer(graphics, esp, cx, cy,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color);
                        }
                    }
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Draws a 2D bounding box outline projected from a world-space AABB. */
    private static void drawBox(GuiGraphics g, EspRenderer esp, AABB box, int color) {
        // Project all 8 corners, find screen bounding rect
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};

        float minSx = Float.MAX_VALUE, minSy = Float.MAX_VALUE;
        float maxSx = Float.MIN_VALUE, maxSy = Float.MIN_VALUE;
        boolean anyVisible = false;

        for (double wx : xs) for (double wy : ys) for (double wz : zs) {
            float[] s = esp.project(wx, wy, wz);
            if (s == null) continue;
            anyVisible = true;
            if (s[0] < minSx) minSx = s[0];
            if (s[1] < minSy) minSy = s[1];
            if (s[0] > maxSx) maxSx = s[0];
            if (s[1] > maxSy) maxSy = s[1];
        }
        if (!anyVisible) return;

        int x1 = (int) minSx, y1 = (int) minSy;
        int x2 = (int) maxSx, y2 = (int) maxSy;
        int t = 1; // line thickness
        // top, bottom, left, right edges
        g.fill(x1, y1, x2, y1 + t, color);
        g.fill(x1, y2 - t, x2, y2, color);
        g.fill(x1, y1, x1 + t, y2, color);
        g.fill(x2 - t, y1, x2, y2, color);
    }

    /** Draws a line from the HUD tracer origin to the projected world point. */
    private static void drawTracer(GuiGraphics g, EspRenderer esp,
                                   float fromX, float fromY,
                                   double wx, double wy, double wz, int color) {
        float[] s = esp.project(wx, wy, wz);
        if (s == null) return;
        drawLine(g, fromX, fromY, s[0], s[1], color);
    }

    /** Bresenham-style 1-pixel line using fill() calls. */
    private static void drawLine(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
        int dx = Math.abs((int)(x1 - x0));
        int dy = Math.abs((int)(y1 - y0));
        int steps = Math.max(1, Math.max(dx, dy));
        float sx = (x1 - x0) / steps;
        float sy = (y1 - y0) / steps;
        float cx = x0, cy2 = y0;
        for (int i = 0; i <= steps; i += 2) {
            g.fill((int) cx, (int) cy2, (int) cx + 1, (int) cy2 + 1, color);
            cx += sx * 2;
            cy2 += sy * 2;
        }
    }

    // ── Level iteration helpers ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Iterable<LevelChunk> loadedChunks(ClientLevel level) {
        // ClientChunkCache exposes a getLoadedChunks() or similar; we access it via
        // the level's chunk source. Cast is unchecked but safe at runtime.
        try {
            var source = level.getChunkSource();
            // ClientChunkCache has storage field; iterate using its allChunks() method.
            // Fallback: return the visible entities' surrounding chunks.
            java.lang.reflect.Method m = source.getClass().getMethod("allLoadedChunks");
            return (Collection<LevelChunk>) m.invoke(source);
        } catch (Exception ignored) {}

        // Safe fallback: only iterate the player's vicinity (±16 chunks).
        var player = Minecraft.getInstance().player;
        if (player == null) return java.util.Collections.emptyList();
        int cx = (int) player.getX() >> 4;
        int cz = (int) player.getZ() >> 4;
        java.util.List<LevelChunk> result = new java.util.ArrayList<>();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk != null) result.add(chunk);
            }
        }
        return result;
    }

    private static boolean isStorageTarget(StorageEsp cfg, BlockEntity be) {
        BlockEntityType<?> type = be.getType();
        if (cfg.chests.get() &&
            (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST)) return true;
        if (cfg.barrels.get()    && type == BlockEntityType.BARREL)     return true;
        if (cfg.hoppers.get()    && type == BlockEntityType.HOPPER)     return true;
        if (cfg.enderChests.get() && type == BlockEntityType.ENDER_CHEST) return true;
        if (cfg.shulkers.get()   && type == BlockEntityType.SHULKER_BOX) return true;
        return false;
    }

    private static Iterable<BlockPos> blockPositionsInChunk(ClientLevel level, LevelChunk chunk, Block target) {
        java.util.List<BlockPos> result = new java.util.ArrayList<>();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        for (int x = baseX; x < baseX + 16; x++) {
            for (int z = baseZ; z < baseZ + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(target)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }
}
