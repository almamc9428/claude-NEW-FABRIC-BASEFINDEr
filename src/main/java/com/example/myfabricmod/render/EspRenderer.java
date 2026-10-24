package com.example.myfabricmod.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Projects world-space points to GUI screen coordinates using matrices captured during
 * WorldRenderEvents.END_EXTRACTION. Thread-confined to the render/tick thread.
 */
public final class EspRenderer {
    private static final EspRenderer INSTANCE = new EspRenderer();

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private Vec3 cameraPos = Vec3.ZERO;
    private boolean ready = false;

    private EspRenderer() {}

    public static EspRenderer get() { return INSTANCE; }

    /** Called from WorldRenderEvents.END_EXTRACTION each frame. */
    public void update(Matrix4fc viewMatrix, Matrix4fc cullProjectionMatrix, Vec3 camera) {
        view.set(viewMatrix);
        proj.set(cullProjectionMatrix);
        cameraPos = camera;
        ready = true;
    }

    /** Returns whether matrices are available for this frame. */
    public boolean isReady() { return ready; }

    /**
     * Projects a world point to screen pixel coordinates [x, y].
     * Returns null if the point is behind the camera.
     */
    public float[] project(double wx, double wy, double wz) {
        Minecraft mc = Minecraft.getInstance();
        float dx = (float)(wx - cameraPos.x);
        float dy = (float)(wy - cameraPos.y);
        float dz = (float)(wz - cameraPos.z);

        // view * pos
        Vector4f clip = new Vector4f(dx, dy, dz, 1.0f);
        view.transform(clip);
        proj.transform(clip);

        if (clip.w <= 0) return null; // behind camera

        float ndcX =  clip.x / clip.w;
        float ndcY = -clip.y / clip.w; // y flipped

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();

        float sx = (ndcX * 0.5f + 0.5f) * guiW;
        float sy = (ndcY * 0.5f + 0.5f) * guiH;
        return new float[]{sx, sy};
    }
}
