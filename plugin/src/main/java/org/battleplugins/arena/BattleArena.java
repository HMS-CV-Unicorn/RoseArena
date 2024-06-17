package org.battleplugins.arena;

import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.command.BACommandExecutor;
import org.battleplugins.arena.competition.Competition;
import org.battleplugins.arena.competition.CompetitionType;
import org.battleplugins.arena.competition.JoinResult;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.PlayerRole;
import org.battleplugins.arena.competition.map.LiveCompetitionMap;
import org.battleplugins.arena.competition.map.MapType;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.battleplugins.arena.competition.phase.phases.VictoryPhase;
import org.battleplugins.arena.config.ArenaConfigParser;
import org.battleplugins.arena.config.BattleArenaConfig;
import org.battleplugins.arena.config.ParseException;
import org.battleplugins.arena.event.BattleArenaPostInitializeEvent;
import org.battleplugins.arena.event.BattleArenaPreInitializeEvent;
import org.battleplugins.arena.event.BattleArenaShutdownEvent;
import org.battleplugins.arena.event.arena.ArenaCreateExecutorEvent;
import org.battleplugins.arena.event.arena.ArenaInitializeEvent;
import org.battleplugins.arena.messages.MessageLoader;
import org.battleplugins.arena.module.ArenaModuleContainer;
import org.battleplugins.arena.module.ArenaModuleLoader;
import org.battleplugins.arena.module.ModuleLoadException;
import org.battleplugins.arena.team.ArenaTeams;
import org.battleplugins.arena.util.CommandInjector;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The main class for BattleArena.
 */
public class BattleArena extends JavaPlugin implements Listener {
    private static BattleArena instance;

    private final Map<String, Class<? extends Arena>> arenaTypes = new HashMap<>();
    private final Map<String, Arena> arenas = new HashMap<>();

    private final Map<Arena, List<LiveCompetitionMap<?>>> arenaMaps = new HashMap<>();
    private final Map<Arena, List<Competition<?>>> competitions = new HashMap<>();

    private final Map<String, ArenaLoader> arenaLoaders = new HashMap<>();

    private BattleArenaConfig config;
    private ArenaModuleLoader moduleLoader;
    private ArenaTeams teams;

    private Path arenasPath;
    private Path modulesPath;

