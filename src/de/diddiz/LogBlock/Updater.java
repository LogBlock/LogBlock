package de.diddiz.LogBlock;

import static de.diddiz.util.Utils.readURL;
import static org.bukkit.Bukkit.getLogger;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

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
			if (logblock.getLBConfig().logChat) {
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
		if (config.getString("version").compareTo("1.33") < 0) {
			getLogger().info("[LogBlock] Updating tables to 1.33 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				for (final WorldConfig wcfg : logblock.getLBConfig().worlds.values()) {
					final Statement st = conn.createStatement();
					st.execute("ALTER TABLE `" + wcfg.table + "` CHANGE `x` `x` INT(10) NOT NULL, CHANGE `y` `y` INT(10) UNSIGNED NOT NULL, CHANGE `z` `z` INT(10) NOT NULL;");
					st.close();
				}
				conn.close();
			} catch (final SQLException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "[LogBlock Updater] Error: ", ex);
				return false;
			}
			config.set("version", "1.33");
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
		if (logblock.getLBConfig().logChat)
			createTable(dbm, state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, message VARCHAR(255) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) ENGINE=MyISAM");
		for (final WorldConfig wcfg : logblock.getLBConfig().worlds.values()) {
			createTable(dbm, state, wcfg.table, "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x INT NOT NULL, y TINYINT UNSIGNED NOT NULL, z INT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
			createTable(dbm, state, wcfg.table + "-sign", "(id INT UNSIGNED NOT NULL, signtext VARCHAR(255) NOT NULL, PRIMARY KEY (id))");
			createTable(dbm, state, wcfg.table + "-chest", "(id INT UNSIGNED NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
			if (wcfg.logKills)
				createTable(dbm, state, wcfg.table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
		}
		state.close();
		conn.close();
	}

	private void createTable(DatabaseMetaData dbm, Statement state, String table, String query) throws SQLException {
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
