package de.diddiz.LogBlock.config;

import de.diddiz.LogBlock.*;
import de.diddiz.util.ComparableVersion;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.parseTimeSpec;
import static org.bukkit.Bukkit.*;

public class Config {
    private static LoggingEnabledMapping superWorldConfig;
    private static Map<String, WorldConfig> worldConfigs;
    public static String url, user, password;
    public static boolean mysqlUseSSL;
    public static boolean mysqlRequireSSL;
    public static int delayBetweenRuns, forceToProcessAtLeast, timePerRun;
    public static boolean fireCustomEvents;
    public static boolean useBukkitScheduler;
    public static int queueWarningSize;
    public static boolean enableAutoClearLog;
    public static List<String> autoClearLog;
    public static int autoClearLogDelay;
    public static boolean dumpDeletedLog;
    public static boolean logBedExplosionsAsPlayerWhoTriggeredThese;
    public static boolean logCreeperExplosionsAsPlayerWhoTriggeredThese, logPlayerInfo;
    public static boolean logFireSpreadAsPlayerWhoCreatedIt;
    public static boolean logFluidFlowAsPlayerWhoTriggeredIt;
    public static LogKillsLevel logKillsLevel;
    public static Set<Material> dontRollback, replaceAnyway;
    public static int rollbackMaxTime, rollbackMaxArea;
    public static Map<String, Tool> toolsByName;
    public static Map<Material, Tool> toolsByType;
    public static int defaultDist, defaultTime;
    public static int linesPerPage, linesLimit, hardLinesLimit;
    public static boolean askRollbacks, askRedos, askClearLogs, askClearLogAfterRollback, askRollbackAfterBan;
    public static String banPermission;
    public static Set<Material> hiddenBlocks;
    public static Set<String> hiddenPlayers;
    public static List<String> ignoredChat;
    public static SimpleDateFormat formatter;
    public static boolean safetyIdCheck;
    public static boolean debug;
    public static boolean logEnvironmentalKills;
    // Not loaded from config - checked at runtime
    public static boolean mb4 = false;

    public static enum LogKillsLevel {
        PLAYERS,
        MONSTERS,
        ANIMALS;
    }

