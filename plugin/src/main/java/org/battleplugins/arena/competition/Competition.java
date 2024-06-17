package org.battleplugins.arena.competition;

import org.battleplugins.arena.Arena;
import org.battleplugins.arena.competition.map.CompetitionMap;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a competition. Competitions are a representation of an
 * active {@link Arena}. Where an Arena will contain all the actual
 * game logic, a Competition will contain all the logic for the live
 * competition itself (such as the game timer, score, etc).
 * <p>
 * Competitions are also responsible for handling the lifecycle of
 * an Arena. This includes starting, stopping, and resetting the
 * Arena.
 */
public interface Competition<T extends Competition<T>> extends CompetitionLike<T> {

    /**
     * Gets the type of competition.
     *
     * @return the type of competition
     */
    CompetitionType<T> getType();

    /**
     * Gets the map for this competition.
     *
     * @return the map for this competition
     */
    CompetitionMap<T> getMap();

    /**
     * Gets the current phase of the competition.
     *
     * @return the current phase of the competition
     */
    CompetitionPhaseType<T, ?> getPhase();

    /**
     * Gets whether the player can join the competition.
     *
     * @param role the role of the player
     */
    CompletableFuture<JoinResult> canJoin(Player player, PlayerRole role);

    /**
     * Adds the player to the competition.
     *
     * @param player the player to join
     * @param type the type of join
     */
    void join(Player player, PlayerRole type);

    /**
     * Removes the player from the competition.
     *
     * @param player the player to leave
     */
    void leave(Player player);

    @Override
    default T getCompetition() {
        return (T) this;
    }
}