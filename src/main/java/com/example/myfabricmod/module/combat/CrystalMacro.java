package com.example.myfabricmod.module.combat;

import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * While you hold right click with an end crystal in your main hand:
 * <ul>
 *   <li>on obsidian or bedrock it places a crystal on top, as fast as the server allows;</li>
 *   <li>on any other block it first places obsidian (taken from your hotbar) against the face you
 *       are looking at and puts a crystal on top of it, in the same tick;</li>
 *   <li>with "Break crystals" on, it also hits the crystal you are looking at, so the loop
 *       place - break - place repeats while you hold right click.</li>
 * </ul>
 * With "Debug messages" on it tells you in the action bar when the server refuses something.
 */
public final class CrystalMacro extends ClientModule {
    /** Don't re-send a placement for the same block within this many ticks (the server needs time to answer). */
    private static final int RETRY_TICKS = 3;
    /** How long to wait for the server's answer before reporting that it refused a placement. */
    private static final int VERIFY_OBSIDIAN_TICKS = 8;
    private static final int VERIFY_CRYSTAL_TICKS = 10;

    private final NumberSetting delay = addSetting(new NumberSetting("Delay (ticks)", 0, 0, 10, 1));
    private final BooleanSetting breakCrystals = addSetting(new BooleanSetting("Break crystals", false));
    private final BooleanSetting debug = addSetting(new BooleanSetting("Debug messages", true));

    private int cooldown;
    private int ticksSinceLast = 100;
    private int messageCooldown;
    private BlockPos lastBase;

    private BlockPos pendingObsidian;
    private int pendingObsidianTicks;
    private BlockPos pendingCrystal;
    private int pendingCrystalTicks;

