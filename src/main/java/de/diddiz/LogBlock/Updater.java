package de.diddiz.LogBlock;

import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.UUIDFetcher;
import de.diddiz.util.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
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
import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import de.diddiz.util.ComparableVersion;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getLogger;

class Updater {
    private final LogBlock logblock;
    final int UUID_CONVERT_BATCH_SIZE = 100;

    Updater(LogBlock logblock) {
        this.logblock = logblock;
    }

    boolean update() {
        final ConfigurationSection config = logblock.getConfig();
        String versionString = config.getString("version");
        if (Pattern.matches("1\\.\\d{2}",versionString)) {
            versionString = "1." + versionString.charAt(2) + "." + versionString.charAt(3);
            config.set("version",versionString);
            logblock.saveConfig();
        }
        ComparableVersion configVersion = new ComparableVersion(versionString);
        if (configVersion.compareTo(new ComparableVersion(logblock.getDescription().getVersion())) >= 0) {
            return false;
        }
        if (configVersion.compareTo(new ComparableVersion("1.2.7")) < 0) {
            getLogger().info("Updating tables to 1.2.7 ...");
            if (isLogging(Logging.CHAT)) {
                final Connection conn = logblock.getConnection();
                try {
                    conn.setAutoCommit(true);
                    final Statement st = conn.createStatement();
                    st.execute("ALTER TABLE `lb-chat` ENGINE = MyISAM, ADD FULLTEXT message (message)");
                    st.close();
                    conn.close();
                } catch (final SQLException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                    return false;
                }
            }
            config.set("version", "1.2.7");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3")) < 0) {
            getLogger().info("Updating config to 1.3.0 ...");
            for (final String tool : config.getConfigurationSection("tools").getKeys(false)) {
                if (config.get("tools." + tool + ".permissionDefault") == null) {
                    config.set("tools." + tool + ".permissionDefault", "OP");
                }
            }
            config.set("version", "1.3.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3.1")) < 0) {
            getLogger().info("Updating tables to 1.3.1 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD COLUMN lastlogin DATETIME NOT NULL, ADD COLUMN onlinetime TIME NOT NULL, ADD COLUMN ip VARCHAR(255) NOT NULL");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.3.1");
        }
        if (configVersion.compareTo(new ComparableVersion("1.3.2")) < 0) {
            getLogger().info("Updating tables to 1.3.2 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD COLUMN firstlogin DATETIME NOT NULL AFTER playername");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.3.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.4")) < 0) {
            getLogger().info("Updating config to 1.4.0 ...");
            config.set("clearlog.keepLogDays", null);
            config.set("version", "1.4.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.4.2")) < 0) {
            getLogger().info("Updating config to 1.4.2 ...");
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
                    Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                }
            }
            config.set("clearlog.keepLogDays", null);
            config.set("version", "1.4.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.5.1")) < 0) {
            getLogger().info("Updating tables to 1.5.1 ...");
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
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.5.1");
        }
        if (configVersion.compareTo(new ComparableVersion("1.5.2")) < 0) {
            getLogger().info("Updating tables to 1.5.2 ...");
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
                    getLogger().info("Column lb-players was already modified, skipping it.");
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.5.2");
        }
        if (configVersion.compareTo(new ComparableVersion("1.8.1")) < 0) {
            getLogger().info("Updating tables to 1.8.1 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    if (wcfg.isLogging(Logging.CHESTACCESS)) {
                        st.execute("ALTER TABLE `" + wcfg.table + "-chest` CHANGE itemdata itemdata SMALLINT NOT NULL");
                        getLogger().info("Table " + wcfg.table + "-chest modified");
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.8.1");
        }

        if (configVersion.compareTo(new ComparableVersion("1.9")) < 0) {
            getLogger().info("Updating tables to 1.9.0 ...");
            getLogger().info("Importing UUIDs for large databases may take some time");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                st.execute("ALTER TABLE `lb-players` ADD `UUID` VARCHAR(36) NOT NULL");
            } catch (final SQLException ex) {
                // Error 1060 is MySQL error "column already exists". We want to continue with import if we get that error
                if (ex.getErrorCode() != 1060) {
                    Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
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
                getLogger().info(total + " players to convert");
                int done = 0;

                conn.setAutoCommit(false);
                Map<String, Integer> players = new HashMap<String, Integer>();
                List<String> names = new ArrayList<String>(UUID_CONVERT_BATCH_SIZE);
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
                                getLogger().warning(entry.getKey() + " not found - giving UUID of " + theUUID);
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
                        getLogger().info("Processed " + Integer.toString(done) + " out of " + total);
                        rs.close();
                        rs = st.executeQuery("SELECT playerid,playername FROM `lb-players` WHERE LENGTH(UUID)=0 LIMIT " + Integer.toString(UUID_CONVERT_BATCH_SIZE));
                    }
                }
                rs.close();
                st.close();
                conn.close();

            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[UUID importer]", ex);
                return false;
            }
            config.set("version", "1.9.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.9.4")) < 0) {
            getLogger().info("Updating tables to 1.9.4 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                // Need to wrap both these next two inside individual try/catch statements in case index does not exist
                try {
                    st.execute("DROP INDEX UUID ON `lb-players`");
                } catch (final SQLException ex) {
                    if (ex.getErrorCode() != 1091) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                        return false;
                    }
                }
                try {
                    st.execute("DROP INDEX playername ON `lb-players`");
                } catch (final SQLException ex) {
                    if (ex.getErrorCode() != 1091) {
                        Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                        return false;
                    }
                }
                st.execute("CREATE INDEX UUID ON `lb-players` (UUID);");
                st.execute("CREATE INDEX playername ON `lb-players` (playername);");
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.9.4");
        }
        // Ensure charset for free-text fields is UTF-8, or UTF8-mb4 if possible
        // As this may be an expensive operation and the database default may already be this, check on a table-by-table basis before converting
        if (configVersion.compareTo(new ComparableVersion("1.10.0")) < 0) {
            getLogger().info("Updating tables to 1.10.0 ...");
            final Connection conn = logblock.getConnection();
            try {
                conn.setAutoCommit(true);
                final Statement st = conn.createStatement();
                checkCharset("lb-players","name",st);
                if (isLogging(Logging.CHAT)) {
                    checkCharset("lb-chat","message", st);
                }
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    if (wcfg.isLogging(Logging.SIGNTEXT)) {
                        checkCharset(wcfg.table + "-sign","signtext",st);
                    }
                }
                st.close();
                conn.close();
            } catch (final SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.10.0");
        }

        if (configVersion.compareTo(new ComparableVersion("1.12.0")) < 0) {
            getLogger().info("Updating tables to 1.12.0 ...");
            if (isLogging(Logging.CHAT)) {
                final Connection conn = logblock.getConnection();
                try {
                    conn.setAutoCommit(true);
                    final Statement st = conn.createStatement();
                    st.execute("ALTER TABLE `lb-chat` MODIFY COLUMN `message` VARCHAR(256) NOT NULL");
                    st.close();
                    conn.close();
                } catch (final SQLException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                    return false;
                }
            }
            config.set("version", "1.12.0");
        }
        if (configVersion.compareTo(new ComparableVersion("1.13.0")) < 0) {
            getLogger().info("Updating tables to 1.13.0 ...");
            try {
                MaterialUpdater1_13 materialUpdater = new MaterialUpdater1_13(logblock);
                getLogger().info("Convertig BlockId to BlockData. This can take a while ...");
                final Connection conn = logblock.getConnection();
                conn.setAutoCommit(false);
                final Statement st = conn.createStatement();
                for (final WorldConfig wcfg : getLoggedWorlds()) {
                    getLogger().info("Processing world " + wcfg.world + "...");
                    getLogger().info("Processing block changes...");
                    boolean hadRow = true;
                    int rowsToConvert = 0;
                    int done = 0;
                    try {
                        ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "`");
                        if (rs.next()) {
                            rowsToConvert = rs.getInt(1);
                            getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table);
                        }
                        rs.close();
                        
                        PreparedStatement deleteStatement = conn.prepareStatement("DELETE FROM `" + wcfg.table + "` WHERE id = ?");
                        PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO `" + wcfg.table + "-blocks` (id, date, playerid, replaced, replacedData, type, typeData, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                        while (hadRow) {
                            hadRow = false;
                            ResultSet entries = st.executeQuery("SELECT id, date, playerid, replaced, type, data, x, y, z FROM `" + wcfg.table + "` ORDER BY id ASC LIMIT 10000");
                            while (entries.next()) {
                                hadRow = true;
                                int id = entries.getInt("id");
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
                                    String replacedBlockData = materialUpdater.getBlockData(replaced, data).getAsString();
                                    String setBlockData = materialUpdater.getBlockData(type, data).getAsString();

                                    int newReplacedId = MaterialConverter.getOrAddMaterialId(replacedBlockData);
                                    int newReplacedData = MaterialConverter.getOrAddBlockStateId(replacedBlockData);

                                    int newSetId = MaterialConverter.getOrAddMaterialId(setBlockData);
                                    int newSetData = MaterialConverter.getOrAddBlockStateId(setBlockData);

                                    insertStatement.setInt(1, id);
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
                                    getLogger().info("Exception in entry " + id + " (" + replaced + ":" + data + "->" + type + ":" + data + "): " + e.getMessage());
                                }
                                deleteStatement.setInt(1, id);
                                deleteStatement.addBatch();

                                done++;
                            }
                            entries.close();
                            if (hadRow) {
                                insertStatement.executeBatch();
                                deleteStatement.executeBatch();
                            }
                            conn.commit();
                            logblock.getConsumer().run(); // force a consumer run to save new material mappings
                            getLogger().info("Done: " + done + "/" + rowsToConvert + " (" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                        }
                        insertStatement.close();
                        deleteStatement.close();
                    } catch (SQLException e) {
                        getLogger().info("Could not convert " + wcfg.table + ": " + e.getMessage());
                    }

                    getLogger().info("Processing chests...");
                    rowsToConvert = 0;
                    done = 0;
                    try {
                        ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "-chest`");
                        if (rs.next()) {
                            rowsToConvert = rs.getInt(1);
                            getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table + "-chest");
                        }
                        rs.close();
                        
                        PreparedStatement insertChestData = conn.prepareStatement("INSERT INTO `" + wcfg.table + "-chestdata` (id, item, itemremove) VALUES (?, ?, ?)");
                        PreparedStatement deleteChest = conn.prepareStatement("DELETE FROM `" + wcfg.table + "-chest` WHERE id = ?");
                        while (true) {
                            rs = st.executeQuery("SELECT id, itemtype, itemamount, itemdata FROM `" + wcfg.table + "-chest` ORDER BY id ASC LIMIT 10000");
                            boolean anyRow = false;
                            while (rs.next()) {
                                anyRow = true;
                                int id = rs.getInt("id");
                                int itemtype = rs.getInt("itemtype");
                                int itemdata = rs.getInt("itemdata");
                                int amount = rs.getInt("itemamount");
                                Material weaponMaterial = materialUpdater.getMaterial(itemtype, itemdata);
                                if (weaponMaterial == null) {
                                    weaponMaterial = Material.AIR;
                                }
                                ItemStack stack = weaponMaterial.getMaxDurability() > 0 ? new ItemStack(weaponMaterial, Math.abs(amount), (short)itemdata) : new ItemStack(weaponMaterial, Math.abs(amount));
                                insertChestData.setInt(1, id);
                                insertChestData.setBytes(2, Utils.saveItemStack(stack));
                                insertChestData.setInt(3, amount >= 0 ? 0 : 1);
                                insertChestData.addBatch();
                                
                                deleteChest.setInt(1, id);
                                deleteChest.addBatch();
                                done++;
                            }
                            rs.close();
                            if (!anyRow) {
                                break;
                            }
                            insertChestData.executeBatch();
                            deleteChest.executeBatch();
                            conn.commit();
                            getLogger().info("Done: " + done + "/" + rowsToConvert + " (" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                        }
                        insertChestData.close();
                        deleteChest.close();
                    } catch (SQLException e) {
                        getLogger().info("Could not convert " + wcfg.table + "-chest: " + e.getMessage());
                    }

                    if (wcfg.isLogging(Logging.KILL)) {
                        getLogger().info("Processing kills...");
                        rowsToConvert = 0;
                        done = 0;
                        try {
                            ResultSet rs = st.executeQuery("SELECT count(*) as rowcount FROM `" + wcfg.table + "-kills`");
                            if (rs.next()) {
                                rowsToConvert = rs.getInt(1);
                                getLogger().info("Converting " + rowsToConvert + " entries in " + wcfg.table + "-kills");
                            }
                            rs.close();
                            
                            PreparedStatement updateWeaponStatement = conn.prepareStatement("UPDATE `" + wcfg.table + "`-kills SET weapon = ? WHERE id = ?");
                            for (int start = 0;; start += 10000) {
                                rs = st.executeQuery("SELECT id, weapon FROM `" + wcfg.table + "-kills` ORDER BY id ASC LIMIT " + start + ",10000");
                                boolean anyUpdate = false;
                                boolean anyRow = false;
                                while (rs.next()) {
                                    anyRow = true;
                                    int id = rs.getInt("id");
                                    int weapon = rs.getInt("weapon");
                                    Material weaponMaterial = materialUpdater.getMaterial(weapon, 0);
                                    if (weaponMaterial == null) {
                                        weaponMaterial = Material.AIR;
                                    }
                                    int newWeapon = MaterialConverter.getOrAddMaterialId(weaponMaterial.getKey());
                                    if (newWeapon != weapon) {
                                        anyUpdate = true;
                                        updateWeaponStatement.setInt(1, newWeapon);
                                        updateWeaponStatement.setInt(2, id);
                                        updateWeaponStatement.addBatch();
                                    }
                                    done++;
                                }
                                rs.close();
                                if (anyUpdate) {
                                    updateWeaponStatement.executeBatch();
                                    conn.commit();
                                    logblock.getConsumer().run(); // force a consumer run to save new material mappings
                                }
                                getLogger().info("Done: " + done + "/" + rowsToConvert + " (" + (rowsToConvert > 0 ? (done * 100 / rowsToConvert) : 100) + "%)");
                                if (!anyRow) {
                                    break;
                                }
                            }
                            updateWeaponStatement.close();
                        } catch (SQLException e) {
                            getLogger().info("Could not convert " + wcfg.table + "-kills: " + e.getMessage());
                        }
                    }
                }
                st.close();
                conn.close();

                getLogger().info("Updating config to 1.13.0 ...");
                config.set("logging.hiddenBlocks", materialUpdater.convertMaterials(config.getStringList("logging.hiddenBlocks")));
                config.set("rollback.dontRollback", materialUpdater.convertMaterials(config.getStringList("rollback.dontRollback")));
                config.set("rollback.replaceAnyway", materialUpdater.convertMaterials(config.getStringList("rollback.replaceAnyway")));
                final ConfigurationSection toolsSec = config.getConfigurationSection("tools");
                for (final String toolName : toolsSec.getKeys(false)) {
                    final ConfigurationSection tSec = toolsSec.getConfigurationSection(toolName);
                    tSec.set("item", materialUpdater.convertMaterial(tSec.getString("item", "OAK_LOG")));
                }
            } catch (final SQLException | IOException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
                return false;
            }
            config.set("version", "1.13.0");
        }

        logblock.saveConfig();
        return true;
    }

    void checkCharset(String table, String column, Statement st) throws SQLException {
        final ResultSet rs = st.executeQuery("SHOW FULL COLUMNS FROM `" + table + "` WHERE field = '" + column + "'");
        String charset = "utf8";
        if (Config.mb4) {
            charset = "utf8mb4";
        }
        if (rs.next() && !rs.getString("Collation").substring(0, charset.length()).equalsIgnoreCase(charset)) {
            st.execute("ALTER TABLE `" + table + "` CONVERT TO CHARSET " + charset);
            getLogger().info("Table " + table + " modified");
        } else {
            getLogger().info("Table " + table + " already fine, skipping it");
        }
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
        final DatabaseMetaData dbm = conn.getMetaData();
        conn.setAutoCommit(true);
        createTable(dbm, state, "lb-players", "(playerid INT UNSIGNED NOT NULL AUTO_INCREMENT, UUID varchar(36) NOT NULL, playername varchar(32) NOT NULL, firstlogin DATETIME NOT NULL, lastlogin DATETIME NOT NULL, onlinetime INT UNSIGNED NOT NULL, ip varchar(255) NOT NULL, PRIMARY KEY (playerid), INDEX (UUID), INDEX (playername)) DEFAULT CHARSET " + charset);
        // Players table must not be empty or inserts won't work - bug #492
        final ResultSet rs = state.executeQuery("SELECT NULL FROM `lb-players` LIMIT 1;");
        if (!rs.next()) {
            state.execute("INSERT IGNORE INTO `lb-players` (UUID,playername) VALUES ('log_dummy_record','dummy_record')");
        }
        if (isLogging(Logging.CHAT)) {
            createTable(dbm, state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, message VARCHAR(256) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) ENGINE=MyISAM DEFAULT CHARSET " + charset);
        }
        createTable(dbm, state, "lb-materials", "(id INT UNSIGNED NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (id)) ENGINE=MyISAM DEFAULT CHARSET " + charset);
        createTable(dbm, state, "lb-blockstates", "(id INT UNSIGNED NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY (id)) ENGINE=MyISAM DEFAULT CHARSET " + charset);
        
        for (final WorldConfig wcfg : getLoggedWorlds()) {
            createTable(dbm, state, wcfg.table + "-blocks", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid INT UNSIGNED NOT NULL, replaced SMALLINT UNSIGNED NOT NULL, replacedData SMALLINT NOT NULL, type SMALLINT UNSIGNED NOT NULL, typeData SMALLINT NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT UNSIGNED NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
            createTable(dbm, state, wcfg.table + "-sign", "(id INT UNSIGNED NOT NULL, signtext VARCHAR(255) NOT NULL, PRIMARY KEY (id)) DEFAULT CHARSET " + charset);
            createTable(dbm, state, wcfg.table + "-chestdata", "(id INT UNSIGNED NOT NULL, item MEDIUMBLOB, itemremove TINYINT, PRIMARY KEY (id))");
            if (wcfg.isLogging(Logging.KILL)) {
                createTable(dbm, state, wcfg.table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer INT UNSIGNED, victim INT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id))");
            }
        }
        state.close();
        conn.close();
    }

    private static void createTable(DatabaseMetaData dbm, Statement state, String table, String query) throws SQLException {
        if (!dbm.getTables(null, null, table, null).next()) {
            getLogger().log(Level.INFO, "Creating table " + table + ".");
            state.execute("CREATE TABLE `" + table + "` " + query);
            if (!dbm.getTables(null, null, table, null).next()) {
                throw new SQLException("Table " + table + " not found and failed to create");
            }
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
                logblock.getLogger().log(Level.SEVERE, "[Updater] Error: ", ex);
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
