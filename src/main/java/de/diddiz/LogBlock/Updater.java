package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.config.Config.getLoggedWorlds;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static org.bukkit.Bukkit.getLogger;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import de.diddiz.LogBlock.config.WorldConfig;

class Updater
{
	private final LogBlock logblock;

	Updater(LogBlock logblock) {
		this.logblock = logblock;
	}

	boolean update() {
		final ConfigurationSection config = logblock.getConfig();
		if (config.getString("version").compareTo(logblock.getDescription().getVersion()) >= 0)
			return false;
		if (config.getString("version").compareTo("1.27") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.27 ...");
			if (isLogging(Logging.CHAT)) {
				final Connection conn = logblock.getConnection();
				try {
					conn.setAutoCommit(true);
					final Statement st = conn.createStatement();
					st.execute("ALTER TABLE `lb-chat` ENGINE = MyISAM, ADD FULLTEXT message (message)");
					st.close();
					conn.close();
				} catch (final SQLException ex) {
					Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
					return false;
				}
			}
			config.set("version", "1.27");
		}
		if (config.getString("version").compareTo("1.30") < 0) {
			getLogger().info("[LogBlock] Updating config to 1.30 ...");
			for (final String tool : config.getConfigurationSection("tools").getKeys(false))
				if (config.get("tools." + tool + ".permissionDefault") == null)
					config.set("tools." + tool + ".permissionDefault", "OP");
			config.set("version", "1.30");
		}
		if (config.getString("version").compareTo("1.31") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.31 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				st.execute("ALTER TABLE `lb-players` ADD COLUMN lastlogin DATETIME NOT NULL, ADD COLUMN onlinetime TIME NOT NULL, ADD COLUMN ip VARCHAR(255) NOT NULL");
				st.close();
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.31");
		}
		if (config.getString("version").compareTo("1.32") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.32 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				st.execute("ALTER TABLE `lb-players` ADD COLUMN firstlogin DATETIME NOT NULL AFTER playername");
				st.close();
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.32");
		}
		if (config.getString("version").compareTo("1.40") < 0) {
			getLogger().info("[LogBlock] Updating config to 1.40 ...");
			config.set("clearlog.keepLogDays", null);
			config.set("version", "1.40");
		}
		if (config.getString("version").compareTo("1.42") < 0) {
			getLogger().info("[LogBlock] Updating config to 1.42 ...");
			for (final String world : config.getStringList("loggedWorlds")) {
				final File file = new File(logblock.getDataFolder(), friendlyWorldname(world) + ".yml");
				final YamlConfiguration wcfg = YamlConfiguration.loadConfiguration(file);
				if (wcfg.contains("logBlockCreations"))
					wcfg.set("logging.BLOCKPLACE", wcfg.getBoolean("logBlockCreations"));
				if (wcfg.contains("logBlockDestroyings"))
					wcfg.set("logging.BLOCKBREAK", wcfg.getBoolean("logBlockDestroyings"));
				if (wcfg.contains("logSignTexts"))
					wcfg.set("logging.SIGNTEXT", wcfg.getBoolean("logSignTexts"));
				if (wcfg.contains("logFire"))
					wcfg.set("logging.FIRE", wcfg.getBoolean("logFire"));
				if (wcfg.contains("logLeavesDecay"))
					wcfg.set("logging.LEAVESDECAY", wcfg.getBoolean("logLeavesDecay"));
				if (wcfg.contains("logLavaFlow"))
					wcfg.set("logging.LAVAFLOW", wcfg.getBoolean("logLavaFlow"));
				if (wcfg.contains("logWaterFlow"))
					wcfg.set("logging.WATERFLOW", wcfg.getBoolean("logWaterFlow"));
				if (wcfg.contains("logChestAccess"))
					wcfg.set("logging.CHESTACCESS", wcfg.getBoolean("logChestAccess"));
				if (wcfg.contains("logButtonsAndLevers"))
					wcfg.set("logging.SWITCHINTERACT", wcfg.getBoolean("logButtonsAndLevers"));
				if (wcfg.contains("logKills"))
					wcfg.set("logging.KILL", wcfg.getBoolean("logKills"));
				if (wcfg.contains("logChat"))
					wcfg.set("logging.CHAT", wcfg.getBoolean("logChat"));
				if (wcfg.contains("logSnowForm"))
					wcfg.set("logging.SNOWFORM", wcfg.getBoolean("logSnowForm"));
				if (wcfg.contains("logSnowFade"))
					wcfg.set("logging.SNOWFADE", wcfg.getBoolean("logSnowFade"));
				if (wcfg.contains("logDoors"))
					wcfg.set("logging.DOORINTERACT", wcfg.getBoolean("logDoors"));
				if (wcfg.contains("logCakes"))
					wcfg.set("logging.CAKEEAT", wcfg.getBoolean("logCakes"));
				if (wcfg.contains("logEndermen"))
					wcfg.set("logging.ENDERMEN", wcfg.getBoolean("logEndermen"));
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
					Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				}
			}
			config.set("clearlog.keepLogDays", null);
			config.set("version", "1.42");
		}
		if (config.getString("version").compareTo("1.51") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.51 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				for (final WorldConfig wcfg : getLoggedWorlds())
					if (wcfg.isLogging(Logging.KILL))
						st.execute("ALTER TABLE `" + wcfg.table + "-kills` ADD (x SMALLINT NOT NULL DEFAULT 0, y TINYINT UNSIGNED NOT NULL DEFAULT 0, z SMALLINT NOT NULL DEFAULT 0)");
				st.close();
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.51");
		}
		if (config.getString("version").compareTo("1.52") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.52 ...");
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
				} else
					getLogger().info("[LogBlock] Column lb-players was alredy modified, skipping it.");
				st.close();
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.52");
		}
		logblock.saveConfig();
		return true;
	}

	void checkTables() throws SQLException {
		final Connection conn = logblock.getConnection();
		if (conn == null)
			throw new SQLException("No connection");
		final Statement state = conn.createStatement();
		final DatabaseMetaData dbm = conn.getMetaData();
		conn.setAutoCommit(true);
		createTable(dbm, state, "lb-players", "(playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, firstlogin DATETIME NOT NULL, lastlogin DATETIME NOT NULL, onlinetime INT UNSIGNED NOT NULL, ip varchar(255) NOT NULL, PRIMARY KEY (playerid), UNIQUE (playername))");
		if (isLogging(Logging.CHAT))
			createTable(dbm, state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, message VARCHAR(255) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) ENGINE=MyISAM");
		for (final WorldConfig wcfg : getLoggedWorlds()) {
			createTable(dbm, state, wcfg.table, "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT UNSIGNED NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
			createTable(dbm, state, wcfg.table + "-sign", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, signtext VARCHAR(255) NOT NULL, PRIMARY KEY (id))");
			createTable(dbm, state, wcfg.table + "-chest", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
			if (wcfg.isLogging(Logging.KILL))
				createTable(dbm, state, wcfg.table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id))");
		}
		state.close();
		conn.close();
	}

	private static void createTable(DatabaseMetaData dbm, Statement state, String table, String query) throws SQLException {
		if (!dbm.getTables(null, null, table, null).next()) {
			getLogger().log(Level.INFO, "[LogBlock] Creating table " + table + ".");
			state.execute("CREATE TABLE `" + table + "` " + query);
			if (!dbm.getTables(null, null, table, null).next())
				throw new SQLException("Table " + table + " not found and failed to create");
		}
	}
}
