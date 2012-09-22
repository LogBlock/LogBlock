package de.diddiz.LogBlock;

import static de.diddiz.util.Utils.newline;
import static org.bukkit.Bukkit.getLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import de.diddiz.util.Utils.ExtensionFilenameFilter;

public class DumpedLogImporter implements Runnable
{
	private final LogBlock logblock;

	DumpedLogImporter(LogBlock logblock) {
		this.logblock = logblock;
	}

	@Override
	public void run() {
		final File[] imports = new File("plugins/LogBlock/import/").listFiles(new ExtensionFilenameFilter("sql"));
		if (imports != null && imports.length > 0) {
			getLogger().info("Found " + imports.length + " imports.");
			Connection conn = null;
			try {
				conn = logblock.getConnection();
				if (conn == null)
					return;
				conn.setAutoCommit(false);
				final Statement st = conn.createStatement();
				final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logblock.getDataFolder(), "import/failed.txt")));
				int successes = 0, errors = 0;
				for (final File sqlFile : imports) {
					getLogger().info("Trying to import " + sqlFile.getName() + " ...");
					final BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
					String line = null;
					while ((line = reader.readLine()) != null)
						try {
							st.execute(line);
							successes++;
						} catch (final Exception ex) {
							getLogger().warning("Error while importing: '" + line + "': " + ex.getMessage());
							writer.write(line + newline);
							errors++;
						}
					conn.commit();
					reader.close();
					sqlFile.delete();
					getLogger().info("Successfully imported " + sqlFile.getName() + ".");
				}
				writer.close();
				st.close();
				getLogger().info("Successfully imported stored queue. (" + successes + " rows imported, " + errors + " errors)");
			} catch (final Exception ex) {
				getLogger().log(Level.WARNING, "Error while importing: ", ex);
			} finally {
				if (conn != null)
					try {
						conn.close();
					} catch (final SQLException ex) {
					}
			}
		}
	}
}
