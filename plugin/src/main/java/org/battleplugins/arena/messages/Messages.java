package org.battleplugins.arena.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.StyleBuilderApplicable;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class Messages {
    public static final TextColor PRIMARY_COLOR = NamedTextColor.YELLOW;
    public static final TextColor SECONDARY_COLOR = NamedTextColor.GOLD;
    public static final TextColor ERROR_COLOR = NamedTextColor.RED;
    public static final TextColor SUCCESS_COLOR = NamedTextColor.GREEN;

    static final TagResolver RESOLVER = TagResolver.builder()
            .tag("primary", Tag.styling(PRIMARY_COLOR))
            .tag("secondary", Tag.styling(SECONDARY_COLOR))
            .tag("error", Tag.styling(ERROR_COLOR))
            .tag("success", Tag.styling(SUCCESS_COLOR))
            .build();

    static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Misc
    public static final Message HEADER = Message.of("header",
            Component.text("------------------").color(NamedTextColor.GRAY)
                    .append(Component.text("[").color(SECONDARY_COLOR))
                    .append(Component.text(" "))
                    .append(Component.text("{}").color(PRIMARY_COLOR).decorate(TextDecoration.BOLD))
                    .append(Component.text(" "))
                    .append(Component.text("]").color(SECONDARY_COLOR))
                    .append(Component.text("------------------")).color(NamedTextColor.GRAY)
    );

    // Command messages
    public static final Message MUST_BE_PLAYER = error("command-must-be-player", "You must be a player to use this command!");
    public static final Message COMMAND_USAGE = error("command-usage", "Invalid syntax! Usage: {}");
    public static final Message ARENA_DOES_NOT_EXIST = error("command-arena-does-not-exist", "An arena by the name of {} does not exist!");
    public static final Message NO_PERMISSION = error("command-no-permission", "You do not have permission to execute this command!");
    public static final Message UNKNOWN_ERROR = error("command-unknown-error", "An unknown error occurred while executing this command! Please contact a server administrator!");
    public static final Message AN_ERROR_OCCURRED = error("command-an-error-occurred", "An error occurred while executing this command: {}!");
    public static final Message PLAYER_NOT_ONLINE = error("command-player-not-online", "The player <secondary>{}</secondary> is not online!");

    // Arena messages
    public static final Message NO_OPEN_ARENAS = error("arena-no-open-arenas", "There are no open arenas!");
    public static final Message NO_ARENA_WITH_NAME = error("arena-arena-with-name", "There is no arena with that name!");
    public static final Message ARENA_FULL = error("arena-full", "This arena is full!");
    public static final Message ARENA_NOT_JOINABLE = error("arena-not-joinable", "This arena is not joinable!");
    public static final Message ARENA_NOT_SPECTATABLE = error("arena-not-spectatable", "This arena is not able to be spectated!");
    public static final Message ARENA_ERROR = error("arena-error", "An error occurred while joining the arena: {}!");
    public static final Message ALREADY_IN_ARENA = error("arena-already-in-arena", "You are already in an arena!");
    public static final Message NOT_IN_ARENA = error("arena-not-in-arena", "You are not in an arena!");
    public static final Message ARENA_JOINED = info("arena-joined", "You have joined <secondary>{}</secondary>!");
    public static final Message ARENA_SPECTATE = info("arena-spectate", "You are now spectating <secondary>{}</secondary>!");
    public static final Message ARENA_LEFT = info("arena-left", "You have left <secondary>{}</secondary>!");

    public static final Message ARENA_STARTS_IN = info("arena-starts-in", "{} will start in <secondary>{}</secondary>!");
    public static final Message ARENA_START_CANCELLED = error("arena-starts-cancelled", "Countdown cancelled as there is not enough players to start!");

    public static final Message FIGHT = message("arena-fight", "<dark_red> O---[{==========> <yellow>Fight</yellow> <==========}]---O </dark_red>");

    // Editor wizard messages
    public static final Message ERROR_OCCURRED_APPLYING_CHANGES = error("editor-error-occurred-applying-changes", "An error occurred while applying changes. Please see the console for more information!");
    public static final Message ERROR_ALREADY_IN_EDITOR = error("editor-error-already-in-editor", "You are already in an editor!");

    public static final Message MAP_CREATE_NAME = info("editor-map-create-name", "Enter a name for the map! Type \"cancel\" to cancel.");
    public static final Message MAP_EXISTS = error("editor-map-exists", "A map by that name already exists!");

    public static final Message MAP_SET_TYPE = info("editor-map-set-type", """
                Enter the type of map. Valid options: <secondary>static, dynamic</secondary>.
    
                <gray>-</gray> <secondary>static</secondary>: A map that is always in the same location (i.e. a survival world) and can only support one game at a time.
                <gray>-</gray> <secondary>dynamic</secondary>: A map that is freshly created in a separate world on-demand. This map can support multiple games at once. Requires WorldEdit!
                """
    );
    public static final Message MAP_SET_MIN_POSITION = info("editor-map-set-min-position", "Click a block to set the minimum (first) position of the map region.");
    public static final Message MAP_SET_MAX_POSITION = info("editor-map-set-max-position", "Click a block to set the maximum (second) position of the map region.");
    public static final Message MAP_SET_WAITROOM_SPAWN = info("editor-map-set-waitroom-spawn", "Type \"waitroom\" to set the waitroom spawn, or \"cancel\" to cancel.");
    public static final Message MAP_SET_SPECTATOR_SPAWN = info("editor-map-set-spectator-spawn", "Type \"spectator\" to set the spectator spawn, or \"cancel\" to cancel.");
    public static final Message MAP_ADD_TEAM_SPAWN = info("editor-map-add-team-spawn", "Type \"spawn\" add a spawn for a team. Once you are finished adding team spawns, type \"done\". " +
            "If you wish to clear all spawns for a certain team and start over, type \"clear\".");
    public static final Message MAP_ADD_TEAM_SPAWN_TEAM = info("editor-map-add-team-spawn-team", "Enter the name of the team you would like to add a spawn for. Type \"cancel\" to cancel.");
    public static final Message MAP_CLEAR_TEAM_SPAWN_TEAM = info("editor-map-clear-team-spawn-team", "Enter the name of the team you would like to clear all spawns for. Type \"cancel\" to cancel.");
    public static final Message MAP_ADD_TEAM_SPAWN_ADDED = success("editor-map-add-team-spawn-added", "Added a spawn for team <secondary>{}</secondary>!");
    public static final Message MAP_MISSING_TEAM_SPAWNS = error("editor-map-missing-team-spawns", "You must add at least one spawn for each team! Teams with no spawns: <secondary>{}</secondary>");
    public static final Message MAP_ADD_TEAM_SPAWN_CLEARED = success("editor-map-add-team-spawn-cleared", "Cleared all spawns for team <secondary>{}</secondary>!");
    public static final Message MAP_CREATED = success("editor-map-created", "Successfully created map <secondary>{}</secondary> for {}!");
    public static final Message MAP_EDITED = success("editor-map-edited", "Successfully edited map <secondary>{}</secondary>!");

    // Util strings
    public static final Message SECONDS = message("util-seconds", "seconds");
    public static final Message SECOND = message("util-second", "second");
    public static final Message MINUTES = message("util-minutes", "minutes");
    public static final Message MINUTE = message("util-minute", "minute");
    public static final Message HOURS = message("util-hours", "hours");
    public static final Message HOUR = message("util-hour", "hour");
    public static final Message ENABLED = message("util-enabled", "enabled", NamedTextColor.GREEN);
    public static final Message DISABLED = message("util-disabled", "disabled", NamedTextColor.RED);

    public static final Message CLICK_TO_SELECT = info("util-click-to-select", "Click to select!");
    public static final Message INVALID_INPUT = error("util-invalid-input", "Invalid input! Valid options: <secondary>{}</secondary>");
    public static final Message INVALID_INVENTORY_CANCELLING = error("util-invalid-inventory-cancelling", "Interacted with inventory that was not own... cancelling item selection!");
    public static final Message VALID_TEAMS = info("util-valid-teams", "Valid teams: <secondary>{}</secondary>");
    public static final Message INVALID_TEAM_VALID_TEAMS = error("util-invalid-team-valid-teams", "Invalid team! Valid teams: <secondary>{}</secondary>.");
    public static final Message INVENTORY_BACKUPS = message("util-inventory-backups", "Backups");
    public static final Message NO_BACKUPS = error("util-no-backups", "No backups found for player <secondary>{}</secondary>!");
    public static final Message BACKUP_NOT_FOUND = error("util-backup-not-found", "A backup at this index could not be found!");
    public static final Message BACKUP_RESTORED = success("util-backup-restored", "Successfully restored backup for player <secondary>{}</secondary>!");
    public static final Message BACKUP_CREATED = success("util-backup-created", "Successfully created backup for player <secondary>{}</secondary>!");
    public static final Message BACKUP_NUMBER = message("util-backup-number", "Backup <secondary>#{}</secondary>");
    public static final Message MODULES = message("util-modules", "Modules");
    public static final Message MODULE = message("util-module", "<gray>-</gray> <secondary>{}:</secondary> {}");

    static void init() {
        // no-op
    }

    public static Message wrap(String defaultText) {
        return new Message("unregistered", MINI_MESSAGE.deserialize(defaultText, RESOLVER));
    }

    public static Message info(String translationKey, String defaultText) {
        return message(translationKey, MINI_MESSAGE.deserialize(defaultText, RESOLVER).color(PRIMARY_COLOR));
    }

    public static Message error(String translationKey, String defaultText) {
        return message(translationKey, MINI_MESSAGE.deserialize(defaultText, RESOLVER).color(ERROR_COLOR));
    }

    public static Message success(String translationKey, String defaultText) {
        return message(translationKey, MINI_MESSAGE.deserialize(defaultText, RESOLVER).color(SUCCESS_COLOR));
    }

    public static Message message(String translationKey, String text) {
        return message(translationKey, MINI_MESSAGE.deserialize(text, RESOLVER));
    }

    public static Message message(String translationKey, String text, StyleBuilderApplicable... styles) {
        return message(translationKey, Component.text(text, Style.style(styles)));
    }

    public static Message message(String translationKey, String text, Style style) {
        return message(translationKey, Component.text(text, style));
    }

    public static Message message(String translationKey, Component text) {
        return MessageLoader.register(Message.of(translationKey, text));
    }
}