package de.diddiz.LogBlock.config;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.util.BukkitUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

public class WorldConfig extends LoggingEnabledMapping {
    public final String world;
    public final String table;
    public final String insertBlockStatementString;
    public final String selectBlockActorIdStatementString;
    public final String insertBlockStateStatementString;
    public final String insertBlockChestDataStatementString;
    public final String insertEntityStatementString;
    public final String updateEntityUUIDString;

    private final EnumMap<EntityLogging, EntityLoggingList> entityLogging = new EnumMap<>(EntityLogging.class);
    public final boolean logNaturalEntitySpawns;
    public final boolean logAllNamedEntityKills;

    public WorldConfig(String world, File file) throws IOException {
        this.world = world;
        final Map<String, Object> def = new HashMap<>();
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
        for (EntityLogging el : EntityLogging.values()) {
            if (!(config.get("entity." + el.name().toLowerCase()) instanceof List)) {
                config.set("entity." + el.name().toLowerCase(), el.getDefaultEnabled());
            }
            entityLogging.put(el, new EntityLoggingList(el, config.getStringList("entity." + el.name().toLowerCase())));
        }
        if (!config.isBoolean("entity.logNaturalSpawns")) {
            config.set("entity.logNaturalSpawns", false);
        }
        logNaturalEntitySpawns = config.getBoolean("entity.logNaturalSpawns");

        if (!config.isBoolean("entity.logAllNamedEntityKills")) {
            config.set("entity.logAllNamedEntityKills", true);
        }
        logAllNamedEntityKills = config.getBoolean("entity.logAllNamedEntityKills");

        config.save(file);
        table = config.getString("table");
        for (final Logging l : Logging.values()) {
            setLogging(l, config.getBoolean("logging." + l.toString()));
        }

        insertBlockStatementString = "INSERT INTO `" + table + "-blocks` (date, playerid, replaced, replaceddata, type, typedata, x, y, z) VALUES (FROM_UNIXTIME(?), ?, ?, ?, ?, ?, ?, ?, ?)";
        selectBlockActorIdStatementString = "SELECT playerid FROM `" + table + "-blocks` WHERE x = ? AND y = ? AND z = ? ORDER BY date DESC LIMIT 1";
        insertBlockStateStatementString = "INSERT INTO `" + table + "-state` (replacedState, typeState, id) VALUES(?, ?, ?)";
        insertBlockChestDataStatementString = "INSERT INTO `" + table + "-chestdata` (item, itemremove, id, itemtype) values (?, ?, ?, ?)";
        insertEntityStatementString = "INSERT INTO `" + table + "-entities` (date, playerid, entityid, entitytypeid, x, y, z, action, data) VALUES (FROM_UNIXTIME(?), ?, ?, ?, ?, ?, ?, ?, ?)";
        updateEntityUUIDString = "UPDATE `" + table + "-entityids` SET entityuuid = ? WHERE entityid = ?";
    }

    public boolean isLogging(EntityLogging logging, Entity entity) {
        return entityLogging.get(logging).isLogging(entity);
    }

    public boolean isLoggingAnyEntities() {
        for (EntityLoggingList list : entityLogging.values()) {
            if (list.isLoggingAnyEntities()) {
                return true;
            }
        }
        return false;
    }

    private class EntityLoggingList {
        private final EntityLogging entityAction;
        private final HashSet<EntityType> logged = new HashSet<>();
        private final boolean logAll;
        private final boolean logAnimals;
        private final boolean logWateranimals;
        private final boolean logMonsters;
        private final boolean logLiving;

        public EntityLoggingList(EntityLogging entityAction, List<String> types) {
            this.entityAction = entityAction;
            boolean all = false;
            boolean animals = false;
            boolean wateranimals = false;
            boolean monsters = false;
            boolean living = false;
            for (String type : types) {
                EntityType et = BukkitUtils.matchEntityType(type);
                if (et != null) {
                    logged.add(et);
                } else {
                    if (type.equalsIgnoreCase("all")) {
                        all = true;
                    } else if (type.equalsIgnoreCase("animal") || type.equalsIgnoreCase("animals")) {
                        animals = true;
                    } else if (type.equalsIgnoreCase("wateranimal") || type.equalsIgnoreCase("wateranimals")) {
                        wateranimals = true;
                    } else if (type.equalsIgnoreCase("monster") || type.equalsIgnoreCase("monsters")) {
                        monsters = true;
                    } else if (type.equalsIgnoreCase("living")) {
                        living = true;
                    } else {
                        LogBlock.getInstance().getLogger().log(Level.WARNING, "Unkown entity type in config for " + world + ": " + type);
                    }
                }
            }
            logAll = all;
            logAnimals = animals;
            logWateranimals = wateranimals;
            logMonsters = monsters;
            logLiving = living;
        }

        public boolean isLogging(Entity entity) {
            if (entity == null || (entity instanceof Player)) {
                return false;
            }
            EntityType type = entity.getType();
            if (logAll || logged.contains(type)) {
                return true;
            }
            if (logLiving && LivingEntity.class.isAssignableFrom(entity.getClass()) && !(entity instanceof ArmorStand)) {
                return true;
            }
            if (logAnimals && Animals.class.isAssignableFrom(entity.getClass())) {
                return true;
            }
            if (logWateranimals && WaterMob.class.isAssignableFrom(entity.getClass())) {
                return true;
            }
            if (logMonsters && (Monster.class.isAssignableFrom(entity.getClass()) || entity.getType() == EntityType.SLIME || entity.getType() == EntityType.WITHER || entity.getType() == EntityType.ENDER_DRAGON || entity.getType() == EntityType.SHULKER || entity.getType() == EntityType.GHAST)) {
                return true;
            }
            if (entityAction == EntityLogging.DESTROY && logAllNamedEntityKills && entity.getCustomName() != null) {
                return true;
            }
            return false;
        }

        public boolean isLoggingAnyEntities() {
            return logAll || logAnimals || logLiving || logMonsters || !logged.isEmpty() || (entityAction == EntityLogging.DESTROY && logAllNamedEntityKills);
        }
    }
}