    @Override
    public void onLoad() {
        instance = this;

        Path dataFolder = this.getDataFolder().toPath();
        this.arenasPath = dataFolder.resolve("arenas");
        this.modulesPath = dataFolder.resolve("modules");

        this.moduleLoader = new ArenaModuleLoader(this, this.modulesPath);
        try {
            this.moduleLoader.loadModules();
        } catch (IOException e) {
            this.error("An error occurred loading modules!", e);
        }

        new BattleArenaPreInitializeEvent(this).callEvent();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        // Copy our default configs
        this.saveDefaultConfig();

        Configuration config = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "config.yml"));
        this.config = ArenaConfigParser.newInstance(BattleArenaConfig.class, config);

        if (Files.notExists(this.arenasPath)) {
            this.saveResource("arenas/arena.yml", false);
        }

        Path dataFolder = this.getDataFolder().toPath();
        if (Files.notExists(dataFolder.resolve("messages.yml"))) {
            this.saveResource("messages.yml", false);
        }

        if (Files.notExists(dataFolder.resolve("teams.yml"))) {
            this.saveResource("teams.yml", false);
        }

        // Load teams
        Configuration teamsConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "teams.yml"));
        this.teams = ArenaConfigParser.newInstance(ArenaTeams.class, teamsConfig, this);

        // Clear any remaining dynamic maps
        this.clearDynamicMaps();

        // Enable modules
        this.moduleLoader.enableModules();

        // Load messages
        MessageLoader.load(dataFolder.resolve("messages.yml"));

        // Register default arenas
        this.registerArena("Arena", Arena.class);

        // Create arena loaders
        try (Stream<Path> arenaPaths = Files.walk(this.arenasPath)) {
            arenaPaths.forEach(arenaPath -> {
                try {
                    if (Files.isDirectory(arenaPath)) {
                        return;
                    }

                    Configuration configuration = YamlConfiguration.loadConfiguration(Files.newBufferedReader(arenaPath));
                    String name = configuration.getString("name");
                    if (name == null) {
                        this.info("Arena {} does not have a name!", arenaPath.getFileName());
                        return;
                    }

                    String mode = configuration.getString("mode", name);
                    ArenaLoader arenaLoader = new ArenaLoader(mode, configuration, arenaPath);
                    this.arenaLoaders.put(name, arenaLoader);

                    // Because Bukkit locks its command map upon startup, we need to
                    // add our plugin commands here, but populating the executor
                    // can happen at any time. This also means that Arenas can specify
                    // their own executors if they so please.
                    CommandInjector.inject(name, name.toLowerCase(Locale.ROOT));
                } catch (IOException e) {
                    throw new RuntimeException("Error reading arena config", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error walking arenas path!", e);
        }

        // Register base command
        PluginCommand command = this.getCommand("battlearena");
        if (command == null) {
            throw new IllegalArgumentException("Failed to register command 'battlearena'. Was it not registered?");
        }

        command.setExecutor(new BACommandExecutor("battlearena"));
    }

    @Override
    public void onDisable() {
        new BattleArenaShutdownEvent(this).callEvent();

        // Close all active competitions
        this.completeAllActiveCompetitions();

        // Clear dynamic maps
        this.clearDynamicMaps();
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // There is logic called later, however by this point all plugins
        // using the BattleArena API should have been loaded. As modules will
        // listen for this event to register their behavior, we need to ensure
        // they are fully initialized so any references to said modules in
        // arena config files will be valid.
        new BattleArenaPostInitializeEvent(this).callEvent();

        // Load all arenas
        this.loadArenas();

        // Load the arena maps
        this.loadArenaMaps();

        // Initialize matches
        for (Map.Entry<Arena, List<LiveCompetitionMap<?>>> entry : this.arenaMaps.entrySet()) {
            if (entry.getKey().getType() != CompetitionType.MATCH) {
                continue;
            }

            for (LiveCompetitionMap<?> map : entry.getValue()) {
                if (map.getType() == MapType.STATIC) {
                    Competition<?> competition = map.createCompetition(entry.getKey());
                    this.addCompetition(entry.getKey(), competition);

                    // TODO: Call event in Arena.java
                }
            }
        }
    }

    public <T extends Arena> void registerArena(String name, Class<T> arena) {
        this.registerArena(name, arena, () -> {
            try {
                return arena.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate arena " + arena.getName(), e);
            }
        });
    }

    public <T extends Arena> void registerArena(String name, Class<T> arenaClass, Supplier<T> arenaFactory) {
        ArenaConfigParser.registerFactory(arenaClass, arenaFactory);

        this.arenaTypes.put(name, arenaClass);
    }

    public void addArenaMap(Arena arena, LiveCompetitionMap<?> map) {
        this.arenaMaps.computeIfAbsent(arena, k -> new ArrayList<>()).add(map);
    }

    public void removeArenaMap(Arena arena, LiveCompetitionMap<?> map) {
        this.arenaMaps.computeIfAbsent(arena, k -> new ArrayList<>()).remove(map);

        // If the map is removed, also remove the competition if applicable
        this.competitions.computeIfAbsent(arena, k -> new ArrayList<>()).removeIf(competition -> competition.getMap() == map);
    }

    public void addCompetition(Arena arena, Competition<?> competition) {
        this.competitions.computeIfAbsent(arena, k -> new ArrayList<>()).add(competition);
    }

    public Path getMapsPath() {
        return this.getDataFolder().toPath().resolve("maps");
    }

    @SuppressWarnings("unchecked")
    private void completeAllActiveCompetitions() {
        for (Map.Entry<Arena, List<Competition<?>>> competitions : this.competitions.entrySet()) {
            Set<CompetitionPhaseType<?, ?>> phases = competitions.getKey().getPhases();

            // Check if we have a victory phase
            CompetitionPhaseType<?, VictoryPhase<?>> victoryPhase = null;
            for (CompetitionPhaseType<?, ?> phase : phases) {
                if (VictoryPhase.class.isAssignableFrom(phase.getPhaseType())) {
                    victoryPhase = (CompetitionPhaseType<?, VictoryPhase<?>>) phase;
                    break;
                }
            }

            for (Competition<?> competition : competitions.getValue()) {
                // We can only complete live competitions
                if (competition instanceof LiveCompetition<?> liveCompetition) {
                    if (victoryPhase != null) {
                        liveCompetition.getPhaseManager().setPhase(victoryPhase);

                        VictoryPhase<?> phase = (VictoryPhase<?>) liveCompetition.getPhaseManager().getCurrentPhase();
                        phase.onDraw(); // Mark as a draw

                        // Dereference any remaining resources
                        liveCompetition.getVictoryManager().end();
                    } else {
                        // No victory phase - just forcefully kick every player
                        for (ArenaPlayer player : liveCompetition.getPlayers()) {
                            liveCompetition.leave(player);
                        }
                    }
                }
            }
        }
    }

    private void loadArenas() {
        // Register our arenas once ALL the plugins have loaded. This ensures that
        // all custom plugins adding their own arena types have been loaded.
        for (ArenaLoader value : this.arenaLoaders.values()) {
            try {
                value.load();
            } catch (Exception e) {
                this.error("An error occurred when loading arena {}: {}", value.arenaPath.getFileName(), e.getMessage(), e);
            }
        }
    }

    private void loadArenaMaps() {
        // All the arenas have been loaded, now we can load the competitions
        Path mapsPath = this.getMapsPath();
        if (Files.notExists(mapsPath)) {
            // No maps to load
            return;
        }

        // Check to see if there are any maps to load
        for (Map.Entry<String, Arena> entry : this.arenas.entrySet()) {
            String arenaName = entry.getKey();
            Arena arena = entry.getValue();

            Path arenaMapPath = mapsPath.resolve(arenaName.toLowerCase(Locale.ROOT));
            if (Files.notExists(arenaMapPath)) {
                continue;
            }

            try (Stream<Path> mapPaths = Files.walk(arenaMapPath)) {
                mapPaths.forEach(mapPath -> {
                    if (Files.isDirectory(mapPath)) {
                        return;
                    }

                    try {
                        Configuration configuration = YamlConfiguration.loadConfiguration(Files.newBufferedReader(mapPath));
                        LiveCompetitionMap<?> map = ArenaConfigParser.newInstance(arena.getCompetitionMapType(), configuration, this);
                        if (map.getBounds() == null && map.getType() == MapType.DYNAMIC) {
                            // Cannot create dynamic map without bounds
                            this.warn("Map {} for arena {} is dynamic but does not have bounds!", map.getName(), arena.getName());
                            return;
                        }

                        this.addArenaMap(arena, map);
                        this.info("Loaded map {} for arena {}.", map.getName(), arena.getName());
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading competition config", e);
                    } catch (ParseException e) {
                        this.error("An error occurred when loading competition for arena {}: {}", arenaName, e.getMessage(), e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Error loading maps for arena " + arenaName, e);
            }
        }
    }

    @Nullable
    public Arena getArena(String name) {
        return this.arenas.get(name);
    }

    public List<LiveCompetitionMap<?>> getMaps(Arena arena) {
        List<LiveCompetitionMap<?>> maps = this.arenaMaps.get(arena);
        if (maps == null) {
            return List.of();
        }

        return maps;
    }

    @Nullable
    public LiveCompetitionMap<?> getMap(Arena arena, String name) {
        List<LiveCompetitionMap<?>> maps = this.arenaMaps.get(arena);
        if (maps == null) {
            return null;
        }

        return maps.stream()
                .filter(map -> map.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<Competition<?>> getCompetitions(Arena arena) {
        return this.competitions.getOrDefault(arena, List.of());
    }

    public List<Competition<?>> getCompetitions(Arena arena, String name) {
        List<Competition<?>> competitions = BattleArena.getInstance().getCompetitions(arena);
        return competitions.stream()
                .filter(competition -> competition.getMap().getName().equals(name))
                .toList();
    }

    public CompletableFuture<Competition<?>> getOrCreateCompetition(Arena arena, Player player, PlayerRole role, @Nullable String name) {
        // See if we can join any already open competitions
        List<Competition<?>> openCompetitions = this.getCompetitions(arena, name);
        CompletableFuture<Competition<?>> joinableCompetition = this.findJoinableCompetition(openCompetitions, player, role);
        return joinableCompetition.thenApplyAsync(comp -> {
            if (comp != null) {
                return comp;
            }

            List<LiveCompetitionMap<?>> maps = this.arenaMaps.get(arena);
            if (maps == null) {
                // No maps, return
                return null;
            }

            // Ensure we have WorldEdit installed
            if (this.getServer().getPluginManager().getPlugin("WorldEdit") == null) {
                this.error("WorldEdit is required to create dynamic competitions! Not proceeding with creating a new dynamic competition.");
                return null;
            }

            // Check if we have exceeded the maximum number of dynamic maps
            List<Competition<?>> allCompetitions = this.getCompetitions(arena);
            long dynamicMaps = allCompetitions.stream()
                    .map(Competition::getMap)
                    .filter(map -> map.getType() == MapType.DYNAMIC)
                    .count();

            if (dynamicMaps >= this.config.getMaxDynamicMaps() && this.config.getMaxDynamicMaps() != -1) {
                this.warn("Exceeded maximum number of dynamic maps for arena {}! Not proceeding with creating a new dynamic competition.", arena.getName());
                return null;
            }

            // Create a new competition if possible

            if (name == null) {
                // Shuffle results if map name is not requested
                maps = new ArrayList<>(maps);
                Collections.shuffle(maps);
            }

            for (LiveCompetitionMap<?> map : maps) {
                if (map.getType() != MapType.DYNAMIC) {
                    continue;
                }

                if ((name == null || map.getName().equals(name))) {
                    Competition<?> competition = map.createDynamicCompetition(arena);
                    if (competition == null) {
                        this.warn("Failed to create dynamic competition for map {} in arena {}!", map.getName(), arena.getName());
                        continue;
                    }

                    this.addCompetition(arena, competition);

                    // TODO: Call event in Arena.java
                    return competition;
                }
            }

            // No open competitions found or unable to create a new one
            return null;
        }, Bukkit.getScheduler().getMainThreadExecutor(this));
    }

    public CompletableFuture<Competition<?>> findJoinableCompetition(List<Competition<?>> competitions, Player player, PlayerRole role) {
        if (competitions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Competition<?> competition = competitions.get(0);
        CompletableFuture<JoinResult> result = competition.canJoin(player, role);
        if (result.join() == JoinResult.SUCCESS) {
            return CompletableFuture.completedFuture(competition);
        } else {
            List<Competition<?>> remainingCompetitions = new ArrayList<>(competitions);
            remainingCompetitions.remove(competition);

            return this.findJoinableCompetition(remainingCompetitions, player, role);
        }
    }

    public BattleArenaConfig getMainConfig() {
        return this.config;
    }

    public ArenaTeams getTeams() {
        return this.teams;
    }

    @Nullable
    public <T> ArenaModuleContainer<T> getModule(String id) {
        return this.moduleLoader.getModule(id);
    }

    public <T> Optional<ArenaModuleContainer<T>> module(String id) {
        return Optional.ofNullable(this.getModule(id));
    }

    public List<ArenaModuleContainer<?>> getModules() {
        return this.moduleLoader.getModules();
    }

    public Set<ModuleLoadException> getFailedModules() {
        return this.moduleLoader.getFailedModules();
    }

    public Path getBackupPath(String type) {
        return this.getDataFolder().toPath().resolve("backups").resolve(type);
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        return sender.isOp() || sender.hasPermission(permission);
    }

    public ClassLoader getPluginClassLoader() {
        return super.getClassLoader();
    }

    public void info(String message, Object... args) {
        this.getSLF4JLogger().info(message, args);
    }

    public void error(String message, Object... args) {
        this.getSLF4JLogger().error(message, args);
    }

    public void warn(String message, Object... args) {
        this.getSLF4JLogger().warn(message, args);
    }

    private void clearDynamicMaps() {
        for (File file : Bukkit.getWorldContainer().listFiles()) {
            if (file.isDirectory() && file.getName().startsWith("ba-dynamic")) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    this.error("Failed to delete dynamic map {}", file.getName(), e);
                }
            }
        }
    }

    public static BattleArena getInstance() {
        return instance;
    }

    class ArenaLoader {
        private final String mode;
        private final Configuration configuration;
        private final Path arenaPath;

        public ArenaLoader(String mode, Configuration configuration, Path arenaPath) {
            this.mode = mode;
            this.configuration = configuration;
            this.arenaPath = arenaPath;
        }

        public void load() {
            if (Files.isDirectory(arenaPath)) {
                return;
            }

            try {
                Class<? extends Arena> arenaType = arenaTypes.get(mode);
                if (arenaType == null) {
                    info("Arena {} does not have a valid mode! Recognized modes: {}", arenaPath.getFileName(), arenaTypes.keySet());
                    return;
                }

                Arena arena = ArenaConfigParser.newInstance(arenaType, configuration, BattleArena.this);
                Bukkit.getPluginManager().registerEvents(arena, BattleArena.this);

                arenas.put(arena.getName(), arena);

                // Register command
                PluginCommand command = getCommand(arena.getName().toUpperCase(Locale.ROOT));

                ArenaCommandExecutor executor = arena.createCommandExecutor();
                ArenaCreateExecutorEvent event = new ArenaCreateExecutorEvent(arena, executor);
                event.callEvent();

                command.setExecutor(executor);

                new ArenaInitializeEvent(arena).callEvent();

                info("Loaded arena: {}.", arena.getName());
            } catch (ParseException e) {
                error("An error occurred when loading arena {}: {}", arenaPath.getFileName(), e.getMessage(), e);
            }
        }
    }
}