package de.diddiz.LogBlock;

import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.listeners.*;
import de.diddiz.LogBlock.questioner.Questioner;
import de.diddiz.util.BukkitUtils;
import de.diddiz.util.MySQLConnectionPool;
import de.diddiz.worldedit.WorldEditHelper;
import de.diddiz.worldedit.WorldEditLoggingHook;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.*;
import static org.bukkit.Bukkit.getPluginManager;

public class LogBlock extends JavaPlugin {
    private static LogBlock logblock = null;
    private MySQLConnectionPool pool;
    private Consumer consumer = null;
    private CommandsHandler commandsHandler;
    private boolean noDb = false, connected = true;
    private PlayerInfoLogging playerInfoLogging;
    private Questioner questioner;
    private volatile boolean isCompletelyEnabled;

    public static LogBlock getInstance() {
        return logblock;
    }

    public boolean isCompletelyEnabled() {
        return isCompletelyEnabled;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public CommandsHandler getCommandsHandler() {
        return commandsHandler;
    }

    @Override
    public void onEnable() {
        logblock = this;

        BukkitUtils.isDoublePlant(Material.AIR); // Force static code to run
        final PluginManager pm = getPluginManager();

        consumer = new Consumer(this);
        try {
            Config.load(this);
        } catch (final Exception ex) {
            getLogger().log(Level.SEVERE, "Could not load LogBlock config! " + ex.getMessage());
            pm.disablePlugin(this);
            return;
        }
        try {
            getLogger().info("Connecting to " + user + "@" + url + "...");
            pool = new MySQLConnectionPool(url, user, password, mysqlUseSSL, mysqlRequireSSL);
            final Connection conn = getConnection(true);
            if (conn == null) {
                noDb = true;
                return;
            }
            final Statement st = conn.createStatement();
            final ResultSet rs = st.executeQuery("SHOW CHARACTER SET where charset='utf8mb4';");
            if (rs.next()) {
                Config.mb4 = true;
                // Allegedly JDBC driver since 2010 hasn't needed this. I did.
                st.executeQuery("SET NAMES utf8mb4;");
            }
            conn.close();
            Updater updater = new Updater(this);
            updater.checkTables();
            MaterialConverter.initializeMaterials(getConnection());
            MaterialConverter.getOrAddMaterialId(Material.AIR.getKey()); // AIR must be the first entry
            EntityTypeConverter.initializeEntityTypes(getConnection());
            if (updater.update()) {
                load(this);
            }
        } catch (final NullPointerException ex) {
            getLogger().log(Level.SEVERE, "Error while loading: ", ex);
        } catch (final Exception ex) {
            getLogger().log(Level.SEVERE, "Error while loading: " + ex.getMessage(), ex);
            pm.disablePlugin(this);
            return;
        }

        if (WorldEditHelper.hasWorldEdit()) {
            new WorldEditLoggingHook(this).hook();
        }
        commandsHandler = new CommandsHandler(this);
        getCommand("lb").setExecutor(commandsHandler);
        if (enableAutoClearLog && autoClearLogDelay > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, new AutoClearLog(this), 6000, autoClearLogDelay * 60 * 20);
        }
        new DumpedLogImporter(this).run();
        registerEvents();
        consumer.start();
        for (final Tool tool : toolsByType.values()) {
            if (pm.getPermission("logblock.tools." + tool.name) == null) {
                final Permission perm = new Permission("logblock.tools." + tool.name, tool.permissionDefault);
                pm.addPermission(perm);
            }
        }
        questioner = new Questioner(this);
        isCompletelyEnabled = true;
        getServer().getScheduler().runTaskAsynchronously(this, new Updater.PlayerCountChecker(this));
    }