    public CrystalMacro() {
        super("Crystal Macro", "Click: obsidian + crystal. Hold right click: spam", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        cooldown = 0;
        lastBase = null;
        pendingObsidian = null;
        pendingCrystal = null;
    }

    @Override
    public void onTick(Minecraft mc) {
        ticksSinceLast++;
        if (cooldown > 0) {
            cooldown--;
        }
        if (messageCooldown > 0) {
            messageCooldown--;
        }
        verifyPlacements(mc);

        if (mc.screen != null || !mc.options.keyUse.isDown()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (!player.getMainHandItem().is(Items.END_CRYSTAL)) {
            return;
        }
        if (cooldown > 0) {
            return;
        }

        HitResult target = mc.hitResult;

        // Looking at a crystal: hit it if "Break crystals" is on.
        if (target instanceof EntityHitResult entityHit) {
            if (breakCrystals.get() && entityHit.getEntity() instanceof EndCrystal crystal) {
                breakCrystal(mc, crystal);
            }
            return;
        }

        if (!(target instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        ClientLevel level = mc.level;
        BlockPos clicked = hit.getBlockPos();
        if (isCrystalBase(level.getBlockState(clicked))) {
            if (placeCrystal(mc, clicked)) {
                cooldown = delay.getInt();
            }
            return;
        }
        placeObsidianThenCrystal(mc, hit);
    }

    private void breakCrystal(Minecraft mc, EndCrystal crystal) {
        LocalPlayer player = mc.player;
        // The crosshair only picks entities that are within hitting range, so no extra distance check is needed.
        mc.gameMode.attack(player, crystal);
        player.swing(InteractionHand.MAIN_HAND);
        lastBase = null; // allow placing the next crystal right away
        cooldown = delay.getInt();
    }

    private void placeObsidianThenCrystal(Minecraft mc, BlockHitResult hit) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        BlockPos clicked = hit.getBlockPos();

        // Replaceable blocks (tall grass, snow...) get replaced instead of built against.
        BlockPos obsidianPos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        if (!level.getBlockState(obsidianPos).canBeReplaced()) {
            return;
        }
        if (!level.getBlockState(obsidianPos.above()).isAir()) {
            report(mc, "no room for a crystal above that spot");
            return;
        }

        Inventory inventory = player.getInventory();
        int obsidianSlot = findHotbarSlot(inventory, Items.OBSIDIAN);
        if (obsidianSlot < 0) {
            say(mc, "put obsidian in your hotbar");
            return;
        }

        int crystalSlot = inventory.getSelectedSlot();
        selectSlot(player, obsidianSlot);
        InteractionResult result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        selectSlot(player, crystalSlot);
        if (!result.consumesAction()) {
            report(mc, "can't place obsidian there (something is in the way)");
            return;
        }
        player.swing(InteractionHand.MAIN_HAND);
        pendingObsidian = obsidianPos.immutable();
        pendingObsidianTicks = VERIFY_OBSIDIAN_TICKS;

        placeCrystal(mc, obsidianPos);
        cooldown = delay.getInt();
    }

    /** Puts a crystal on top of the given obsidian/bedrock block. Returns true if a placement was sent. */
    private boolean placeCrystal(Minecraft mc, BlockPos base) {
        if (base.equals(lastBase) && ticksSinceLast < RETRY_TICKS) {
            return false;
        }
        ClientLevel level = mc.level;
        if (!isCrystalBase(level.getBlockState(base))) {
            return false;
        }
        BlockPos above = base.above();
        if (!level.getBlockState(above).isAir()) {
            report(mc, "no room above that block");
            return false;
        }
        // Same space check the game uses: nothing may stand in the 1x2x1 area above the base.
        AABB space = new AABB(above.getX(), above.getY(), above.getZ(), above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
        List<Entity> inTheWay = level.getEntities((Entity) null, space);
        for (Entity entity : inTheWay) {
            if (!(entity instanceof EndCrystal)) {
                report(mc, "something is standing where the crystal goes");
                return false;
            }
        }
        if (!inTheWay.isEmpty()) {
            return false; // a crystal is already there
        }

        Vec3 topCenter = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        BlockHitResult topFace = new BlockHitResult(topCenter, Direction.UP, base, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topFace);
        mc.player.swing(InteractionHand.MAIN_HAND);

        lastBase = base.immutable();
        ticksSinceLast = 0;
        pendingCrystal = lastBase;
        pendingCrystalTicks = VERIFY_CRYSTAL_TICKS;
        return true;
    }

    /** Checks that the server really accepted what was sent, and says so if it did not. */
    private void verifyPlacements(Minecraft mc) {
        if (pendingObsidian != null && --pendingObsidianTicks <= 0) {
            if (!mc.level.getBlockState(pendingObsidian).is(Blocks.OBSIDIAN)) {
                report(mc, "the server removed the obsidian (protected area, too far or anti-cheat)");
            }
            pendingObsidian = null;
        }
        if (pendingCrystal != null) {
            BlockPos above = pendingCrystal.above();
            AABB space = new AABB(above.getX(), above.getY(), above.getZ(), above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
            if (!mc.level.getEntitiesOfClass(EndCrystal.class, space).isEmpty()) {
                pendingCrystal = null; // the crystal showed up, all good
            } else if (--pendingCrystalTicks <= 0) {
                report(mc, "the server did not place the crystal");
                pendingCrystal = null;
            }
        }
    }

    /** Changes the held hotbar slot and tells the server about it right away. */
    private static void selectSlot(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    /** Action bar message that is always shown (e.g. missing obsidian). */
    private void say(Minecraft mc, String text) {
        if (messageCooldown > 0) {
            return;
        }
        mc.player.displayClientMessage(Component.literal("Crystal Macro: " + text), true);
        messageCooldown = 20;
    }

    /** Action bar message that is only shown when "Debug messages" is on. */
    private void report(Minecraft mc, String text) {
        if (debug.get()) {
            say(mc, text);
        }
    }

    private static boolean isCrystalBase(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    private static int findHotbarSlot(Inventory inventory, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }
}