    public static void load(LogBlock logblock) throws DataFormatException, IOException {
        final ConfigurationSection config = logblock.getConfig();
        final Map<String, Object> def = new HashMap<>();
        def.put("version", logblock.getDescription().getVersion());
        final List<String> worldNames = new ArrayList<>();
        for (final World world : getWorlds()) {
            worldNames.add(world.getName());
        }
        if (worldNames.isEmpty()) {
            worldNames.add("world");
            worldNames.add("world_nether");
            worldNames.add("world_the_end");
        }
        def.put("loggedWorlds", worldNames);
        def.put("mysql.host", "localhost");
        def.put("mysql.port", 3306);
        def.put("mysql.database", "minecraft");
        def.put("mysql.user", "username");
        def.put("mysql.password", "pass");
        def.put("mysql.useSSL", true);
        def.put("mysql.requireSSL", false);
        def.put("consumer.delayBetweenRuns", 2);
        def.put("consumer.forceToProcessAtLeast", 200);
        def.put("consumer.timePerRun", 1000);
        def.put("consumer.fireCustomEvents", false);
        def.put("consumer.useBukkitScheduler", true);
        def.put("consumer.queueWarningSize", 1000);
        def.put("clearlog.dumpDeletedLog", false);
        def.put("clearlog.enableAutoClearLog", false);
        final List<String> autoClearlog = new ArrayList<>();
        for (final String world : worldNames) {
            autoClearlog.add("world \"" + world + "\" before 365 days all");
            autoClearlog.add("world \"" + world + "\" player lavaflow waterflow leavesdecay before 7 days all");
            autoClearlog.add("world \"" + world + "\" entities before 365 days");
        }
        def.put("clearlog.auto", autoClearlog);
        def.put("clearlog.autoClearLogDelay", "6h");
        def.put("logging.logBedExplosionsAsPlayerWhoTriggeredThese", true);
        def.put("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
        def.put("logging.logFireSpreadAsPlayerWhoCreatedIt", true);
        def.put("logging.logFluidFlowAsPlayerWhoTriggeredIt", false);
        def.put("logging.logKillsLevel", "PLAYERS");
        def.put("logging.logEnvironmentalKills", false);
        def.put("logging.logPlayerInfo", false);
        def.put("logging.hiddenPlayers", new ArrayList<String>());
        def.put("logging.hiddenBlocks", Arrays.asList(Material.AIR.name(), Material.CAVE_AIR.name(), Material.VOID_AIR.name()));
        def.put("logging.ignoredChat", Arrays.asList("/register", "/login"));
        def.put("rollback.dontRollback", Arrays.asList(Material.LAVA.name(), Material.TNT.name(), Material.FIRE.name()));
        def.put("rollback.replaceAnyway", Arrays.asList(Material.LAVA.name(), Material.WATER.name(), Material.FIRE.name(), Material.GRASS_BLOCK.name()));
        def.put("rollback.maxTime", "2 days");
        def.put("rollback.maxArea", 50);
        def.put("lookup.defaultDist", 20);
        def.put("lookup.defaultTime", "30 minutes");
        def.put("lookup.linesPerPage", 15);
        def.put("lookup.linesLimit", 1500);
        def.put("lookup.hardLinesLimit", 100000);
        try {
            formatter = new SimpleDateFormat(config.getString("lookup.dateFormat", "MM-dd HH:mm:ss"));
        } catch (IllegalArgumentException e) {
            throw new DataFormatException("Invalid specification for  date format, please see http://docs.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html : " + e.getMessage());
        }
        def.put("lookup.dateFormat", "MM-dd HH:mm:ss");
        def.put("questioner.askRollbacks", true);
        def.put("questioner.askRedos", true);
        def.put("questioner.askClearLogs", true);
        def.put("questioner.askClearLogAfterRollback", true);
        def.put("questioner.askRollbackAfterBan", false);
        def.put("questioner.banPermission", "mcbans.ban.local");
        def.put("tools.tool.aliases", Arrays.asList("t"));
        def.put("tools.tool.leftClickBehavior", "NONE");
        def.put("tools.tool.rightClickBehavior", "TOOL");
        def.put("tools.tool.defaultEnabled", true);
        def.put("tools.tool.item", Material.WOODEN_PICKAXE.name());
        def.put("tools.tool.canDrop", true);
        def.put("tools.tool.removeOnDisable", true);
        def.put("tools.tool.dropToDisable", false);
        def.put("tools.tool.params", "area 0 all sum none limit 15 desc since 60d silent");
        def.put("tools.tool.mode", "LOOKUP");
        def.put("tools.tool.permissionDefault", "OP");
        def.put("tools.toolblock.aliases", Arrays.asList("tb"));
        def.put("tools.toolblock.leftClickBehavior", "TOOL");
        def.put("tools.toolblock.rightClickBehavior", "BLOCK");
        def.put("tools.toolblock.defaultEnabled", true);
        def.put("tools.toolblock.item", Material.BEDROCK.name());
        def.put("tools.toolblock.canDrop", false);
        def.put("tools.toolblock.removeOnDisable", true);
        def.put("tools.toolblock.dropToDisable", false);
        def.put("tools.toolblock.params", "area 0 all sum none limit 15 desc since 60d silent");
        def.put("tools.toolblock.mode", "LOOKUP");
        def.put("tools.toolblock.permissionDefault", "OP");
        def.put("safety.id.check", true);
        def.put("debug", false);
        for (final Entry<String, Object> e : def.entrySet()) {
            if (!config.contains(e.getKey())) {
                config.set(e.getKey(), e.getValue());
            }
        }
        logblock.saveConfig();

        ComparableVersion configVersion = new ComparableVersion(config.getString("version"));
        boolean oldConfig = configVersion.compareTo(new ComparableVersion(logblock.getDescription().getVersion().replace(" (manually compiled)", ""))) < 0;

        url = "jdbc:mysql://" + config.getString("mysql.host") + ":" + config.getInt("mysql.port") + "/" + getStringIncludingInts(config, "mysql.database");
        user = getStringIncludingInts(config, "mysql.user");
        password = getStringIncludingInts(config, "mysql.password");
        mysqlUseSSL = config.getBoolean("mysql.useSSL", true);
        mysqlRequireSSL = config.getBoolean("mysql.requireSSL", false);
        delayBetweenRuns = config.getInt("consumer.delayBetweenRuns", 2);
        forceToProcessAtLeast = config.getInt("consumer.forceToProcessAtLeast", 0);
        timePerRun = config.getInt("consumer.timePerRun", 1000);
        fireCustomEvents = config.getBoolean("consumer.fireCustomEvents", false);
        useBukkitScheduler = config.getBoolean("consumer.useBukkitScheduler", true);
        queueWarningSize = config.getInt("consumer.queueWarningSize", 1000);
        enableAutoClearLog = config.getBoolean("clearlog.enableAutoClearLog");
        autoClearLog = config.getStringList("clearlog.auto");
        dumpDeletedLog = config.getBoolean("clearlog.dumpDeletedLog", false);
        autoClearLogDelay = parseTimeSpec(config.getString("clearlog.autoClearLogDelay").split(" "));
        logBedExplosionsAsPlayerWhoTriggeredThese = config.getBoolean("logging.logBedExplosionsAsPlayerWhoTriggeredThese", true);
        logCreeperExplosionsAsPlayerWhoTriggeredThese = config.getBoolean("logging.logCreeperExplosionsAsPlayerWhoTriggeredThese", false);
        logFireSpreadAsPlayerWhoCreatedIt = config.getBoolean("logging.logFireSpreadAsPlayerWhoCreatedIt", true);
        logFluidFlowAsPlayerWhoTriggeredIt = config.getBoolean("logging.logFluidFlowAsPlayerWhoTriggeredIt", false);
        logPlayerInfo = config.getBoolean("logging.logPlayerInfo", true);
        try {
            logKillsLevel = LogKillsLevel.valueOf(config.getString("logging.logKillsLevel").toUpperCase());
        } catch (final IllegalArgumentException ex) {
            throw new DataFormatException("logging.logKillsLevel doesn't appear to be a valid log level. Allowed are 'PLAYERS', 'MONSTERS' and 'ANIMALS'");
        }
        logEnvironmentalKills = config.getBoolean("logging.logEnvironmentalKills", false);
        hiddenPlayers = new HashSet<>();
        for (final String playerName : config.getStringList("logging.hiddenPlayers")) {
            hiddenPlayers.add(playerName.toLowerCase().trim());
        }
        hiddenBlocks = new HashSet<>();
        for (final String blocktype : config.getStringList("logging.hiddenBlocks")) {
            final Material mat = Material.matchMaterial(blocktype);
            if (mat != null) {
                hiddenBlocks.add(mat);
            } else if (!oldConfig) {
                throw new DataFormatException("Not a valid material in hiddenBlocks: '" + blocktype + "'");
            }
        }
        ignoredChat = new ArrayList<>();
        for (String chatCommand : config.getStringList("logging.ignoredChat")) {
            ignoredChat.add(chatCommand.toLowerCase());
        }
        dontRollback = new HashSet<>();
        for (String e : config.getStringList("rollback.dontRollback")) {
            Material mat = Material.matchMaterial(e);
            if (mat != null) {
                dontRollback.add(mat);
            } else if (!oldConfig) {
                throw new DataFormatException("Not a valid material in dontRollback: '" + e + "'");
            }
        }
        replaceAnyway = new HashSet<>();
        for (String e : config.getStringList("rollback.replaceAnyway")) {
            Material mat = Material.matchMaterial(e);
            if (mat != null) {
                replaceAnyway.add(mat);
            } else if (!oldConfig) {
                throw new DataFormatException("Not a valid material in replaceAnyway: '" + e + "'");
            }
        }
        rollbackMaxTime = parseTimeSpec(config.getString("rollback.maxTime").split(" "));
        rollbackMaxArea = config.getInt("rollback.maxArea", 50);
        defaultDist = config.getInt("lookup.defaultDist", 20);
        defaultTime = parseTimeSpec(config.getString("lookup.defaultTime").split(" "));
        linesPerPage = config.getInt("lookup.linesPerPage", 15);
        linesLimit = config.getInt("lookup.linesLimit", 1500);
        hardLinesLimit = config.getInt("lookup.hardLinesLimit", 100000);
        askRollbacks = config.getBoolean("questioner.askRollbacks", true);
        askRedos = config.getBoolean("questioner.askRedos", true);
        askClearLogs = config.getBoolean("questioner.askClearLogs", true);
        askClearLogAfterRollback = config.getBoolean("questioner.askClearLogAfterRollback", true);
        askRollbackAfterBan = config.getBoolean("questioner.askRollbackAfterBan", false);
        safetyIdCheck = config.getBoolean("safety.id.check", true);
        debug = config.getBoolean("debug", false);
        banPermission = config.getString("questioner.banPermission");
        final List<Tool> tools = new ArrayList<>();
        final ConfigurationSection toolsSec = config.getConfigurationSection("tools");
        for (final String toolName : toolsSec.getKeys(false)) {
            try {
                final ConfigurationSection tSec = toolsSec.getConfigurationSection(toolName);
                final List<String> aliases = tSec.getStringList("aliases");
                final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(tSec.getString("leftClickBehavior").toUpperCase());
                final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(tSec.getString("rightClickBehavior").toUpperCase());
                final boolean defaultEnabled = tSec.getBoolean("defaultEnabled", false);
                final Material item = Material.matchMaterial(tSec.getString("item", "OAK_LOG"));
                final boolean canDrop = tSec.getBoolean("canDrop", false);
                final boolean removeOnDisable = tSec.getBoolean("removeOnDisable", true);
                final boolean dropToDisable = tSec.getBoolean("dropToDisable", false);
                final QueryParams params = new QueryParams(logblock);
                params.prepareToolQuery = true;
                params.parseArgs(getConsoleSender(), Arrays.asList(tSec.getString("params").split(" ")), false);
                final ToolMode mode = ToolMode.valueOf(tSec.getString("mode").toUpperCase());
                final PermissionDefault pdef = PermissionDefault.valueOf(tSec.getString("permissionDefault").toUpperCase());
                tools.add(new Tool(toolName, aliases, leftClickBehavior, rightClickBehavior, defaultEnabled, item, canDrop, params, mode, pdef, removeOnDisable, dropToDisable));
            } catch (final Exception ex) {
                getLogger().log(Level.WARNING, "Error at parsing tool '" + toolName + "': ", ex);
            }
        }
        toolsByName = new HashMap<>();
        toolsByType = new HashMap<>();
        for (final Tool tool : tools) {
            toolsByType.put(tool.item, tool);
            toolsByName.put(tool.name.toLowerCase(), tool);
            for (final String alias : tool.aliases) {
                toolsByName.put(alias, tool);
            }
        }
        final List<String> loggedWorlds = config.getStringList("loggedWorlds");
        worldConfigs = new HashMap<>();
        if (loggedWorlds.isEmpty()) {
            throw new DataFormatException("No worlds configured");
        }
        for (final String world : loggedWorlds) {
            worldConfigs.put(world, new WorldConfig(world, new File(logblock.getDataFolder(), friendlyWorldname(world) + ".yml")));
        }
        superWorldConfig = new LoggingEnabledMapping();
        for (final WorldConfig wcfg : worldConfigs.values()) {
            for (final Logging l : Logging.values()) {
                if (wcfg.isLogging(l)) {
                    superWorldConfig.setLogging(l, true);
                }
            }
        }
    }

    private static String getStringIncludingInts(ConfigurationSection cfg, String key) {
        String str = cfg.getString(key);
        if (str == null) {
            str = String.valueOf(cfg.getInt(key));
        }
        if (str == null) {
            str = "No value set for '" + key + "'";
        }
        return str;
    }

    public static boolean isLogging(World world, Logging l) {
        final WorldConfig wcfg = worldConfigs.get(world.getName());
        return wcfg != null && wcfg.isLogging(l);
    }

    public static boolean isLogging(String worldName, Logging l) {
        final WorldConfig wcfg = worldConfigs.get(worldName);
        return wcfg != null && wcfg.isLogging(l);
    }

    public static boolean isLogged(World world) {
        return worldConfigs.containsKey(world.getName());
    }

    public static WorldConfig getWorldConfig(World world) {
        return worldConfigs.get(world.getName());
    }

    public static WorldConfig getWorldConfig(String world) {
        return worldConfigs.get(world);
    }

    public static boolean isLogging(Logging l) {
        return superWorldConfig.isLogging(l);
    }

    public static Collection<WorldConfig> getLoggedWorlds() {
        return worldConfigs.values();
    }

    public static boolean isLogging(World world, EntityLogging logging, Entity entity) {
        final WorldConfig wcfg = worldConfigs.get(world.getName());
        return wcfg != null && wcfg.isLogging(logging, entity);
    }

    public static boolean isLoggingAnyEntities() {
        for (WorldConfig worldConfig : worldConfigs.values()) {
            if (worldConfig.isLoggingAnyEntities()) {
                return true;
            }
        }
        return false;
    }
}

class LoggingEnabledMapping {
    private final EnumSet<Logging> logging = EnumSet.noneOf(Logging.class);

    public void setLogging(Logging l, boolean enabled) {
        if (enabled) {
            logging.add(l);
        } else {
            logging.remove(l);
        }
    }

    public boolean isLogging(Logging l) {
        return logging.contains(l);
    }
}
