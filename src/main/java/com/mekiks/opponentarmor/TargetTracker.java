// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Mekiks

package com.mekiks.opponentarmor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Decides which player the HUD shows. A hit keeps that player up for the
 * configured hold time; aiming (when enabled) only shows a player while the
 * crosshair is actually on them, plus a short grace period so the panel doesn't
 * flicker at the edge of their hitbox.
 */
public final class TargetTracker {

    private static final long AIM_GRACE_MS = 250;

    private static @Nullable UUID hitTarget;
    private static long lastHitMs;
    private static @Nullable UUID aimTarget;
    private static long lastAimMs;

    private TargetTracker() {
    }

    /** Called whenever the local player swings at an entity. */
    public static void onAttack(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            hitTarget = player.getUuid();
            lastHitMs = System.currentTimeMillis();
        }
    }

    /** Called every client tick to follow the crosshair. */
    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            hitTarget = null;
            aimTarget = null;
            return;
        }
        if (ArmorConfig.showOnAim && client.targetedEntity instanceof PlayerEntity player
                && player != client.player) {
            aimTarget = player.getUuid();
            lastAimMs = System.currentTimeMillis();
        }
    }

    /** The player to show right now, or {@code null} if nothing should be drawn. */
    public static @Nullable PlayerEntity current(MinecraftClient client) {
        if (client.world == null) {
            return null;
        }
        long now = System.currentTimeMillis();

        // Whoever is under the crosshair right now wins.
        if (ArmorConfig.showOnAim && aimTarget != null && now - lastAimMs <= AIM_GRACE_MS) {
            PlayerEntity aimed = alive(client, aimTarget);
            if (aimed != null) {
                return aimed;
            }
        }
        // Otherwise the last player we hit, for the configured hold time.
        if (hitTarget != null && now - lastHitMs <= ArmorConfig.holdSeconds * 1000L) {
            return alive(client, hitTarget);
        }
        return null;
    }

    private static @Nullable PlayerEntity alive(MinecraftClient client, UUID id) {
        PlayerEntity player = client.world.getPlayerByUuid(id);
        return player != null && player.isAlive() ? player : null;
    }
}