    private void registerEvents() {
        final PluginManager pm = getPluginManager();
        pm.registerEvents(new ToolListener(this), this);
        pm.registerEvents(playerInfoLogging = new PlayerInfoLogging(this), this);
        if (askRollbackAfterBan) {
            pm.registerEvents(new BanListener(this), this);
        }
        if (isLogging(Logging.BLOCKPLACE)) {
            pm.registerEvents(new BlockPlaceLogging(this), this);
        }
        if (isLogging(Logging.LAVAFLOW) || isLogging(Logging.WATERFLOW)) {
            pm.registerEvents(new FluidFlowLogging(this), this);
        }
        if (isLogging(Logging.BLOCKBREAK)) {
            pm.registerEvents(new BlockBreakLogging(this), this);
        }
        if (isLogging(Logging.SIGNTEXT)) {
            pm.registerEvents(new SignChangeLogging(this), this);
        }
        if (isLogging(Logging.FIRE)) {
            pm.registerEvents(new BlockBurnLogging(this), this);
        }
        if (isLogging(Logging.SNOWFORM)) {
            pm.registerEvents(new SnowFormLogging(this), this);
        }
        if (isLogging(Logging.SNOWFADE)) {
            pm.registerEvents(new SnowFadeLogging(this), this);
        }
        if (isLogging(Logging.CREEPEREXPLOSION) || isLogging(Logging.TNTEXPLOSION) || isLogging(Logging.GHASTFIREBALLEXPLOSION) || isLogging(Logging.ENDERDRAGON) || isLogging(Logging.MISCEXPLOSION)) {
            pm.registerEvents(new ExplosionLogging(this), this);
        }
        if (isLogging(Logging.LEAVESDECAY)) {
            pm.registerEvents(new LeavesDecayLogging(this), this);
        }
        if (isLogging(Logging.CHESTACCESS)) {
            pm.registerEvents(new ChestAccessLogging(this), this);
        }
        if (isLogging(Logging.BLOCKBREAK) || isLogging(Logging.BLOCKPLACE) || isLogging(Logging.SWITCHINTERACT) || isLogging(Logging.DOORINTERACT) || isLogging(Logging.CAKEEAT) || isLogging(Logging.DIODEINTERACT) || isLogging(Logging.COMPARATORINTERACT) || isLogging(Logging.NOTEBLOCKINTERACT)
                || isLogging(Logging.PRESUREPLATEINTERACT) || isLogging(Logging.TRIPWIREINTERACT) || isLogging(Logging.CROPTRAMPLE)) {
            pm.registerEvents(new InteractLogging(this), this);
        }
        if (isLogging(Logging.CREATURECROPTRAMPLE)) {
            pm.registerEvents(new CreatureInteractLogging(this), this);
        }
        if (isLogging(Logging.KILL)) {
            pm.registerEvents(new KillLogging(this), this);
        }
        if (isLogging(Logging.CHAT)) {
            pm.registerEvents(new ChatLogging(this), this);
        }
        if (isLogging(Logging.ENDERMEN)) {
            pm.registerEvents(new EndermenLogging(this), this);
        }
        if (isLogging(Logging.WITHER)) {
            pm.registerEvents(new WitherLogging(this), this);
        }
        if (isLogging(Logging.NATURALSTRUCTUREGROW) || isLogging(Logging.BONEMEALSTRUCTUREGROW)) {
            pm.registerEvents(new StructureGrowLogging(this), this);
        }
        if (isLogging(Logging.GRASSGROWTH) || isLogging(Logging.MYCELIUMSPREAD) || isLogging(Logging.VINEGROWTH) || isLogging(Logging.MUSHROOMSPREAD)) {
            pm.registerEvents(new BlockSpreadLogging(this), this);
        }
        if (isLogging(Logging.DRAGONEGGTELEPORT)) {
            pm.registerEvents(new DragonEggLogging(this), this);
        }
        if (isLogging(Logging.LECTERNBOOKCHANGE)) {
            pm.registerEvents(new LecternLogging(this), this);
        }
        if (Config.isLoggingAnyEntities()) {
            if (!WorldEditHelper.hasFullWorldEdit()) {
                getLogger().severe("No compatible WorldEdit found, entity logging will not work!");
            } else {
                pm.registerEvents(new AdvancedEntityLogging(this), this);
                getLogger().info("Entity logging enabled!");
            }
        }
    }

