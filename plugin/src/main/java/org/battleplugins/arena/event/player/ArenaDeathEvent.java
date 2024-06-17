package org.battleplugins.arena.event.player;

import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.event.EventTrigger;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player dies in an arena.
 */
@EventTrigger("on-death")
public class ArenaDeathEvent extends BukkitArenaPlayerEvent {
    private final static HandlerList HANDLERS = new HandlerList();

    public ArenaDeathEvent(ArenaPlayer player) {
        super(player.getArena(), player);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}