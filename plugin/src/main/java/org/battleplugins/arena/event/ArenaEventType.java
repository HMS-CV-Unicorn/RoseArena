package org.battleplugins.arena.event;

import org.battleplugins.arena.event.arena.ArenaDrawEvent;
import org.battleplugins.arena.event.arena.ArenaLoseEvent;
import org.battleplugins.arena.event.arena.ArenaPhaseCompleteEvent;
import org.battleplugins.arena.event.arena.ArenaPhaseStartEvent;
import org.battleplugins.arena.event.arena.ArenaVictoryEvent;
import org.battleplugins.arena.event.player.ArenaDeathEvent;
import org.battleplugins.arena.event.player.ArenaJoinEvent;
import org.battleplugins.arena.event.player.ArenaKillEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;
import org.battleplugins.arena.event.player.ArenaRespawnEvent;
import org.battleplugins.arena.event.player.ArenaSpectateEvent;
import org.battleplugins.arena.event.player.ArenaStatChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ArenaEventType<T extends ArenaEvent> {
    private static final Map<String, ArenaEventType<?>> EVENT_TYPES = new HashMap<>();

    public static final ArenaEventType<ArenaPhaseCompleteEvent> ON_COMPLETE = new ArenaEventType<>("on-complete", ArenaPhaseCompleteEvent.class);
    public static final ArenaEventType<ArenaDeathEvent> ON_DEATH = new ArenaEventType<>("on-death", ArenaDeathEvent.class);
    public static final ArenaEventType<ArenaDrawEvent> ON_DRAW = new ArenaEventType<>("on-draw", ArenaDrawEvent.class);
    public static final ArenaEventType<ArenaKillEvent> ON_KILL = new ArenaEventType<>("on-kill", ArenaKillEvent.class);
    public static final ArenaEventType<ArenaJoinEvent> ON_JOIN = new ArenaEventType<>("on-join", ArenaJoinEvent.class);
    public static final ArenaEventType<ArenaLeaveEvent> ON_LEAVE = new ArenaEventType<>("on-leave", ArenaLeaveEvent.class);
    public static final ArenaEventType<ArenaLoseEvent> ON_LOSE = new ArenaEventType<>("on-lose", ArenaLoseEvent.class);
    public static final ArenaEventType<ArenaRespawnEvent> ON_RESPAWN = new ArenaEventType<>("on-respawn", ArenaRespawnEvent.class);
    public static final ArenaEventType<ArenaSpectateEvent> ON_SPECTATE = new ArenaEventType<>("on-spectate", ArenaSpectateEvent.class);
    public static final ArenaEventType<ArenaPhaseStartEvent> ON_START = new ArenaEventType<>("on-start", ArenaPhaseStartEvent.class);
    public static final ArenaEventType<ArenaStatChangeEvent> ON_STAT_CHANGE = new ArenaEventType<>("on-stat-change", ArenaStatChangeEvent.class);
    public static final ArenaEventType<ArenaVictoryEvent> ON_VICTORY = new ArenaEventType<>("on-victory", ArenaVictoryEvent.class);

    private final Class<T> clazz;

    ArenaEventType(String name, Class<T> clazz) {
        this.clazz = clazz;

        EVENT_TYPES.put(name, this);
    }

    public Class<T> getEventType() {
        return this.clazz;
    }

    @Nullable
    public static ArenaEventType<?> get(String name) {
        return EVENT_TYPES.get(name);
    }

    public static <E extends ArenaEvent> ArenaEventType<E> create(String name, Class<E> clazz) {
        return new ArenaEventType<>(name, clazz);
    }
}