    @Override
    public void onDisable() {
        isCompletelyEnabled = false;
        getServer().getScheduler().cancelTasks(this);
        if (consumer != null) {
            if (logPlayerInfo && playerInfoLogging != null) {
                for (final Player player : getServer().getOnlinePlayers()) {
                    playerInfoLogging.onPlayerQuit(player);
                }
            }
            getLogger().info("Waiting for consumer ...");
            consumer.shutdown();
            if (consumer.getQueueSize() > 0) {
                getLogger().info("Remaining queue size: " + consumer.getQueueSize() + ". Trying to write to a local file.");
                try {
                    consumer.writeToFile();
                    getLogger().info("Successfully dumped queue.");
                } catch (final FileNotFoundException ex) {
                    getLogger().info("Failed to write. Given up.");
                }
            }
        }
        if (pool != null) {
            pool.close();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (noDb) {
            sender.sendMessage(ChatColor.RED + "No database connected. Check your MySQL user/pw and database for typos. Start/restart your MySQL server.");
        }
        return true;
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    public Connection getConnection() {
        return getConnection(false);
    }

    public Connection getConnection(boolean testConnection) {
        try {
            final Connection conn = pool.getConnection();
            if (!connected) {
                getLogger().info("MySQL connection rebuild");
                connected = true;
            }
            return conn;
        } catch (final Exception ex) {
            if (testConnection) {
                getLogger().log(Level.SEVERE, "Could not connect to the Database! Please check your config! " + ex.getMessage());
            } else if (connected) {
                getLogger().log(Level.SEVERE, "Error while fetching connection: ", ex);
                connected = false;
            } else {
                getLogger().log(Level.SEVERE, "MySQL connection lost", ex);
            }
            return null;
        }
    }

    /**
     * Returns a list of block changes based on the given query parameters, the query parameters
     * are essentially programmatic versions of the parameters a player would pass
     * to the logblock lookup command i.e /lb lookup <i>query-parameters</i>
     *
     * Note: this method directly calls a SQL query and is hence a slow blocking function, avoid running
     * it on the main game thread
     *
     * @param params QueryParams that contains the needed columns (all other will be filled with default values) and the params. World is required.
     * @return Returns a list of block changes based on the given query parameters
     * @throws SQLException if a sql exception occurs while looking up the block changes
     */
    public List<BlockChange> getBlockChanges(QueryParams params) throws SQLException {
        final Connection conn = getConnection();
        Statement state = null;
        if (conn == null) {
            throw new SQLException("No connection");
        }
        try {
            state = conn.createStatement();
            final ResultSet rs = state.executeQuery(params.getQuery());
            final List<BlockChange> blockchanges = new ArrayList<>();
            while (rs.next()) {
                blockchanges.add(new BlockChange(rs, params));
            }
            return blockchanges;
        } finally {
            if (state != null) {
                state.close();
            }
            conn.close();
        }
    }

    public int getCount(QueryParams params) throws SQLException {
        if (params == null || params.world == null || !Config.isLogged(params.world)) {
            throw new IllegalArgumentException("World is not logged: " + ((params == null || params.world == null) ? "null" : params.world.getName()));
        }
        final Connection conn = getConnection();
        Statement state = null;
        if (conn == null) {
            throw new SQLException("No connection");
        }
        try {
            state = conn.createStatement();
            final QueryParams p = params.clone();
            p.needCount = true;
            final ResultSet rs = state.executeQuery(p.getQuery());
            if (!rs.next()) {
                return 0;
            }
            return rs.getInt(1);
        } finally {
            if (state != null) {
                state.close();
            }
            conn.close();
        }
    }

    @Override
    public File getFile() {
        return super.getFile();
    }

    public Questioner getQuestioner() {
        return questioner;
    }
}
