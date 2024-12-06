package de.diddiz.LogBlock;

import de.diddiz.LogBlock.blockstate.BlockStateCodecSign;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.ComparableVersion;
import de.diddiz.LogBlock.util.ItemStackAndAmount;
import de.diddiz.LogBlock.util.UUIDFetcher;
import de.diddiz.LogBlock.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.getLoggedWorlds;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.util.BukkitUtils.friendlyWorldname;

class Updater {
    private final LogBlock logblock;
    final int UUID_CONVERT_BATCH_SIZE = 100;
    final int BLOCKS_CONVERT_BATCH_SIZE = 100000;
    final int OTHER_CONVERT_BATCH_SIZE = 20000;

    Updater(LogBlock logblock) {
        this.logblock = logblock;
    }

    boolean update() {
        final ConfigurationSection config = logblock.getConfig();
        String versionString = config.getString("version");
        ComparableVersion configVersion = new ComparableVersion(versionString);
        // if (configVersion.compareTo(new ComparableVersion(logblock.getDescription().getVersion().replace(" (manually compiled)", ""))) >= 0) {
        // return false;
        // }
        if (configVersion.compareTo(new ComparableVersion("1.2.7")) < 0) {
            logblock.getLogger().info("Updating tables to 1.2.7 ...");
            if (isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS)) {
                final Connection conn = logblock.getConnection();
                try {
                    conn.setAutoCommit(true);
                    final Statement st = conn.createStatement();
                    st.execute("ALTER TABLE `lb-chat` ADD FULLTEXT message (message)");
                    st.close();
                    conn.close();
                } catch (final SQLException ex) {
                    logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                    return false;
                }
            }
            config.set("version", "1.2.7");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3")) < 0) {
            logblock.getLogger().info("Updating config to 1.3.0 ...");
            for (final String tool : config.getConfigurationSection("tools").getKeys(false)) {
                if (config.get("tools." + tool + ".permissionDefault") == null) {
                    config.set("tools." + tool + ".permissionDefault", "OP");
                }
            }
            config.set("version", "1.3.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3.1")) < 0) {
            logblock.getLogger().info("Updating tables to 1.3.1 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD COLUMN lastlogin DATETIME NOT NULL, ADD COLUMN onlinetime TIME NOT NULL, ADD COLUMN ip VARCHAR(255) NOT NULL");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.3.1");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3.2")) < 0) {
            logblock.getLogger().info("Updating tables to 1.3.2 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD COLUMN firstlogin DATETIME NOT NULL AFTER playername");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.3.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.4")) < 0) {
            logblock.getLogger().info("Updating config to 1.4.0 ...");
            config.set("clearlog.keepLogDays", null);
            config.set("version", "1.4.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.4.2")) < 0) {
            logblock.getLogger().info("Updating config to 1.4.2 ...");
            for (final String world : config.getStringList("loggedWorlds")) {
                final File file = new File(logblock.getDataFolder(), friendlyWorldname(world) + ".yml");
                final YamlConfiguration wcfg = YamlConfiguration.loadConfiguration(file);
                if (wcfg.contains("logBlockCreations")) {
                    wcfg.set("logging.BLOCKPLACE", wcfg.getBoolean("logBlockCreations"));
                }
                if (wcfg.contains("logBlockDestroyings")) {
                    wcfg.set("logging.BLOCKBREAK", wcfg.getBoolean("logBlockDestroyings"));
                }
                if (wcfg.contains("logSignTexts")) {
                    wcfg.set("logging.SIGNTEXT", wcfg.getBoolean("logSignTexts"));
                }
                if (wcfg.contains("logFire")) {
                    wcfg.set("logging.FIRE", wcfg.getBoolean("logFire"));
                }
                if (wcfg.contains("logLeavesDecay")) {
                    wcfg.set("logging.LEAVESDECAY", wcfg.getBoolean("logLeavesDecay"));
                }
                if (wcfg.contains("logLavaFlow")) {
                    wcfg.set("logging.LAVAFLOW", wcfg.getBoolean("logLavaFlow"));
                }
                if (wcfg.contains("logWaterFlow")) {
                    wcfg.set("logging.WATERFLOW", wcfg.getBoolean("logWaterFlow"));
                }
                if (wcfg.contains("logChestAccess")) {
                    wcfg.set("logging.CHESTACCESS", wcfg.getBoolean("logChestAccess"));
                }
                if (wcfg.contains("logButtonsAndLevers")) {
                    wcfg.set("logging.SWITCHINTERACT", wcfg.getBoolean("logButtonsAndLevers"));
                }
                if (wcfg.contains("logKills")) {
                    wcfg.set("logging.KILL", wcfg.getBoolean("logKills"));
                }
                if (wcfg.contains("logChat")) {
                    wcfg.set("logging.CHAT", wcfg.getBoolean("logChat"));
                }
                if (wcfg.contains("logSnowForm")) {
                    wcfg.set("logging.SNOWFORM", wcfg.getBoolean("logSnowForm"));
                }
                if (wcfg.contains("logSnowFade")) {
                    wcfg.set("logging.SNOWFADE", wcfg.getBoolean("logSnowFade"));
                }
                if (wcfg.contains("logDoors")) {
                    wcfg.set("logging.DOORINTERACT", wcfg.getBoolean("logDoors"));
                }
                if (wcfg.contains("logCakes")) {
                    wcfg.set("logging.CAKEEAT", wcfg.getBoolean("logCakes"));
                }
                if (wcfg.contains("logEndermen")) {
                    wcfg.set("logging.ENDERMEN", wcfg.getBoolean("logEndermen"));
                }
                if (wcfg.contains("logExplosions")) {
                    final boolean logExplosions = wcfg.getBoolean("logExplosions");
                    wcfg.set("logging.TNTEXPLOSION", logExplosions);
                    wcfg.set("logging.MISCEXPLOSION", logExplosions);
                    wcfg.set("logging.CREEPEREXPLOSION", logExplosions);
                    wcfg.set("logging.GHASTFIREBALLEXPLOSION", logExplosions);
                }
                wcfg.set("logBlockCreations", null);
                wcfg.set("logBlockDestroyings", null);
                wcfg.set("logSignTexts", null);
                wcfg.set("logExplosions", null);
                wcfg.set("logFire", null);
                wcfg.set("logLeavesDecay", null);
                wcfg.set("logLavaFlow", null);
                wcfg.set("logWaterFlow", null);
                wcfg.set("logChestAccess", null);
                wcfg.set("logButtonsAndLevers", null);
                wcfg.set("logKills", null);
                wcfg.set("logChat", null);
                wcfg.set("logSnowForm", null);
                wcfg.set("logSnowFade", null);
                wcfg.set("logDoors", null);
                wcfg.set("logCakes", null);
                wcfg.set("logEndermen", null);
                try {
                    wcfg.save(file);
                } catch (final IOException ex) {
                    logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                }
            }
            config.set("clearlog.keepLogDays", null);
            config.set("version", "1.4.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.5.1")) < 0) {
            logblock.getLogger().info("Updating tables to 1.5.1 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    if (wcfg.isLogging(Logging.KILL)) {
                        st.execute("ALTER TABLE `" + wcfg.table + "-kills` ADD (x MEDIUMINT NOT NULL DEFAULT 0, y SMALLINT NOT NULL DEFAULT 0, z MEDIUMINT NOT NULL DEFAULT 0)");
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.5.1");
        }
        if (configVersion.compareTo(new ComparableVersion("1.5.2")) < 0) {
            logblock.getLogger().info("Updating tables to 1.5.2 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                final ResultSet rs = st.executeQuery("SHOW COLUMNS FROM `lb-players` WHERE field = 'onlinetime'");
                if (rs.next() && rs.getString("Type").equalsIgnoreCase("time")) {
                    st.execute("ALTER TABLE `lb-players` ADD onlinetime2 INT UNSIGNED NOT NULL");
                    st.execute("UPDATE `lb-players` SET onlinetime2 = HOUR(onlinetime) * 3600 + MINUTE(onlinetime) * 60 + SECOND(onlinetime)");
                    st.execute("ALTER TABLE `lb-players` DROP onlinetime");
                    st.execute("ALTER TABLE `lb-players` CHANGE onlinetime2 onlinetime INT UNSIGNED NOT NULL");
                } else {
                    logblock.getLogger().info("Column lb-players was already modified, skipping it.");
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.5.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.8.1")) < 0) {
            logblock.getLogger().info("Updating tables to 1.8.1 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    if (wcfg.isLogging(Logging.CHESTACCESS)) {
                        st.execute("ALTER TABLE `" + wcfg.table + "-chest` CHANGE itemdata itemdata SMALLINT NOT NULL");
                        logblock.getLogger().info("Table " + wcfg.table + "-chest modified");
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.8.1");
        }

        if (configVersion.compareTo(new ComparableVersion("1.9")) < 0) {
            logblock.getLogger().info("Updating tables to 1.9.0 ...");
            logblock.getLogger().info("Importing UUIDs for large databases may take some time");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD `UUID` VARCHAR(36) NOT NULL");
            } catch (final SQLException ex) {
                // Error 1060 is MySQL error "column already exists". We want to continue with import if we get that error
                if (ex.getErrorCode() != 1060) {
                    logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                    return false;
                }
            }
            try {
                String unimportedPrefix = "noimport_";
                ResultSet rs;
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                if (config.getBoolean("logging.logPlayerInfo")) {
                    // Start by assuming anything with no onlinetime is not a player
                    st.execute("UPDATE `lb-players` SET UUID = CONCAT ('log_',playername) WHERE onlinetime=0 AND LENGTH(UUID) = 0");
                } else {
                    // If we can't assume that, we must assume anything we can't look up is not a player
                    unimportedPrefix = "log_";
                }
                // Tell people how many are needing converted
                rs = st.executeQuery("SELECT COUNT(playername) FROM `lb-players` WHERE LENGTH(UUID)=0");
                rs.next();
                String total = Integer.toString(rs.getInt(1));
                logblock.getLogger().info(total + " players to convert");
                int done = 0;

                conn.setAutoCommit(false);
                Map<String, Integer> players = new HashMap<>();
                List<String> names = new ArrayList<>(UUID_CONVERT_BATCH_SIZE);
                Map<String, UUID> response;
                rs = st.executeQuery("SELECT playerid,playername FROM `lb-players` WHERE LENGTH(UUID)=0 LIMIT " + Integer.toString(UUID_CONVERT_BATCH_SIZE));
                while (rs.next()) {
                    do {
                        players.put(rs.getString(2), rs.getInt(1));
                        names.add(rs.getString(2));
                    } while (rs.next());
                    if (names.size() > 0) {
                        String theUUID;
                        response = UUIDFetcher.getUUIDs(names);
                        for (Map.Entry<String, Integer> entry : players.entrySet()) {
                            if (response.get(entry.getKey()) == null) {
                                theUUID = unimportedPrefix + entry.getKey();
                                logblock.getLogger().warning(entry.getKey() + " not found - giving UUID of " + theUUID);
                            } else {
                                theUUID = response.get(entry.getKey()).toString();
                            }
                            String thePID = entry.getValue().toString();
                            st.execute("UPDATE `lb-players` SET UUID = '" + theUUID + "' WHERE playerid = " + thePID);
                            done++;
                        }
                        conn.commit();
                        players.clear();
                        names.clear();
                        logblock.getLogger().info("Processed " + Integer.toString(done) + " out of " + total);
                        rs.close();
                        rs = st.executeQuery("SELECT playerid,playername FROM `lb-players` WHERE LENGTH(UUID)=0 LIMIT " + Integer.toString(UUID_CONVERT_BATCH_SIZE));
                    }
                }
                rs.close();
                st.close();
                conn.close();

            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            } catch (Exception ex) {
                logblock.getLogger().log(Level.SEVERE, "[UUID importer]", ex);
                return false;
            }
            config.set("version", "1.9.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.9.4")) < 0) {
            logblock.getLogger().info("Updating tables to 1.9.4 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                // Need to wrap both these next two inside individual try/catch statements in case index does not exist
                try {
                    st.execute("DROP INDEX UUID ON `lb-players`");
                } catch (final SQLException ex) {
                    if (ex.getErrorCode() != 1091) {
                        logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                        return false;
                    }
                }
                try {
                    st.execute("DROP INDEX playername ON `lb-players`");
                } catch (final SQLException ex) {
                    if (ex.getErrorCode() != 1091) {
                        logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                        return false;
                    }
                }
                st.execute("CREATE INDEX UUID ON `lb-players` (UUID);");
                st.execute("CREATE INDEX playername ON `lb-players` (playername);");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.9.4");
        }
        // Ensure charset for free-text fields is UTF-8, or UTF8-mb4 if possible
        // As this may be an expensive operation and the database default may already be this, check on a table-by-table basis before converting
        if (configVersion.compareTo(new ComparableVersion("1.10.0")) < 0) {
            logblock.getLogger().info("Updating tables to 1.10.0 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                checkCharset("lb-players", "name", st, false);
                if (isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS)) {
                    checkCharset("lb-chat", "message", st, false);
                }
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    if (wcfg.isLogging(Logging.SIGNTEXT)) {
                        // checkCharset(wcfg.table + "-sign","signtext",st);
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.10.0");
        }

        if (configVersion.compareTo(new ComparableVersion("1.12.0")) < 0) {
            logblock.getLogger().info("Updating tables to 1.12.0 ...");
            if (isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS)) {
                final Connection conn = logblock.getConnection();
                try {
                    conn.setAutoCommit(true);
                    final Statement st = conn.createStatement();
                    st.execute("ALTER TABLE `lb-chat` MODIFY COLUMN `message` VARCHAR(256) NOT NULL");
                    st.close();
                    conn.close();
                } catch (final SQLException ex) {
                    logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                    return false;
                }
            }
            config.set("version", "1.12.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.13.0")) < 0) {
            logblock.getLogger().info("Updating tables to 1.13.0 ...");
            try {
                MaterialUpdater1_13 materialUpdater = new MaterialUpdater1_13(logblock);
                logblock.getLogger().info("Convertig BlockId to BlockData. This can take a while ...");
                final Connection conn = logblock.getConnection();
                conn.setAutoCommit(false);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    logblock.getLogger().info("Processing world " + wcfg.world + "...");
                    logblock.getLogger().info("Processing block changes...");
                    boolean hadRow = true;
                    long rowsToConvert = 0;
                    long done = 0;
                    try {
                        ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "`");
                        if (rs.next()) {
                            rowsToConvert = rs.getLong(1);
                            logblock.getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table);
                        }
                        rs.close();

                        PreparedStatement deleteStatement = conn.prepareStatement("DELETE FROM `" + wcfg.table + "` WHERE id = ?");
                        PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO `" + wcfg.table + "-blocks` (id, date, playerid, replaced, replacedData, type, typeData, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                        while (hadRow) {
                            hadRow = false;
                            ResultSet entries = st.executeQuery("SELECT id, date, playerid, replaced, type, data, x, y, z FROM `" + wcfg.table + "` ORDER BY id ASC LIMIT " + BLOCKS_CONVERT_BATCH_SIZE);
                            while (entries.next()) {
                                hadRow = true;
                                long id = entries.getLong("id");
                                Timestamp date = entries.getTimestamp("date");
                                int playerid = entries.getInt("playerid");
                                int replaced = entries.getInt("replaced");
                                int type = entries.getInt("type");
                                int data = entries.getInt("data");
                                int x = entries.getInt("x");
                                int y = entries.getInt("y");
                                int z = entries.getInt("z");
                                if (data == 16) {
                                    data = 0;
                                }

                                try {
                                    BlockData replacedBlockData = materialUpdater.getBlockData(replaced, data);
                                    BlockData setBlockData = materialUpdater.getBlockData(type, data);

                                    int newReplacedId = MaterialConverter.getOrAddMaterialId(replacedBlockData);
                                    int newReplacedData = MaterialConverter.getOrAddBlockStateId(replacedBlockData);

                                    int newSetId = MaterialConverter.getOrAddMaterialId(setBlockData);
                                    int newSetData = MaterialConverter.getOrAddBlockStateId(setBlockData);

                                    insertStatement.setLong(1, id);
                                    insertStatement.setTimestamp(2, date);
                                    insertStatement.setInt(3, playerid);
                                    insertStatement.setInt(4, newReplacedId);
                                    insertStatement.setInt(5, newReplacedData);
                                    insertStatement.setInt(6, newSetId);
                                    insertStatement.setInt(7, newSetData);
                                    insertStatement.setInt(8, x);
                                    insertStatement.setInt(9, y);
                                    insertStatement.setInt(10, z);
                                    insertStatement.addBatch();
                                } catch (Exception e) {
                                    logblock.getLogger().info("Exception in entry " + id + " (" + replaced + ":" + data + "->" + type + ":" + data + "): " + e.getMessage());
                                }
                                deleteStatement.setLong(1, id);
                                deleteStatement.addBatch();

                                done++;
                            }
                            entries.close();
                            int failedRows = 0;
                            if (hadRow) {
                                try {
                                    insertStatement.executeBatch();
                                } catch (BatchUpdateException e) {
                                    for (int result : e.getUpdateCounts()) {
                                        if (result == Statement.EXECUTE_FAILED) {
                                            failedRows++;
                                        }
                                    }
                                }
                                deleteStatement.executeBatch();
                            }
                            conn.commit();
                            logblock.getLogger().info("Done: " + done + "/" + rowsToConvert + " " + (failedRows > 0 ? "Duplicates: " + failedRows + " " : "") + "(" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                        }
                        insertStatement.close();
                        deleteStatement.close();
                    } catch (SQLException e) {
                        logblock.getLogger().info("Could not convert " + wcfg.table + ": " + e.getMessage());
                    }

                    logblock.getLogger().info("Processing chests...");
                    rowsToConvert = 0;
                    done = 0;
                    try {
                        ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "-chest`");
                        if (rs.next()) {
                            rowsToConvert = rs.getLong(1);
                            logblock.getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table + "-chest");
                        }
                        rs.close();

                        PreparedStatement insertChestData = conn.prepareStatement("INSERT INTO `" + wcfg.table + "-chestdata` (id, item, itemremove, itemtype) VALUES (?, ?, ?, ?)");
                        PreparedStatement deleteChest = conn.prepareStatement("DELETE FROM `" + wcfg.table + "-chest` WHERE id = ?");
                        while (true) {
                            rs = st.executeQuery("SELECT id, itemtype, itemamount, itemdata FROM `" + wcfg.table + "-chest` ORDER BY id ASC LIMIT " + OTHER_CONVERT_BATCH_SIZE);
                            boolean anyRow = false;
                            while (rs.next()) {
                                anyRow = true;
                                long id = rs.getLong("id");
                                int itemtype = rs.getInt("itemtype");
                                int itemdata = rs.getInt("itemdata");
                                int amount = rs.getInt("itemamount");
                                Material weaponMaterial = materialUpdater.getMaterial(itemtype, itemdata);
                                if (weaponMaterial == null) {
                                    weaponMaterial = Material.AIR;
                                }
                                @SuppressWarnings("deprecation")
                                ItemStack stack = weaponMaterial.getMaxDurability() > 0 ? new ItemStack(weaponMaterial, Math.abs(amount), (short) itemdata) : new ItemStack(weaponMaterial, Math.abs(amount));
                                insertChestData.setLong(1, id);
                                insertChestData.setBytes(2, Utils.saveItemStack(ItemStackAndAmount.fromStack(stack)));
                                insertChestData.setInt(3, amount >= 0 ? 0 : 1);
                                insertChestData.setInt(4, MaterialConverter.getOrAddMaterialId(weaponMaterial));
                                insertChestData.addBatch();

                                deleteChest.setLong(1, id);
                                deleteChest.addBatch();
                                done++;
                            }
                            rs.close();
                            if (!anyRow) {
                                break;
                            }
                            int failedRows = 0;
                            try {
                                insertChestData.executeBatch();
                            } catch (BatchUpdateException e) {
                                for (int result : e.getUpdateCounts()) {
                                    if (result == Statement.EXECUTE_FAILED) {
                                        failedRows++;
                                    }
                                }
                            }
                            deleteChest.executeBatch();
                            conn.commit();
                            logblock.getLogger().info("Done: " + done + "/" + rowsToConvert + " " + (failedRows > 0 ? "Duplicates: " + failedRows + " " : "") + "(" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                        }
                        insertChestData.close();
                        deleteChest.close();
                    } catch (SQLException e) {
                        logblock.getLogger().info("Could not convert " + wcfg.table + "-chest: " + e.getMessage());
                    }

                    if (wcfg.isLogging(Logging.KILL)) {
                        logblock.getLogger().info("Processing kills...");
                        rowsToConvert = 0;
                        done = 0;
                        try {
                            ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "-kills`");
                            if (rs.next()) {
                                rowsToConvert = rs.getLong(1);
                                logblock.getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table + "-kills");
                            }
                            rs.close();

                            PreparedStatement updateWeaponStatement = conn.prepareStatement("UPDATE `" + wcfg.table + "-kills` SET weapon = ? WHERE id = ?");
                            for (int start = 0;; start += OTHER_CONVERT_BATCH_SIZE) {
                                rs = st.executeQuery("SELECT id, weapon FROM `" + wcfg.table + "-kills` ORDER BY id ASC LIMIT " + start + "," + OTHER_CONVERT_BATCH_SIZE);
                                boolean anyUpdate = false;
                                boolean anyRow = false;
                                while (rs.next()) {
                                    anyRow = true;
                                    long id = rs.getLong("id");
                                    int weapon = rs.getInt("weapon");
                                    Material weaponMaterial = materialUpdater.getMaterial(weapon, 0);
                                    if (weaponMaterial == null) {
                                        weaponMaterial = Material.AIR;
                                    }
                                    int newWeapon = MaterialConverter.getOrAddMaterialId(weaponMaterial);
                                    if (newWeapon != weapon) {
                                        anyUpdate = true;
                                        updateWeaponStatement.setInt(1, newWeapon);
                                        updateWeaponStatement.setLong(2, id);
                                        updateWeaponStatement.addBatch();
                                    }
                                    done++;
                                }
                                rs.close();
                                if (anyUpdate) {
                                    updateWeaponStatement.executeBatch();
                                    conn.commit();
                                }
                                logblock.getLogger().info("Done: " + done + "/" + rowsToConvert + " (" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                                if (!anyRow) {
                                    break;
                                }
                            }
                            updateWeaponStatement.close();
                        } catch (SQLException e) {
                            logblock.getLogger().info("Could not convert " + wcfg.table + "-kills: " + e.getMessage());
                        }
                    }
                }
                st.close();
                conn.close();

                logblock.getLogger().info("Updating config to 1.13.0 ...");
                config.set("logging.hiddenBlocks", materialUpdater.convertMaterials(config.getStringList("logging.hiddenBlocks")));
                config.set("rollback.dontRollback", materialUpdater.convertMaterials(config.getStringList("rollback.dontRollback")));
                config.set("rollback.replaceAnyway", materialUpdater.convertMaterials(config.getStringList("rollback.replaceAnyway")));
                final ConfigurationSection toolsSec = config.getConfigurationSection("tools");
                for (final String toolName : toolsSec.getKeys(false)) {
                    final ConfigurationSection tSec = toolsSec.getConfigurationSection(toolName);
                    tSec.set("item", materialUpdater.convertMaterial(tSec.getString("item", "OAK_LOG")));
                }
            } catch (final SQLException | IOException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.13.0");
        }

        if (configVersion.compareTo(new ComparableVersion("1.13.1")) < 0) {
            logblock.getLogger().info("Updating tables to 1.13.1 ...");
            BlockStateCodecSign signCodec = new BlockStateCodecSign();
            try (Connection conn = logblock.getConnection()) {
                conn.setAutoCommit(false);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    logblock.getLogger().info("Processing world " + wcfg.world + "...");
                    ResultSet rsCol = st.executeQuery("SHOW COLUMNS FROM `" + wcfg.table + "-chestdata` LIKE 'itemtype'");
                    if (!rsCol.next()) {
                        st.execute("ALTER TABLE `" + wcfg.table + "-chestdata` ADD COLUMN `itemtype` SMALLINT NOT NULL DEFAULT '0'");
                    }
                    rsCol.close();
                    conn.commit();
                    if (wcfg.isLogging(Logging.SIGNTEXT)) {
                        long rowsToConvert = 0;
                        long done = 0;
                        try {
                            ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "-sign`");
                            if (rs.next()) {
                                rowsToConvert = rs.getLong(1);
                                logblock.getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table + "-sign");
                            }
                            rs.close();

                            PreparedStatement insertSignState = conn.prepareStatement("INSERT INTO `" + wcfg.table + "-state` (id, replacedState, typeState) VALUES (?, ?, ?)");
                            PreparedStatement deleteSign = conn.prepareStatement("DELETE FROM `" + wcfg.table + "-sign` WHERE id = ?");
                            while (true) {
                                rs = st.executeQuery("SELECT id, signtext, replaced, type FROM `" + wcfg.table + "-sign` LEFT JOIN `" + wcfg.table + "-blocks` USING (id) ORDER BY id ASC LIMIT " + OTHER_CONVERT_BATCH_SIZE);
                                boolean anyRow = false;
                                while (rs.next()) {
                                    anyRow = true;
                                    long id = rs.getLong("id");
                                    String signText = rs.getString("signtext");
                                    int replaced = rs.getInt("replaced");
                                    boolean nullBlock = rs.wasNull();
                                    int type = rs.getInt("type");

                                    if (!nullBlock && signText != null) {
                                        String[] lines = signText.split("\0", 4);
                                        byte[] bytes = Utils.serializeYamlConfiguration(signCodec.serialize(null, Side.FRONT, lines));

                                        Material replacedMaterial = MaterialConverter.getBlockData(replaced, -1).getMaterial();
                                        Material typeMaterial = MaterialConverter.getBlockData(type, -1).getMaterial();
                                        boolean wasSign = replacedMaterial == Material.OAK_SIGN || replacedMaterial == Material.OAK_WALL_SIGN;
                                        boolean isSign = typeMaterial == Material.OAK_SIGN || typeMaterial == Material.OAK_WALL_SIGN;

                                        insertSignState.setLong(1, id);
                                        insertSignState.setBytes(2, wasSign ? bytes : null);
                                        insertSignState.setBytes(3, isSign ? bytes : null);
                                        insertSignState.addBatch();
                                    }

                                    deleteSign.setLong(1, id);
                                    deleteSign.addBatch();
                                    done++;
                                }
                                rs.close();
                                if (!anyRow) {
                                    break;
                                }
                                int failedRows = 0;
                                try {
                                    insertSignState.executeBatch();
                                } catch (BatchUpdateException e) {
                                    for (int result : e.getUpdateCounts()) {
                                        if (result == Statement.EXECUTE_FAILED) {
                                            failedRows++;
                                        }
                                    }
                                }
                                deleteSign.executeBatch();
                                conn.commit();
                                logblock.getLogger().info("Done: " + done + "/" + rowsToConvert + " " + (failedRows > 0 ? "Duplicates: " + failedRows + " " : "") + "(" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                            }
                            insertSignState.close();
                            deleteSign.close();
                        } catch (SQLException e) {
                            logblock.getLogger().info("Could not convert " + wcfg.table + "-sign: " + e.getMessage());
                        }
                    }
                }

                st.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }

            config.set("version", "1.13.1");
        }

        if (configVersion.compareTo(new ComparableVersion("1.16.0")) < 0) {
            logblock.getLogger().info("Updating tables to 1.16.0 ...");
            try (Connection conn = logblock.getConnection()) {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    createIndexIfDoesNotExist(wcfg.table + "-entities", "entityid", "KEY `entityid` (entityid)", st, false);
                }
                st.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Warning: Could not add index", ex);
            }
            config.set("version", "1.16.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.17.0")) < 0) {
            logblock.getLogger().info("Updating tables to 1.17.0 ...");
            logblock.getLogger().warning("The updating process might take several minutes if you have a huge log table! Please do not shutdown your server until it is completed.");
            try (Connection conn = logblock.getConnection()) {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    st.executeUpdate("ALTER TABLE `" + wcfg.table + "-blocks` CHANGE `y` `y` SMALLINT(5) NOT NULL");
                }
                st.close();
            } catch (final SQLException ex) {
                logblock.getLogger().log(Level.SEVERE, "[Updater] Warning: Could not alter table", ex);
            }
            logblock.getLogger().info("Update to 1.17.0 completed.");
            config.set("version", "1.17.0");
        }

        if (configVersion.compareTo(new ComparableVersion(Config.CURRENT_CONFIG_VERSION)) < 0) {
            config.set("version", Config.CURRENT_CONFIG_VERSION);
        }

        // this can always be checked
        try (Connection conn = logblock.getConnection()) {
            conn.setAutoCommit(true);
            final Statement st = conn.createStatement();
            checkCharset("lb-players", "name", st, true);
            if (isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS)) {
                checkCharset("lb-chat", "message", st, true);
            }
            createIndexIfDoesNotExist("lb-materials", "name", "UNIQUE KEY `name` (`name`(150))", st, true);
            createIndexIfDoesNotExist("lb-blockstates", "name", "UNIQUE KEY `name` (`name`(150))", st, true);

            st.close();
        } catch (final SQLException ex) {
            logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
            return false;
        }
        try (Connection conn = logblock.getConnection()) {
            conn.setAutoCommit(true);
            final Statement st = conn.createStatement();
            PreparedStatement stSelectColumnType = conn.prepareStatement("SELECT `TABLE_NAME`, `COLUMN_TYPE` FROM information_schema.`COLUMNS` WHERE `TABLE_SCHEMA` = ? AND `COLUMN_NAME` = ?");
            stSelectColumnType.setString(1, Config.mysqlDatabase);
            stSelectColumnType.setString(2, "y");
            HashMap<String, String> tablesAndYColumnType = new HashMap<>();
            try (ResultSet rs = stSelectColumnType.executeQuery()) {
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME").toLowerCase();
                    String type = rs.getString("COLUMN_TYPE").toLowerCase();
                    tablesAndYColumnType.put(table, type);
                }
            }
            for (final WorldConfig wcfg : getLoggedWorlds()) {
                String type = tablesAndYColumnType.get((wcfg.table + "-blocks").toLowerCase());
                if (type != null) {
                    if (type.contains("tinyint") || type.contains("unsigned")) {
                        logblock.getLogger().info("Fixing y column type for table " + wcfg.table + "-blocks ...");
                        logblock.getLogger().warning("The updating process might take several minutes if you have a huge log table! Please do not shutdown your server until it is completed.");
                        st.executeUpdate("ALTER TABLE `" + wcfg.table + "-blocks` CHANGE `y` `y` SMALLINT(5) NOT NULL");
                    }
                }
                type = tablesAndYColumnType.get((wcfg.table + "-entities").toLowerCase());
                if (type != null) {
                    if (type.contains("tinyint") || type.contains("unsigned")) {
                        logblock.getLogger().info("Fixing y column type for table " + wcfg.table + "-entities ...");
                        logblock.getLogger().warning("The updating process might take several minutes if you have a huge log table! Please do not shutdown your server until it is completed.");
                        st.executeUpdate("ALTER TABLE `" + wcfg.table + "-entities` CHANGE `y` `y` SMALLINT(5) NOT NULL");
                    }
                }
                type = tablesAndYColumnType.get((wcfg.table + "-kills").toLowerCase());
                if (type != null) {
                    if (type.contains("tinyint") || type.contains("unsigned")) {
                        logblock.getLogger().info("Fixing y column type for table " + wcfg.table + "-kills ...");
                        logblock.getLogger().warning("The updating process might take several minutes if you have a huge log table! Please do not shutdown your server until it is completed.");
                        st.executeUpdate("ALTER TABLE `" + wcfg.table + "-kills` CHANGE `y` `y` SMALLINT(5) NOT NULL");
                    }
                }
            }
            st.close();
            conn.close();
        } catch (final SQLException ex) {
            logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
            return false;
        }

        updateMaterialsPost1_13();

        logblock.saveConfig();
        return true;
    }

    void createIndexIfDoesNotExist(String table, String indexName, String definition, Statement st, boolean silent) throws SQLException {
        final ResultSet rs = st.executeQuery("SHOW INDEX FROM `" + table + "` WHERE Key_name = '" + indexName + "'");
        if (!rs.next()) {
            st.execute("ALTER TABLE `" + table + "` ADD " + definition);
            logblock.getLogger().info("Add index " + indexName + " to table " + table + ": Table modified");
        } else if (!silent) {
            logblock.getLogger().info("Add index " + indexName + " to table " + table + ": Already fine, skipping it");
        }
        rs.close();
    }

    void checkCharset(String table, String column, Statement st, boolean silent) throws SQLException {
        final ResultSet rs = st.executeQuery("SHOW FULL COLUMNS FROM `" + table + "` WHERE field = '" + column + "'");
        String charset = "utf8";
        if (Config.mb4) {
            charset = "utf8mb4";
        }
        if (rs.next() && !rs.getString("Collation").substring(0, charset.length()).equalsIgnoreCase(charset)) {
            st.execute("ALTER TABLE `" + table + "` CONVERT TO CHARSET " + charset);
            logblock.getLogger().info("Table " + table + " modified");
        } else if (!silent) {
            logblock.getLogger().info("Table " + table + " already fine, skipping it");
        }
        rs.close();
    }

    void checkTables() throws SQLException {
        String charset = "utf8";
        if (Config.mb4) {
            charset = "utf8mb4";
        }
        final Connection conn = logblock.getConnection();
        if (conn == null) {
            throw new SQLException("No connection");
        }
        final Statement state = conn.createStatement();
        conn.setAutoCommit(true);
        createTable(state, "lb-players", "(playerid INT UNSIGNED NOT NULL AUTO_INCREMENT, UUID varchar(36) NOT NULL, playername varchar(32) NOT NULL, firstlogin DATETIME NOT NULL, lastlogin DATETIME NOT NULL, onlinetime INT UNSIGNED NOT NULL, ip varchar(255) NOT NULL, PRIMARY KEY (playerid), INDEX (UUID), INDEX (playername)) DEFAULT CHARSET " + charset);
        // Players table must not be empty or inserts won't work - bug #492
        final ResultSet rs = state.executeQuery("SELECT NULL FROM `lb-players` LIMIT 1;");
        if (!rs.next()) {
            state.execute("INSERT IGNORE INTO `lb-players` (UUID,playername) VALUES ('log_dummy_record','dummy_record')");
        }
        if (isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS)) {
            try {
                createTable(state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, message VARCHAR(256) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) DEFAULT CHARSET " + charset);
            } catch (SQLException e) {
                createTable(state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, message VARCHAR(256) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid)) DEFAULT CHARSET " + charset);
            }
        }
        createTable(state, "lb-materials", "(id INT UNSIGNED NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (id)) DEFAULT CHARSET " + charset);
        createTable(state, "lb-blockstates", "(id INT UNSIGNED NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (id)) DEFAULT CHARSET " + charset);
        createTable(state, "lb-entitytypes", "(id INT UNSIGNED NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (id)) DEFAULT CHARSET " + charset);

        for (final WorldConfig wcfg : getLoggedWorlds()) {
            createTable(state, wcfg.table + "-blocks", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, replaced SMALLINT UNSIGNED NOT NULL, replacedData SMALLINT NOT NULL, type SMALLINT UNSIGNED NOT NULL, typeData SMALLINT NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
            createTable(state, wcfg.table + "-chestdata", "(id INT UNSIGNED NOT NULL, item MEDIUMBLOB, itemremove TINYINT, itemtype SMALLINT NOT NULL DEFAULT '0', PRIMARY KEY (id))");
            createTable(state, wcfg.table + "-state", "(id INT UNSIGNED NOT NULL, replacedState MEDIUMBLOB NULL, typeState MEDIUMBLOB NULL, PRIMARY KEY (id))");
            if (wcfg.isLogging(Logging.KILL)) {
                createTable(state, wcfg.table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer INT UNSIGNED, victim INT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id))");
            }
            createTable(state, wcfg.table + "-entityids", "(entityid INT UNSIGNED NOT NULL AUTO_INCREMENT, entityuuid VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, PRIMARY KEY (entityid), UNIQUE KEY (entityuuid))");
            createTable(state, wcfg.table + "-entities", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, entityid INT UNSIGNED NOT NULL, entitytypeid INT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT NOT NULL, z MEDIUMINT NOT NULL, action TINYINT UNSIGNED NOT NULL, data MEDIUMBLOB NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid), KEY entityid (entityid))");
        }
        state.close();
        conn.close();
    }

    private void createTable(Statement state, String table, String query) throws SQLException {
        try (ResultSet tableResult = state.executeQuery("SHOW TABLES LIKE '" + table + "'")) {
            if (!tableResult.next()) {
                logblock.getLogger().log(Level.INFO, "Creating table " + table + ".");
                state.execute("CREATE TABLE `" + table + "` " + query);
                try (ResultSet tableResultNew = state.executeQuery("SHOW TABLES LIKE '" + table + "'")) {
                    if (!tableResultNew.next()) {
                        throw new SQLException("Table " + table + " not found and failed to create");
                    }
                }
            }
        }
    }

    /**
     * Update materials that were renamed
     */
    private void updateMaterialsPost1_13() {
        final ConfigurationSection config = logblock.getConfig();
        String previousMinecraftVersion = config.getString("previousMinecraftVersion");
        if (previousMinecraftVersion == null) {
            previousMinecraftVersion = "1.13";
        }
        ComparableVersion comparablePreviousMinecraftVersion = new ComparableVersion(previousMinecraftVersion);
        String currentMinecraftVersion = logblock.getServer().getVersion();
        currentMinecraftVersion = currentMinecraftVersion.substring(currentMinecraftVersion.indexOf("(MC: ") + 5);
        int currentVersionEnd = currentMinecraftVersion.indexOf(" ");
        int currentVersionEnd2 = currentMinecraftVersion.indexOf(")");
        if (currentVersionEnd2 >= 0 && (currentVersionEnd < 0 || currentVersionEnd2 < currentVersionEnd)) {
            currentVersionEnd = currentVersionEnd2;
        }
        currentMinecraftVersion = currentMinecraftVersion.substring(0, currentVersionEnd);
        logblock.getLogger().info("[Updater] Current Minecraft Version: '" + currentMinecraftVersion + "'");
        ComparableVersion comparableCurrentMinecraftVersion = new ComparableVersion(currentMinecraftVersion);

        if (comparablePreviousMinecraftVersion.compareTo("1.14") < 0 && comparableCurrentMinecraftVersion.compareTo("1.14") >= 0) {
            logblock.getLogger().info("[Updater] Upgrading Materials to 1.14");
            renameMaterial("minecraft:sign", Material.OAK_SIGN);
            renameMaterial("minecraft:wall_sign", Material.OAK_WALL_SIGN);
            renameMaterial("minecraft:stone_slab", Material.SMOOTH_STONE_SLAB);
            renameMaterial("minecraft:rose_red", Material.RED_DYE);
            renameMaterial("minecraft:dandelion_yellow", Material.YELLOW_DYE);
            renameMaterial("minecraft:cactus_green", Material.GREEN_DYE);
        }

        if (comparablePreviousMinecraftVersion.compareTo("1.17") < 0 && comparableCurrentMinecraftVersion.compareTo("1.17") >= 0) {
            logblock.getLogger().info("[Updater] Upgrading Materials to 1.17");
            renameMaterial("minecraft:grass_path", Material.DIRT_PATH);
        }

        if (comparablePreviousMinecraftVersion.compareTo("1.20.4") < 0 && comparableCurrentMinecraftVersion.compareTo("1.20.4") >= 0) {
            logblock.getLogger().info("[Updater] Upgrading Materials to 1.20.4");
            renameMaterial("minecraft:grass", Material.SHORT_GRASS);
        }

        config.set("previousMinecraftVersion", currentMinecraftVersion);
        logblock.saveConfig();
    }

    private void renameMaterial(String oldName, Material newName) {
        final Connection conn = logblock.getConnection();
        try {
            conn.setAutoCommit(false);
            PreparedStatement stSelectMaterial = conn.prepareStatement("SELECT id FROM `lb-materials` WHERE name = ?");
            stSelectMaterial.setString(1, oldName);
            ResultSet rs = stSelectMaterial.executeQuery();
            if (rs.next()) {
                logblock.getLogger().info("[Updater] Updating " + oldName + " to " + newName);
                int oldId = rs.getInt(1);
                int newId = MaterialConverter.getOrAddMaterialId(newName);

                Statement st = conn.createStatement();
                int rows = 0;
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    rows += st.executeUpdate("UPDATE `" + wcfg.table + "-blocks` SET replaced = " + newId + " WHERE replaced = " + oldId);
                    rows += st.executeUpdate("UPDATE `" + wcfg.table + "-blocks` SET type = " + newId + " WHERE type = " + oldId);
                    rows += st.executeUpdate("UPDATE `" + wcfg.table + "-chestdata` SET itemtype = " + newId + " WHERE itemtype = " + oldId);
                    if (wcfg.isLogging(Logging.KILL)) {
                        rows += st.executeUpdate("UPDATE `" + wcfg.table + "-kills` SET weapon = " + newId + " WHERE weapon = " + oldId);
                    }
                }
                st.close();
                if (rows > 0) {
                    logblock.getLogger().info("[Updater] Successfully updated " + rows + " entries..");
                }
            }
            stSelectMaterial.close();
            conn.commit();
            conn.close();
        } catch (final SQLException ex) {
            logblock.getLogger().log(Level.SEVERE, "[Updater] Error: " + ex.getMessage(), ex);
        }
    }

    public static class PlayerCountChecker implements Runnable {

        private LogBlock logblock;

        public PlayerCountChecker(LogBlock logblock) {
            this.logblock = logblock;
        }

        @Override
        public void run() {
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT auto_increment FROM information_schema.columns AS col join information_schema.tables AS tab ON (col.table_schema=tab.table_schema AND col.table_name=tab.table_name) WHERE col.table_name = 'lb-players' AND col.column_name = 'playerid' AND col.data_type = 'smallint' AND col.table_schema = DATABASE() AND auto_increment > 65000;");
                if (rs.next()) {
                    for (int i = 0; i < 6; i++) {
                        logblock.getLogger().warning("Your server reached 65000 players. You should soon update your database table schema - see FAQ: https://github.com/LogBlock/LogBlock/wiki/FAQ#logblock-your-server-reached-65000-players-");
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                if (logblock.isCompletelyEnabled()) {
                    logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                }
            }
        }
    }

    public static class MaterialUpdater1_13 {
        BlockData[][] blockDataMapping;
        Material[][] itemMapping = new Material[10][];

        public MaterialUpdater1_13(LogBlock plugin) throws IOException {
            blockDataMapping = new BlockData[256][16];
            try (JarFile file = new JarFile(plugin.getFile())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(file.getInputStream(file.getJarEntry("blockdata.txt"))), "UTF-8"));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    int splitter1 = line.indexOf(":");
                    int splitter2 = line.indexOf(",");
                    if (splitter1 >= 0 && splitter2 >= 0) {
                        int blockid = Integer.parseInt(line.substring(0, splitter1));
                        int blockdata = Integer.parseInt(line.substring(splitter1 + 1, splitter2));
                        BlockData newBlockData = Bukkit.createBlockData(line.substring(splitter2 + 1));

                        if (blockdata == 0) {
                            for (int i = 0; i < 16; i++) {
                                if (blockDataMapping[blockid][i] == null) {
                                    blockDataMapping[blockid][i] = newBlockData;
                                }
                            }
                        } else {
                            blockDataMapping[blockid][blockdata] = newBlockData;
                        }
                    }
                }
                reader.close();

                HashMap<String, Material> materialKeysToMaterial = new HashMap<>();
                for (Material material : Material.values()) {
                    materialKeysToMaterial.put(material.getKey().toString(), material);
                }

                reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(file.getInputStream(file.getJarEntry("itemdata.txt"))), "UTF-8"));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    int splitter1 = line.indexOf(":");
                    int splitter2 = line.indexOf(",");
                    if (splitter1 >= 0 && splitter2 >= 0) {
                        int itemid = Integer.parseInt(line.substring(0, splitter1));
                        int itemdata = Integer.parseInt(line.substring(splitter1 + 1, splitter2));
                        Material newMaterial = materialKeysToMaterial.get(line.substring(splitter2 + 1));
                        if (newMaterial == null) {
                            throw new IOException("Unknown item: " + line.substring(splitter2 + 1));
                        }
                        if (itemid >= itemMapping.length) {
                            itemMapping = Arrays.copyOf(itemMapping, Math.max(itemMapping.length * 3 / 2, itemid + 1));
                        }

                        Material[] itemValues = itemMapping[itemid];
                        if (itemValues == null) {
                            itemValues = new Material[itemdata + 1];
                            itemMapping[itemid] = itemValues;
                        } else if (itemValues.length <= itemdata) {
                            itemValues = Arrays.copyOf(itemValues, itemdata + 1);
                            itemMapping[itemid] = itemValues;
                        }
                        itemValues[itemdata] = newMaterial;
                    }
                }
                reader.close();
            }
        }

        public BlockData getBlockData(int id, int data) {
            return id >= 0 && id < 256 && data >= 0 && data < 16 ? blockDataMapping[id][data] : null;
        }

        public Material getMaterial(int id, int data) {
            Material[] materials = id >= 0 && id < itemMapping.length ? itemMapping[id] : null;
            if (materials != null && materials.length > 0) {
                if (materials[0] != null && materials[0].getMaxDurability() == 0 && data >= 0 && data < materials.length && materials[data] != null) {
                    return materials[data];
                }
                return materials[0];
            }
            return null;
        }

        public Material getMaterial(String id) {
            int item = 0;
            int data = 0;
            int seperator = id.indexOf(':');
            if (seperator < 0) {
                item = Integer.parseInt(id);
            } else {
                item = Integer.parseInt(id.substring(0, seperator));
                data = Integer.parseInt(id.substring(seperator + 1));
            }
            return getMaterial(item, data);
        }

        public String convertMaterial(String oldEntry) {
            if (oldEntry == null) {
                return null;
            }
            try {
                Material newMaterial = getMaterial(oldEntry);
                if (newMaterial != null) {
                    return newMaterial.name();
                }
            } catch (Exception e) {
                Material newMaterial = Material.matchMaterial(oldEntry, true);
                if (newMaterial != null) {
                    return newMaterial.name();
                } else {
                    newMaterial = Material.matchMaterial(oldEntry);
                    if (newMaterial != null) {
                        return newMaterial.name();
                    }
                }
            }
            return null;
        }

        public List<String> convertMaterials(Collection<String> oldEntries) {
            Set<String> newEntries = new LinkedHashSet<>();
            for (String oldEntry : oldEntries) {
                String newEntry = convertMaterial(oldEntry);
                if (newEntry != null) {
                    newEntries.add(newEntry);
                    if (newEntry.equals(Material.AIR.name())) {
                        newEntries.add(Material.CAVE_AIR.name());
                        newEntries.add(Material.VOID_AIR.name());
                    }
                }
            }
            return new ArrayList<>(newEntries);
        }
    }
}
