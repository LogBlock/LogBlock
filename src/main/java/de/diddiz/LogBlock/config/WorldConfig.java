package de.diddiz.LogBlock.config;

import de.diddiz.LogBlock.Logging;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WorldConfig extends LoggingEnabledMapping {
    public final String world;
    public final String table;

    public WorldConfig(String world, File file) throws IOException {
        this.world = world;
        final Map<String, Object> def = new HashMap<String, Object>();
        // "Before MySQL 5.1.6, database and table names cannot contain "/", "\", ".", or characters that are not permitted in file names" - MySQL manual
        // They _can_ contain spaces, but replace them as well
        def.put("table", "lb-" + file.getName().substring(0, file.getName().length() - 4).replaceAll("[ ./\\\\]", "_"));
        for (final Logging l : Logging.values()) {
            def.put("logging." + l.toString(), l.isDefaultEnabled());
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (final Entry<String, Object> e : def.entrySet()) {
            if (config.get(e.getKey()) == null) {
                config.set(e.getKey(), e.getValue());
            }
        }
        config.save(file);
        table = config.getString("table");
        for (final Logging l : Logging.values()) {
            setLogging(l, config.getBoolean("logging." + l.toString()));
        }
    }
}
