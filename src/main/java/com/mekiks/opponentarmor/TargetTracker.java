package com.mekiks.opponentarmor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Remembers the last player we hit (or aimed at, if enabled) and for how long
 * that player should stay on the HUD.
 */
public final class TargetTracker {

    private static @Nullable UUID target;
    private static long lastSeenMs;

    private TargetTracker() {
    }

    /** Called whenever the local player swings at an entity. */
    public static void onAttack(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            mark(player);
        }
    }

    /** Called every client tick; keeps an aimed-at player alive when that option is on. */
    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            target = null;
            return;
        }
        if (ArmorConfig.showOnAim && client.targetedEntity instanceof PlayerEntity player
                && player != client.player) {
            mark(player);
        }
    }

    private static void mark(PlayerEntity player) {
        target = player.getUuid();
        lastSeenMs = System.currentTimeMillis();
    }

    /** The player to show right now, or {@code null} if nothing should be drawn. */
    public static @Nullable PlayerEntity current(MinecraftClient client) {
        if (target == null || client.world == null) {
            return null;
        }
        if (System.currentTimeMillis() - lastSeenMs > ArmorConfig.holdSeconds * 1000L) {
            target = null;
            return null;
        }
        PlayerEntity player = client.world.getPlayerByUuid(target);
        if (player == null || !player.isAlive()) {
            return null;
        }
        return player;
    }
}
