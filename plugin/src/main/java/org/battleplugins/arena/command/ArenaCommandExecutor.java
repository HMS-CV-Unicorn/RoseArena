package org.battleplugins.arena.command;

import org.battleplugins.arena.Arena;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.competition.Competition;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.PlayerRole;
import org.battleplugins.arena.competition.map.CompetitionMap;
import org.battleplugins.arena.competition.map.LiveCompetitionMap;
import org.battleplugins.arena.editor.ArenaEditorWizards;
import org.battleplugins.arena.editor.WizardStage;
import org.battleplugins.arena.editor.context.MapCreateContext;
import org.battleplugins.arena.editor.type.MapOption;
import org.battleplugins.arena.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class ArenaCommandExecutor extends BaseCommandExecutor {
    protected final Arena arena;

    public ArenaCommandExecutor(Arena arena) {
        this(arena.getName().toLowerCase(Locale.ROOT), arena);
    }

    public ArenaCommandExecutor(String parentCommand, Arena arena) {
        super(parentCommand, arena.getName().toLowerCase(Locale.ROOT));

        this.arena = arena;
    }

    @ArenaCommand(commands = { "join", "j" }, description = "Join an arena.", permissionNode = "join")
    public void join(Player player) {
        List<LiveCompetitionMap<?>> maps = this.arena.getPlugin().getMaps(this.arena);
        if (maps.isEmpty()) {
            Messages.NO_OPEN_ARENAS.send(player);
            return;
        }

        this.join(player, maps.iterator().next());
    }

    @ArenaCommand(commands = { "join", "j" }, description = "Join an arena.", permissionNode = "join")
    public void join(Player player, CompetitionMap<?> map) {
        System.out.println("Running w/ " + map);
        if (ArenaPlayer.getArenaPlayer(player) != null) {
            Messages.ALREADY_IN_ARENA.send(player);
            return;
        }

        if (map == null) {
            Messages.NO_ARENA_WITH_NAME.send(player);
            return;
        }

        List<Competition<?>> competitions = this.arena.getPlugin().getCompetitions(this.arena, map.getName());
        this.arena.getPlugin().findJoinableCompetition(competitions, player, PlayerRole.PLAYING).whenCompleteAsync((competition, e) -> {
            if (e != null) {
                Messages.ARENA_ERROR.send(player, e.getMessage());
                this.arena.getPlugin().error("An error occurred while joining the arena", e);
                return;
            }

            if (competition != null) {
                competition.join(player, PlayerRole.PLAYING);

                Messages.ARENA_JOINED.send(player, competition.getMap().getName());
            } else {
                // Try and create a dynamic competition if possible
                this.arena.getPlugin()
                        .getOrCreateCompetition(this.arena, player, PlayerRole.PLAYING, map.getName())
                        .whenComplete((newCompetition, ex) -> {
                            if (ex != null) {
                                Messages.ARENA_ERROR.send(player, ex.getMessage());
                                this.arena.getPlugin().error("An error occurred while joining the arena", ex);
                                return;
                            }

                            if (newCompetition == null) {
                                // No competition - something happened that stopped the
                                // dynamic arena from being created. Not much we can do here,
                                // but info will be in console in the event of an error
                                Messages.ARENA_NOT_JOINABLE.send(player);
                                return;
                            }

                            newCompetition.join(player, PlayerRole.PLAYING);
                            Messages.ARENA_JOINED.send(player, newCompetition.getMap().getName());
                        });
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(this.arena.getPlugin()));
    }

    @ArenaCommand(commands = { "spectate", "s" }, description = "Spectate an arena.", permissionNode = "spectate")
    public void spectate(Player player) {
        List<Competition<?>> competitions = this.arena.getPlugin().getCompetitions(this.arena);
        if (competitions.isEmpty()) {
            Messages.NO_OPEN_ARENAS.send(player);
            return;
        }

        Competition<?> competition = competitions.iterator().next();
        this.spectate(player, competition);
    }

    @ArenaCommand(commands = { "spectate", "s" }, description = "Spectate an arena.", permissionNode = "spectate")
    public void spectate(Player player, Competition<?> competition) {
        if (ArenaPlayer.getArenaPlayer(player) != null) {
            Messages.ALREADY_IN_ARENA.send(player);
            return;
        }

        if (competition == null) {
            Messages.NO_ARENA_WITH_NAME.send(player);
            return;
        }

        competition.canJoin(player, PlayerRole.SPECTATING).whenComplete((result, e) -> {
            if (e != null) {
                Messages.ARENA_ERROR.send(player, e.getMessage());
                this.arena.getPlugin().error("An error occurred while spectating the arena", e);
                return;
            }

            if (result.canJoin()) {
                competition.join(player, PlayerRole.SPECTATING);

                Messages.ARENA_SPECTATE.send(player, competition.getMap().getName());
            } else {
                if (result.getMessage() != null) {
                    result.getMessage().send(player);
                } else {
                    Messages.ARENA_NOT_SPECTATABLE.send(player);
                }
            }
        });
    }

    @ArenaCommand(commands = { "leave", "l" }, description = "Leave an arena.", permissionNode = "leave")
    public void leave(Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            Messages.NOT_IN_ARENA.send(player);
            return;
        }

        arenaPlayer.getCompetition().leave(player);
        Messages.ARENA_LEFT.send(player, arenaPlayer.getCompetition().getMap().getName());
    }

    @ArenaCommand(commands = "create", description = "Create a new arena.", permissionNode = "create")
    public void create(Player player) {
        ArenaEditorWizards.MAP_CREATION.openWizard(player, this.arena);
    }

    @ArenaCommand(commands = "edit", description = "Edit an arena map.", permissionNode = "edit")
    public void map(Player player, Competition<?> competition, MapOption option) {
        if (!(competition instanceof LiveCompetition<?> liveCompetition)) {
            Messages.NO_ARENA_WITH_NAME.send(player);
            return;
        }

        LiveCompetitionMap<?> map = liveCompetition.getMap();

        WizardStage<MapCreateContext> stage = ArenaEditorWizards.MAP_CREATION.getStage(option);
        ArenaEditorWizards.MAP_CREATION.openSingleWizardStage(player, this.arena, stage, context -> context.reconstructFrom(map));
    }

    @Override
    protected Object onVerifyArgument(CommandSender sender, String arg, Class<?> parameter) {
        switch (parameter.getSimpleName().toLowerCase()) {
            case "competition" -> {
                List<Competition<?>> openCompetitions = BattleArena.getInstance().getCompetitions(this.arena, arg);
                if (openCompetitions.isEmpty()) {
                    return null;
                }

                return openCompetitions.get(0);
            }
            case "competitionmap" -> {
                return BattleArena.getInstance().getMap(this.arena, arg);
            }
        }

        return super.onVerifyArgument(sender, arg, parameter);
    }

    @Override
    protected boolean onInvalidArgument(CommandSender sender, Class<?> parameter, String input) {
        switch (parameter.getSimpleName().toLowerCase()) {
            case "competition", "competitionmap" -> {
                Messages.NO_ARENA_WITH_NAME.send(sender);
                return true;
            }
        }

        return super.onInvalidArgument(sender, parameter, input);
    }

    @Override
    protected List<String> onVerifyTabComplete(String arg, Class<?> parameter) {
        if (parameter.getSimpleName().equalsIgnoreCase("competition")) {
            return BattleArena.getInstance().getCompetitions(this.arena)
                    .stream()
                    .map(competition -> competition.getMap().getName())
                    .toList();
        }

        return super.onVerifyTabComplete(arg, parameter);
    }
}