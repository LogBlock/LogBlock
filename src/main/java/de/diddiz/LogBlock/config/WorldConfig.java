package de.diddiz.LogBlock.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.configuration.file.YamlConfiguration;
import de.diddiz.LogBlock.Logging;

public class WorldConfig extends LoggingEnabledMapping
{
	public final String table;

	public WorldConfig(File file) throws IOException {
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("table", "lb_" + file.getName().substring(0, file.getName().length() - 4).replace(' ', '_'));
		for (final Logging l : Logging.values())
			def.put("logging." + l.toString(), l.isDefaultEnabled());
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (final Entry<String, Object> e : def.entrySet())
			if (config.get(e.getKey()) == null)
				config.set(e.getKey(), e.getValue());
		config.save(file);
		table = config.getString("table");
		for (final Logging l : Logging.values())
			setLogging(l, config.getBoolean("logging." + l.toString()));
	}
}
