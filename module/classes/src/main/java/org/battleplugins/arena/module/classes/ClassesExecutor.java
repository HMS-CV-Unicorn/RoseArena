package org.battleplugins.arena.module.classes;

import org.battleplugins.arena.Arena;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.command.ArenaCommand;
import org.battleplugins.arena.command.BaseSubCommandExecutor;
import org.battleplugins.arena.messages.Messages;
import org.battleplugins.arena.options.types.BooleanArenaOption;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ClassesExecutor implements BaseSubCommandExecutor {
    private final Classes module;
    private final Arena arena;

    public ClassesExecutor(Classes module, Arena arena) {
        this.module = module;
        this.arena = arena;
    }

    @ArenaCommand(commands = "equip", description = "Equip a class.", permissionNode = "equip")
    public void equip(Player player, ArenaClass arenaClass) {
        // Should not get here, but just as an additional safeguard
        // *just in case*
        if (!this.arena.isModuleEnabled(Classes.ID)) {
            ClassMessages.CLASSES_NOT_ENABLED.send(player);
            return;
        }

        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            Messages.NOT_IN_ARENA.send(player);
            return;
        }

        // Ensure that the current competition phase allows class equipping
        boolean canEquip = arenaPlayer.getCompetition().option(Classes.CLASS_EQUIPPING_OPTION)
                .map(BooleanArenaOption::isEnabled)
                .orElse(false);
        if (canEquip) {
            boolean equipOnlySelects = arenaPlayer.getCompetition().option(Classes.CLASS_EQUIP_ONLY_SELECTS)
                    .map(BooleanArenaOption::isEnabled)
                    .orElse(false);

            // Check if equip only selects, and if so, set the player's class
            if (equipOnlySelects) {
                arenaPlayer.setMetadata(ArenaClass.class, arenaClass);
            } else {
                arenaClass.equip(arenaPlayer);
            }

            ClassMessages.CLASS_EQUIPPED.send(player, arenaClass.getName());
        } else {
            ClassMessages.CANNOT_EQUIP_CLASS.send(player);
        }
    }

    @Override
    public Object onVerifyArgument(CommandSender sender, String arg, Class<?> parameter) {
        if (parameter.getSimpleName().equalsIgnoreCase("arenaclass")) {
            return this.module.getClass(arg);
        }

        return null;
    }

    @Override
    public List<String> onVerifyTabComplete(String arg, Class<?> parameter) {
        if (parameter.getSimpleName().equalsIgnoreCase("arenaclass")) {
            return List.copyOf(this.module.getClasses().keySet());
        }

        return List.of();
    }

    @Override
    public String getUsageString(Class<?> parameter) {
        if (parameter.getSimpleName().equalsIgnoreCase("arenaclass")) {
            return "<class> ";
        }

        return null;
    }
}