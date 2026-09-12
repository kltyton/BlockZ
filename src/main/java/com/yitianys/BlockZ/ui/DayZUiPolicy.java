package com.yitianys.BlockZ.ui;

import net.minecraft.world.entity.player.Player;

/**
 * Selects the inventory rules from the player's current game mode.
 */
public final class DayZUiPolicy {
    private DayZUiPolicy() {
    }

    public static boolean shouldUseDayZ(Player player) {
        return player != null && !player.isCreative();
    }
}
