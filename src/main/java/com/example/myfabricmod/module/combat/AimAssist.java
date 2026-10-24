package com.example.myfabricmod.module.combat;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Turns your view towards the closest player in range. */
public final class AimAssist extends ClientModule {
    private final NumberSetting range = addSetting(new NumberSetting("Range", 6.0, 1.0, 12.0, 0.5));
    // 100 = snap straight onto the target ("lock"). Lower values turn more slowly.
    private final NumberSetting speed = addSetting(new NumberSetting("Speed %", 100, 5, 100, 5));
    private final BooleanSetting visibleOnly = addSetting(new BooleanSetting("Visible only", true));

    public AimAssist() {
        super("Aim Assist", "Locks your aim onto the closest player in range", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.screen != null) {
            return; // don't turn the camera while a menu is open
        }
        LocalPlayer self = mc.player;
        Player target = findTarget(mc, self);
        if (target == null) {
            return;
        }

        Vec3 eye = self.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        // Minecraft yaw 0 = facing south (+Z), positive pitch = looking down.
        float wantedYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantedPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));

        float strength = (float) (speed.get() / 100.0);
        float yawStep = Mth.wrapDegrees(wantedYaw - self.getYRot()) * strength;
        float pitchStep = (wantedPitch - self.getXRot()) * strength;

        self.setYRot(self.getYRot() + yawStep);
        self.setXRot(Mth.clamp(self.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    private Player findTarget(Minecraft mc, LocalPlayer self) {
        double maxDistanceSqr = range.get() * range.get();
        Player best = null;
        double bestDistanceSqr = maxDistanceSqr;
        for (Player other : mc.level.players()) {
            if (other == self || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            double distanceSqr = self.distanceToSqr(other);
            if (distanceSqr > bestDistanceSqr) {
                continue;
            }
            if (visibleOnly.get() && !self.hasLineOfSight(other)) {
                continue;
            }
            best = other;
            bestDistanceSqr = distanceSqr;
        }
        return best;
    }
}
