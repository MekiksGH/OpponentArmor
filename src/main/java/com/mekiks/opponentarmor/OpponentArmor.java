// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Mekiks

package com.mekiks.opponentarmor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

/**
 * Client entrypoint: tracks the player you're fighting and draws their armor
 * durability in a screen corner. Settings live behind Mod Menu or a keybind.
 */
public class OpponentArmor implements ClientModInitializer {

    public static final String MOD_ID = "opponentarmor";

    private static KeyBinding openSettings;
    private static KeyBinding toggle;

    @Override
    public void onInitializeClient() {
        ArmorConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.opponentarmor.settings", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), category));
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.opponentarmor.toggle", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), category));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                TargetTracker.onAttack(entity);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TargetTracker.tick(client);
            while (openSettings.wasPressed()) {
                client.setScreen(new OpponentArmorScreen(client.currentScreen));
            }
            while (toggle.wasPressed()) {
                ArmorConfig.enabled = !ArmorConfig.enabled;
                ArmorConfig.save();
            }
        });

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "panel"), (ctx, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!ArmorConfig.enabled || client.options.hudHidden
                    || client.currentScreen instanceof OpponentArmorScreen) {
                return;
            }
            PlayerEntity target = TargetTracker.current(client);
            if (target == null) {
                return;
            }
            ItemStack[] armor = {
                    target.getEquippedStack(EquipmentSlot.HEAD),
                    target.getEquippedStack(EquipmentSlot.CHEST),
                    target.getEquippedStack(EquipmentSlot.LEGS),
                    target.getEquippedStack(EquipmentSlot.FEET)
            };
            SkinTextures skin = target instanceof AbstractClientPlayerEntity p
                    ? p.getSkin() : DefaultSkinHelper.getSkinTextures(target.getUuid());
            ArmorHudRenderer.render(ctx, client.textRenderer, target.getName().getString(), skin, armor);
        });
    }
}
