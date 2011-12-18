package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.readURL;
import static org.bukkit.Bukkit.getLogger;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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
			if (logblock.getLBConfig().isLogging(Logging.CHAT)) {
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
				wcfg.set("logging.BLOCKPLACE", wcfg.getBoolean("logBlockCreations"));
				wcfg.set("logging.BLOCKBREAK", wcfg.getBoolean("logBlockDestroyings"));
				wcfg.set("logging.SIGNTEXT", wcfg.getBoolean("logSignTexts"));
				wcfg.set("logging.FIRE", wcfg.getBoolean("logFire"));
				wcfg.set("logging.LEAVESDECAY", wcfg.getBoolean("logLeavesDecay"));
				wcfg.set("logging.LAVAFLOW", wcfg.getBoolean("logLavaFlow"));
				wcfg.set("logging.WATERFLOW", wcfg.getBoolean("logWaterFlow"));
				wcfg.set("logging.CHESTACCESS", wcfg.getBoolean("logChestAccess"));
				wcfg.set("logging.SWITCHINTERACT", wcfg.getBoolean("logButtonsAndLevers"));
				wcfg.set("logging.KILL", wcfg.getBoolean("logKills"));
				wcfg.set("logging.CHAT", wcfg.getBoolean("logChat"));
				wcfg.set("logging.SNOWFORM", wcfg.getBoolean("logSnowForm"));
				wcfg.set("logging.SNOWFADE", wcfg.getBoolean("logSnowFade"));
				wcfg.set("logging.DOORINTERACT", wcfg.getBoolean("logDoors"));
				wcfg.set("logging.CAKEEAT", wcfg.getBoolean("logCakes"));
				wcfg.set("logging.ENDERMEN", wcfg.getBoolean("logEndermen"));
				wcfg.set("logging.TNTEXPLOSION", wcfg.getBoolean("logExplosions"));
				wcfg.set("logging.MISCEXPLOSION", wcfg.getBoolean("logExplosions"));
				wcfg.set("logging.CREEPEREXPLOSION", wcfg.getBoolean("logExplosions"));
				wcfg.set("logging.GHASTFIREBALLEXPLOSION", wcfg.getBoolean("logExplosions"));
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
		if (config.getString("version").compareTo("1.51" /* FIXME: Needs correct version number */) < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.51 ...");//FIXME: Needs correct version number
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				for (final WorldConfig wcfg : logblock.getLBConfig().worlds.values()) {
					if (wcfg.isLogging(Logging.KILL))
					{
						st.execute("ALTER TABLE `" + wcfg.table + "-kills` ADD (x SMALLINT NOT NULL DEFAULT 0, y TINYINT UNSIGNED NOT NULL DEFAULT 0, z SMALLINT NOT NULL DEFAULT 0)");
					}
				}
				st.close();
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.51" /* FIXME: Needs correct version number */);
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
		createTable(dbm, state, "lb-players", "(playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, firstlogin DATETIME NOT NULL, lastlogin DATETIME NOT NULL, onlinetime TIME NOT NULL, ip varchar(255) NOT NULL, PRIMARY KEY (playerid), UNIQUE (playername))");
		if (logblock.getLBConfig().isLogging(Logging.CHAT))
			createTable(dbm, state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, message VARCHAR(255) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) ENGINE=MyISAM");
		for (final WorldConfig wcfg : logblock.getLBConfig().worlds.values()) {
			createTable(dbm, state, wcfg.table, "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
			createTable(dbm, state, wcfg.table + "-sign", "(id INT UNSIGNED NOT NULL, signtext VARCHAR(255) NOT NULL, PRIMARY KEY (id))");
			createTable(dbm, state, wcfg.table + "-chest", "(id INT UNSIGNED NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
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

	String checkVersion() {
		try {
			return readURL(new URL("http://diddiz.insane-architects.net/lbuptodate.php?v=" + logblock.getDescription().getVersion()));
		} catch (final Exception ex) {
			return "Can't check version";
		}
	}
}
