package com.example.weaponauras;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class WeaponAurasClient implements ClientModInitializer {
    private static final double HELD_RANGE_SQ = 32 * 32;
    private static final double DROP_RANGE = 24;
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        AuraConfig.load();
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.weaponauras.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_K, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new AuraScreen(null));
            }
            tick(mc);
        });
    }

    private static void tick(Minecraft mc) {
        if (!AuraConfig.enabled || mc.level == null || mc.player == null || mc.isPaused()) return;
        ClientLevel level = mc.level;
        LocalPlayer self = mc.player;
        AuraEmitter.beginTick(mc);

        if (AuraConfig.held) {
            for (AbstractClientPlayer p : level.players()) {
                if (p.isSpectator() || (p != self && p.isInvisible())) continue;
                if (p.distanceToSqr(self) > HELD_RANGE_SQ) continue;
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = p.getItemInHand(hand);
                    AuraProfile prof = AuraRegistry.get(stack);
                    if (prof.isEmpty()) continue;

                    double yaw = Math.toRadians(p.getYRot());
                    Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                    Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                    boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (p.getMainArm() == HumanoidArm.RIGHT);
                    double side = rightHand ? 0.38 : -0.38;

                    Vec3 handPos = new Vec3(p.getX(), p.getEyeY() - 0.5, p.getZ())
                            .add(fwd.scale(0.45)).add(right.scale(side));
                    Vec3 dir = p.getLookAngle().scale(0.6).add(0, 0.8, 0).normalize();
                    AuraEmitter.emit(level, prof, handPos, dir, 0.95, 1.0, 0.05);
                }
            }
        }

        if (AuraConfig.dropped && (level.getGameTime() & 1) == 0) {
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, self.getBoundingBox().inflate(DROP_RANGE), x -> !x.isRemoved())) {
                AuraProfile prof = AuraRegistry.get(e.getItem());
                if (prof.isEmpty()) continue;
                Vec3 base = e.position().add(0, 0.12, 0);
                AuraEmitter.emit(level, prof, base, new Vec3(0, 1, 0), 0.55, 1.6, 0.14);
            }
        }
    }
}
