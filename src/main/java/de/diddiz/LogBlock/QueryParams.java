package de.diddiz.LogBlock;

import de.diddiz.LogBlock.config.Config;
import de.diddiz.util.BukkitUtils;
import de.diddiz.util.CuboidRegion;
import de.diddiz.util.SqlUtil;
import de.diddiz.util.Utils;
import de.diddiz.worldedit.WorldEditHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.*;

public final class QueryParams implements Cloneable {
    private static final HashMap<String, Integer> keywords = new HashMap<>();
    static {
        keywords.put("player", 1);
        keywords.put("area", 0);
        keywords.put("selection", 0);
        keywords.put("sel", 0);
        keywords.put("block", 1);
        keywords.put("type", 1);
        keywords.put("sum", 1);
        keywords.put("destroyed", 0);
        keywords.put("created", 0);
        keywords.put("chestaccess", 0);
        keywords.put("all", 0);
        keywords.put("time", 0);
        keywords.put("since", 0);
        keywords.put("before", 0);
        keywords.put("limit", 1);
        keywords.put("world", 1);
        keywords.put("asc", 0);
        keywords.put("desc", 0);
        keywords.put("last", 0);
        keywords.put("coords", 0);
        keywords.put("silent", 0);
        keywords.put("chat", 0);
        keywords.put("search", 1);
        keywords.put("match", 1);
        keywords.put("loc", 1);
        keywords.put("location", 1);
        keywords.put("kills", 0);
        keywords.put("killer", 1);
        keywords.put("victim", 1);
        keywords.put("both", 0);
        keywords.put("force", 0);
        keywords.put("nocache", 0);
        keywords.put("entities", 0);
        keywords.put("entity", 0);
    }
    public BlockChangeType bct = BlockChangeType.BOTH;
    public int limit = -1, before = 0, since = 0, radius = -1;
    public Location loc = null;
    public Order order = Order.DESC;
    public List<String> players = new ArrayList<>();
    public List<String> killers = new ArrayList<>();
    public List<String> victims = new ArrayList<>();
    public boolean excludePlayersMode = false, excludeKillersMode = false, excludeVictimsMode = false, excludeBlocksEntitiesMode = false, prepareToolQuery = false, silent = false, noForcedLimit = false;
    public boolean forceReplace = false, noCache = false;
    public CuboidRegion sel = null;
    public SummarizationMode sum = SummarizationMode.NONE;
    public List<Material> types = new ArrayList<>();
    public List<Tag<Material>> typeTags = new ArrayList<>();
    public List<EntityType> entityTypes = new ArrayList<>();
    public World world = null;
    public String match = null;
    public boolean needCount = false, needId = false, needDate = false, needType = false, needData = false, needPlayerId = false, needPlayer = false, needCoords = false, needChestAccess = false, needMessage = false, needKiller = false, needVictim = false, needWeapon = false;
    private final LogBlock logblock;

    public QueryParams(LogBlock logblock) {
        this.logblock = logblock;
    }

    public QueryParams(LogBlock logblock, CommandSender sender, List<String> args) throws IllegalArgumentException {
        this.logblock = logblock;
        parseArgs(sender, args);
    }

    public static boolean isKeyWord(String param) {
        return keywords.containsKey(param.toLowerCase());
    }

    public static int getKeyWordMinArguments(String param) {
        Integer minArgs = keywords.get(param.toLowerCase());
        return minArgs == null ? 0 : minArgs;
    }

    public String getLimit() {
        if (noForcedLimit || Config.hardLinesLimit <= 0 || (limit > 0 && limit <= Config.hardLinesLimit)) {
            return limit > 0 ? "LIMIT " + limit : "";
        }
        return "LIMIT " + Config.hardLinesLimit;
    }

    public String getFrom() {
        if (sum != SummarizationMode.NONE) {
            throw new IllegalStateException("Not implemented for summarization");
        }
        if (bct == BlockChangeType.CHAT) {
            String from = "FROM `lb-chat` ";

            if (needPlayer || players.size() > 0) {
                from += "INNER JOIN `lb-players` USING (playerid) ";
            }
            return from;
        }
        if (bct == BlockChangeType.KILLS) {
            String from = "FROM `" + getTable() + "-kills` ";

            if (needPlayer || needKiller || killers.size() > 0) {
                from += "INNER JOIN `lb-players` as killers ON (killer=killers.playerid) ";
            }

            if (needPlayer || needVictim || victims.size() > 0) {
                from += "INNER JOIN `lb-players` as victims ON (victim=victims.playerid) ";
            }
            return from;
        }
        if (bct == BlockChangeType.ENTITIES || bct == BlockChangeType.ENTITIES_CREATED || bct == BlockChangeType.ENTITIES_KILLED) {
            String from = "FROM `" + getTable() + "-entities` ";

            if (needPlayer || players.size() > 0) {
                from += "INNER JOIN `lb-players` USING (playerid) ";
            }
            if (!needCount && needData) {
                from += "LEFT JOIN `" + getTable() + "-entityids` USING (entityid) ";
            }
            return from;
        }

        String from = "FROM `" + getTable() + "-blocks` ";

        if (needPlayer || players.size() > 0) {
            from += "INNER JOIN `lb-players` USING (playerid) ";
        }
        if (!needCount && needData) {
            from += "LEFT JOIN `" + getTable() + "-state` USING (id) ";
        }
        if (needChestAccess)
        // If BlockChangeType is CHESTACCESS, we can use more efficient query
        {
            if (bct == BlockChangeType.CHESTACCESS) {
                from += "RIGHT JOIN `" + getTable() + "-chestdata` USING (id) ";
            } else {
                from += "LEFT JOIN `" + getTable() + "-chestdata` USING (id) ";
            }
        }
        return from;
    }

    public String getOrder() {
        if (sum != SummarizationMode.NONE) {
            throw new IllegalStateException("Not implemented for summarization");
        }

        // heuristics, for small time spans this might be faster
        final int twoDaysInSeconds = 60 * 24 * 2;
        if (since > 0 && since <= twoDaysInSeconds) {
            return "ORDER BY date " + order + ", id " + order + " ";
        }

        return "ORDER BY id " + order + " ";
    }

    public String getFields() {
        if (sum != SummarizationMode.NONE) {
            throw new IllegalStateException("Not implemented for summarization");
        }
        if (bct == BlockChangeType.CHAT) {
            String select = "";
            if (needCount) {
                select += "COUNT(*) AS count ";
            } else {
                if (needId) {
                    select += "id, ";
                }
                if (needDate) {
                    select += "date, ";
                }
                if (needPlayer) {
                    select += "playername, UUID, ";
                }
                if (needPlayerId) {
                    select += "playerid, ";
                }
                if (needMessage) {
                    select += "message, ";
                }
                select = select.substring(0, select.length() - 2) + " ";
            }
            return select;
        }
        if (bct == BlockChangeType.KILLS) {
            String select = "";
            if (needCount) {
                select += "COUNT(*) AS count ";
            } else {
                if (needId) {
                    select += "id, ";
                }
                if (needDate) {
                    select += "date, ";
                }
                if (needPlayer || needKiller) {
                    select += "killers.playername as killer, ";
                }
                if (needPlayer || needVictim) {
                    select += "victims.playername as victim, ";
                }
                if (needPlayerId) {
                    select += "killer as killerid, victim as victimid, ";
                }
                if (needWeapon) {
                    select += "weapon, ";
                }
                if (needCoords) {
                    select += "x, y, z, ";
                }
                select = select.substring(0, select.length() - 2) + " ";
            }
            return select;
        }
        String select = "";
        if (needCount) {
            select += "COUNT(*) AS count ";
        } else {
            if (needId) {
                if (bct != BlockChangeType.ENTITIES && bct != BlockChangeType.ENTITIES_CREATED && bct != BlockChangeType.ENTITIES_KILLED) {
                    select += "`" + getTable() + "-blocks`.id, ";
                } else {
                    select += "`" + getTable() + "-entities`.id, ";
                }
            }
            if (needDate) {
                select += "date, ";
            }
            if (bct != BlockChangeType.ENTITIES && bct != BlockChangeType.ENTITIES_CREATED && bct != BlockChangeType.ENTITIES_KILLED) {
                if (needType) {
                    select += "replaced, type, ";
                }
                if (needData) {
                    select += "replacedData, typeData, ";
                }
            }
            if (needPlayer) {
                select += "playername, UUID, ";
            }
            if (needPlayerId) {
                select += "playerid, ";
            }
            if (needCoords) {
                select += "x, y, z, ";
            }
            if (bct != BlockChangeType.ENTITIES && bct != BlockChangeType.ENTITIES_CREATED && bct != BlockChangeType.ENTITIES_KILLED) {
                if (needData) {
                    select += "replacedState, typeState, ";
                }
                if (needChestAccess) {
                    select += "item, itemremove, itemtype, ";
                }
            } else {
                if (needType) {
                    select += "entitytypeid, action, ";
                }
                if (needData) {
                    select += "entityid, entityuuid, data, ";
                }
            }
            select = select.substring(0, select.length() - 2) + " ";
        }
        return select;
    }

    public String getQuery() {
        if (sum == SummarizationMode.NONE) {
            return "SELECT " + getFields() + getFrom() + getWhere() + getOrder() + getLimit();
        }
        if (bct == BlockChangeType.CHAT) {
            throw new IllegalStateException("Invalid summarization for chat");
        }
        if (bct == BlockChangeType.KILLS) {
            if (sum == SummarizationMode.PLAYERS) {
                return "SELECT playername, UUID, SUM(kills) AS kills, SUM(killed) AS killed FROM ((SELECT killer AS playerid, count(*) AS kills, 0 as killed FROM `" + getTable() + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY killer) UNION (SELECT victim AS playerid, 0 as kills, count(*) AS killed FROM `" + getTable()
                        + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY victim)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(kills) + SUM(killed) " + order + " " + getLimit();
            }
            throw new IllegalStateException("Invalid summarization for kills");
        }
        if (bct == BlockChangeType.ENTITIES || bct == BlockChangeType.ENTITIES_CREATED || bct == BlockChangeType.ENTITIES_KILLED) {
            if (sum == SummarizationMode.TYPES) {
                return "SELECT entitytypeid, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT entitytypeid, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-entities` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.ENTITIES_CREATED) + "GROUP BY entitytypeid) UNION (SELECT entitytypeid, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-entities` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.ENTITIES_KILLED)
                        + "GROUP BY entitytypeid)) AS t GROUP BY entitytypeid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
            } else {
                return "SELECT playername, UUID, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-entities` " + getWhere(BlockChangeType.ENTITIES_CREATED) + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-entities` " + getWhere(BlockChangeType.ENTITIES_KILLED)
                        + "GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
            }
        }
        if (sum == SummarizationMode.TYPES) {
            return "SELECT type, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT type, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-blocks` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.CREATED) + "GROUP BY type) UNION (SELECT replaced AS type, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-blocks` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.DESTROYED)
                    + "GROUP BY replaced)) AS t GROUP BY type ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
        } else {
            return "SELECT playername, UUID, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-blocks` " + getWhere(BlockChangeType.CREATED) + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-blocks` " + getWhere(BlockChangeType.DESTROYED)
                    + "GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
        }
    }

    public String getTable() {
        return getWorldConfig(world).table;
    }

    public String getTitle() {
        final StringBuilder title = new StringBuilder();
        if (bct == BlockChangeType.CHESTACCESS) {
            title.append("chest accesses ");
        } else if (bct == BlockChangeType.CHAT) {
            title.append("chat messages ");
        } else if (bct == BlockChangeType.KILLS) {
            title.append("kills ");
        } else if (bct == BlockChangeType.ENTITIES || bct == BlockChangeType.ENTITIES_CREATED || bct == BlockChangeType.ENTITIES_KILLED) {
            if (!entityTypes.isEmpty()) {
                if (excludeBlocksEntitiesMode) {
                    title.append("all entities except ");
                }
                final String[] entityTypeNames = new String[entityTypes.size()];
                for (int i = 0; i < entityTypes.size(); i++) {
                    entityTypeNames[i] = entityTypes.get(i).name();
                }
                title.append(listing(entityTypeNames, ", ", " and ")).append(" ");
            } else {
                title.append("entity changes ");
            }
        } else {
            if (!types.isEmpty() || !typeTags.isEmpty()) {
                if (excludeBlocksEntitiesMode) {
                    title.append("all blocks except ");
                }
                final String[] blocknames = new String[types.size() + typeTags.size()];
                for (int i = 0; i < types.size(); i++) {
                    blocknames[i] = types.get(i).name();
                }
                for (int i = 0; i < typeTags.size(); i++) {
                    blocknames[i + types.size()] = "#" + typeTags.get(i).getKey().getKey().toUpperCase();
                }
                title.append(listing(blocknames, ", ", " and ")).append(" ");
            } else {
                title.append("block ");
            }
            if (bct == BlockChangeType.CREATED) {
                title.append("creations ");
            } else if (bct == BlockChangeType.DESTROYED) {
                title.append("destructions ");
            } else {
                title.append("changes ");
            }
        }
        if (killers.size() > 10) {
            title.append(excludeKillersMode ? "without" : "from").append(" many killers ");
        } else if (!killers.isEmpty()) {
            title.append(excludeKillersMode ? "without" : "from").append(" ").append(listing(killers.toArray(new String[killers.size()]), ", ", " and ")).append(" ");
        }
        if (victims.size() > 10) {
            title.append(excludeVictimsMode ? "without" : "of").append(" many victims ");
        } else if (!victims.isEmpty()) {
            title.append(excludeVictimsMode ? "without" : "of").append(" victim").append(victims.size() != 1 ? "s" : "").append(" ").append(listing(victims.toArray(new String[victims.size()]), ", ", " and ")).append(" ");
        }
        if (players.size() > 10) {
            title.append(excludePlayersMode ? "without" : "from").append(" many players ");
        } else if (!players.isEmpty()) {
            title.append(excludePlayersMode ? "without" : "from").append(" player").append(players.size() != 1 ? "s" : "").append(" ").append(listing(players.toArray(new String[players.size()]), ", ", " and ")).append(" ");
        }
        if (match != null && match.length() > 0) {
            title.append("matching '").append(match).append("' ");
        }
        if (before > 0 && since > 0) {
            title.append("between ").append(since).append(" and ").append(before).append(" minutes ago ");
        } else if (since > 0) {
            title.append("in the last ").append(since).append(" minutes ");
        } else if (before > 0) {
            title.append("more than ").append(before).append(" minutes ago ");
        }
        if (loc != null) {
            if (radius > 0) {
                title.append("within ").append(radius).append(" blocks of ").append(prepareToolQuery ? "clicked block" : "location").append(" ");
            } else if (radius == 0) {
                title.append("at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ()).append(" ");
            }
        } else if (sel != null) {
            title.append(prepareToolQuery ? "at double chest " : "inside selection ");
        } else if (prepareToolQuery) {
            if (radius > 0) {
                title.append("within ").append(radius).append(" blocks of clicked block ");
            } else if (radius == 0) {
                title.append("at clicked block ");
            }
        }
        if (world != null && !(sel != null && prepareToolQuery)) {
            title.append("in ").append(friendlyWorldname(world.getName())).append(" ");
        }
        if (sum != SummarizationMode.NONE) {
            title.append("summed up by ").append(sum == SummarizationMode.TYPES ? ((bct == BlockChangeType.ENTITIES || bct == BlockChangeType.ENTITIES_CREATED || bct == BlockChangeType.ENTITIES_KILLED) ? "entities" : "blocks") : "players").append(" ");
        }
        title.deleteCharAt(title.length() - 1);
        title.setCharAt(0, String.valueOf(title.charAt(0)).toUpperCase().toCharArray()[0]);
        return title.toString();
    }

    public String getWhere() {
        return getWhere(bct);
    }

    public String getWhere(BlockChangeType blockChangeType) {
        final StringBuilder where = new StringBuilder("WHERE ");
        if (blockChangeType == BlockChangeType.CHAT) {
            if (match != null && match.length() > 0) {
                final boolean unlike = match.startsWith("-");
                if (match.length() > 3 && !unlike || match.length() > 4) {
                    where.append("MATCH (message) AGAINST ('").append(SqlUtil.escapeString(match)).append("' IN BOOLEAN MODE) AND ");
                } else {
                    where.append("message ").append(unlike ? "NOT " : "").append("LIKE '%").append(SqlUtil.escapeString(unlike ? match.substring(1) : match, true)).append("%' AND ");
                }
            }
        } else if (blockChangeType == BlockChangeType.KILLS) {
            if (!players.isEmpty()) {
                if (!excludePlayersMode) {
                    where.append('(');
                    for (final String killerName : players) {
                        where.append("killers.playername = '").append(SqlUtil.escapeString(killerName, true)).append("' OR ");
                    }
                    for (final String victimName : players) {
                        where.append("victims.playername = '").append(SqlUtil.escapeString(victimName, true)).append("' OR ");
                    }
                    where.delete(where.length() - 4, where.length());
                    where.append(") AND ");
                } else {
                    for (final String killerName : players) {
                        where.append("killers.playername != '").append(SqlUtil.escapeString(killerName, true)).append("' AND ");
                    }
                    for (final String victimName : players) {
                        where.append("victims.playername != '").append(SqlUtil.escapeString(victimName, true)).append("' AND ");
                    }
                }
            }

            if (!killers.isEmpty()) {
                if (!excludeKillersMode) {
                    where.append('(');
                    for (final String killerName : killers) {
                        where.append("killers.playername = '").append(SqlUtil.escapeString(killerName)).append("' OR ");
                    }
                    where.delete(where.length() - 4, where.length());
                    where.append(") AND ");
                } else {
                    for (final String killerName : killers) {
                        where.append("killers.playername != '").append(SqlUtil.escapeString(killerName)).append("' AND ");
                    }
                }
            }

            if (!victims.isEmpty()) {
                if (!excludeVictimsMode) {
                    where.append('(');
                    for (final String victimName : victims) {
                        where.append("victims.playername = '").append(victimName).append("' OR ");
                    }
                    where.delete(where.length() - 4, where.length());
                    where.append(") AND ");
                } else {
                    for (final String victimName : victims) {
                        where.append("victims.playername != '").append(victimName).append("' AND ");
                    }
                }
            }
        } else if (blockChangeType == BlockChangeType.ENTITIES || blockChangeType == BlockChangeType.ENTITIES_CREATED || blockChangeType == BlockChangeType.ENTITIES_KILLED) {
            Set<Integer> entityTypeIds = getEntityTypeIds();
            if (!entityTypeIds.isEmpty()) {
                if (excludeBlocksEntitiesMode) {
                    where.append("NOT ");
                }
                where.append('(');
                for (final Integer entityType : entityTypeIds) {
                    where.append("(entitytypeid = ").append(entityType);
                    where.append(") OR ");
                }
                where.delete(where.length() - 4, where.length() - 1);
                where.append(") AND ");
            }
            if (blockChangeType == BlockChangeType.ENTITIES_CREATED) {
                where.append("action = " + EntityChange.EntityChangeType.CREATE.ordinal() + " AND ");
            } else if (blockChangeType == BlockChangeType.ENTITIES_KILLED) {
                where.append("action = " + EntityChange.EntityChangeType.KILL.ordinal() + " AND ");
            }
        } else {
            Set<Integer> typeIds = getTypeIds();
            switch (blockChangeType) {
                case ALL:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksEntitiesMode) {
                            where.append("NOT ");
                        }
                        where.append('(');
                        for (final Integer block : typeIds) {
                            where.append("((type = ").append(block).append(" OR replaced = ").append(block);
                            where.append(")");
                            where.append(") OR ");
                        }
                        where.delete(where.length() - 4, where.length() - 1);
                        where.append(") AND ");
                    }
                    break;
                case BOTH:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksEntitiesMode) {
                            where.append("NOT ");
                        }
                        where.append('(');
                        for (final Integer block : typeIds) {
                            where.append("((type = ").append(block).append(" OR replaced = ").append(block);
                            where.append(")");
                            where.append(") OR ");
                        }
                        where.delete(where.length() - 4, where.length());
                        where.append(") AND ");
                    }
                    where.append("type != replaced AND ");
                    break;
                case CREATED:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksEntitiesMode) {
                            where.append("NOT ");
                        }
                        where.append('(');
                        for (final Integer block : typeIds) {
                            where.append("((type = ").append(block);
                            where.append(")");
                            where.append(") OR ");
                        }
                        where.delete(where.length() - 4, where.length());
                        where.append(") AND ");
                    }
                    where.append("type != 0 AND type != replaced AND ");
                    break;
                case DESTROYED:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksEntitiesMode) {
                            where.append("NOT ");
                        }
                        where.append('(');
                        for (final Integer block : typeIds) {
                            where.append("((replaced = ").append(block);
                            where.append(")");
                            where.append(") OR ");
                        }
                        where.delete(where.length() - 4, where.length());
                        where.append(") AND ");
                    }
                    where.append("replaced != 0 AND type != replaced AND ");
                    break;
                case CHESTACCESS:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksEntitiesMode) {
                            where.append("NOT ");
                        }
                        where.append('(');
                        for (final Integer block : typeIds) {
                            where.append("((itemtype = ").append(block);
                            where.append(")");
                            where.append(") OR ");
                        }
                        where.delete(where.length() - 4, where.length());
                        where.append(") AND ");
                    }
                    break;
                default:
                    break;
            }
        }
        if (blockChangeType != BlockChangeType.CHAT) {
            if (loc != null) {
                if (radius == 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX(), loc.getBlockX(),
                            loc.getBlockY(), loc.getBlockY(),
                            loc.getBlockZ(), loc.getBlockZ());
                } else if (radius > 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX() - radius + 1, loc.getBlockX() + radius - 1,
                            loc.getBlockY() - radius + 1, loc.getBlockY() + radius - 1,
                            loc.getBlockZ() - radius + 1, loc.getBlockZ() + radius - 1);
                }

            } else if (sel != null) {
                compileLocationQuery(
                        where,
                        sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX(),
                        sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY(),
                        sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ());
            }
        }
        if (!players.isEmpty() && sum != SummarizationMode.PLAYERS && blockChangeType != BlockChangeType.KILLS) {
            if (!excludePlayersMode) {
                where.append('(');
                for (final String playerName : players) {
                    where.append("playername = '").append(SqlUtil.escapeString(playerName, true)).append("' OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(") AND ");
            } else {
                for (final String playerName : players) {
                    where.append("playername != '").append(SqlUtil.escapeString(playerName, true)).append("' AND ");
                }
            }
        }
        if (since > 0) {
            where.append("date > date_sub(now(), INTERVAL ").append(since).append(" MINUTE) AND ");
        }
        if (before > 0) {
            where.append("date < date_sub(now(), INTERVAL ").append(before).append(" MINUTE) AND ");
        }
        if (where.length() > 6) {
            where.delete(where.length() - 4, where.length());
        } else {
            where.delete(0, where.length());
        }
        return where.toString();
    }

    private void compileLocationQuery(StringBuilder where, int blockX, int blockX2, int blockY, int blockY2, int blockZ, int blockZ2) {
        compileLocationQueryPart(where, "x", blockX, blockX2);
        where.append(" AND ");
        compileLocationQueryPart(where, "y", blockY, blockY2);
        where.append(" AND ");
        compileLocationQueryPart(where, "z", blockZ, blockZ2);
        where.append(" AND ");
    }

    private void compileLocationQueryPart(StringBuilder where, String locValue, int loc, int loc2) {
        int min = Math.min(loc, loc2);
        int max = Math.max(loc2, loc);

        if (min == max) {
            where.append(locValue).append(" = ").append(min);
        } else if (max - min > 50) {
            where.append(locValue).append(" >= ").append(min).append(" AND ").append(locValue).append(" <= ").append(max);
        } else {
            where.append(locValue).append(" in (");
            for (int c = min; c < max; c++) {
                where.append(c).append(",");
            }
            where.append(max);
            where.append(")");
        }
    }

    public void parseArgs(CommandSender sender, List<String> args) throws IllegalArgumentException {
        parseArgs(sender, args, true);
    }

    public void parseArgs(CommandSender sender, List<String> args, boolean validate) throws IllegalArgumentException {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("No parameters specified.");
        }
        args = Utils.parseQuotes(args);
        final Player player = sender instanceof Player ? (Player) sender : null;
        final Session session = prepareToolQuery ? null : getSession(sender);
        if (player != null && world == null) {
            world = player.getWorld();
        }
        for (int i = 0; i < args.size(); i++) {
            final String param = args.get(i).toLowerCase();
            final String[] values = getValues(args, i + 1, getKeyWordMinArguments(param));
            if (param.equals("last")) {
                if (session == null || session.lastQuery == null) {
                    throw new IllegalArgumentException("This is your first command, you can't use last.");
                }
                merge(session.lastQuery);
            } else if (param.equals("player")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (String playerName : values) {
                    if (playerName.length() > 0) {
                        if (playerName.startsWith("!")) {
                            playerName = playerName.substring(1);
                            excludePlayersMode = true;
                            if (playerName.isEmpty()) {
                                continue;
                            }
                        }
                        if (playerName.contains("\"")) {
                            players.add(playerName.replace("\"", ""));
                        } else {
                            final Player matches = logblock.getServer().getPlayerExact(playerName);
                            players.add(matches != null ? matches.getName() : playerName.replace("\\\"", ""));
                        }
                    }
                }
                needPlayer = true;
            } else if (param.equals("killer")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String killerName : values) {
                    if (killerName.length() > 0) {
                        if (killerName.contains("!")) {
                            excludeVictimsMode = true;
                        }
                        if (killerName.contains("\"")) {
                            killers.add(killerName.replaceAll("[^a-zA-Z0-9_]", ""));
                        } else {
                            final List<Player> matches = logblock.getServer().matchPlayer(killerName);
                            if (matches.size() > 1) {
                                throw new IllegalArgumentException("Ambiguous victimname '" + param + "'");
                            }
                            killers.add(matches.size() == 1 ? matches.get(0).getName() : killerName.replaceAll("[^a-zA-Z0-9_]", ""));
                        }
                    }
                }
                needKiller = true;
            } else if (param.equals("victim")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String victimName : values) {
                    if (victimName.length() > 0) {
                        if (victimName.contains("!")) {
                            excludeVictimsMode = true;
                        }
                        if (victimName.contains("\"")) {
                            victims.add(victimName.replaceAll("[^a-zA-Z0-9_]", ""));
                        } else {
                            final List<Player> matches = logblock.getServer().matchPlayer(victimName);
                            if (matches.size() > 1) {
                                throw new IllegalArgumentException("Ambiguous victimname '" + param + "'");
                            }
                            victims.add(matches.size() == 1 ? matches.get(0).getName() : victimName.replaceAll("[^a-zA-Z0-9_]", ""));
                        }
                    }
                }
                needVictim = true;
            } else if (param.equals("weapon")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String weaponName : values) {
                    Material mat = weaponName.equalsIgnoreCase("fist") ? Material.AIR : Material.matchMaterial(weaponName);
                    if (mat == null) {
                        throw new IllegalArgumentException("No material matching: '" + weaponName + "'");
                    }
                    types.add(mat);
                }
                needWeapon = true;
            } else if (param.equals("block") || param.equals("type")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (String blockName : values) {
                    if (blockName.startsWith("!")) {
                        excludeBlocksEntitiesMode = true;
                        blockName = blockName.substring(1);
                    }
                    if (blockName.startsWith("#")) {
                        String tagName = blockName.substring(1).toLowerCase();
                        Tag<Material> tag = logblock.getServer().getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
                        if (tag == null || tag.getValues().isEmpty()) {
                            tag = logblock.getServer().getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(tagName), Material.class);
                            if (tag == null || tag.getValues().isEmpty()) {
                                throw new IllegalArgumentException("No block tag matching: '" + blockName + "'");
                            }
                        }
                        typeTags.add(tag);
                    } else if (blockName.contains("*")) {
                        StringBuilder sb = new StringBuilder();
                        for (int ci = 0; ci < blockName.length(); ci++) {
                            char c = blockName.charAt(ci);
                            if (!Character.isAlphabetic(c)) {
                                if (c == '*') {
                                    sb.append('.');
                                } else {
                                    sb.append('\\');
                                }
                            }
                            sb.append(c);
                        }
                        String blockNamePattern = sb.toString();
                        Pattern pattern = Pattern.compile(blockNamePattern, Pattern.CASE_INSENSITIVE);
                        ArrayList<Material> matched = new ArrayList<>();
                        for (Material mat : Material.values()) {
                            if (pattern.matcher(mat.name()).matches()) {
                                matched.add(mat);
                            }
                        }
                        if (matched.isEmpty()) {
                            throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                        }
                        for (Material mat : matched) {
                            types.add(mat);
                        }
                    } else {
                        final Material mat = Material.matchMaterial(blockName);
                        if (mat == null) {
                            throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                        }
                        types.add(mat);
                    }
                }
            } else if (param.equals("area")) {
                if (player == null && !prepareToolQuery && loc == null) {
                    throw new IllegalArgumentException("You have to be a player to use area, or specify a location first");
                }
                if (values.length == 0) {
                    radius = defaultDist;
                    if (!prepareToolQuery && loc == null) {
                        loc = player.getLocation();
                    }
                } else {
                    if (!isInt(values[0])) {
                        throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
                    }
                    radius = Integer.parseInt(values[0]);
                    if (!prepareToolQuery && loc == null) {
                        loc = player.getLocation();
                    }
                }
            } else if (param.equals("selection") || param.equals("sel")) {
                if (player == null) {
                    throw new IllegalArgumentException("You have to be a player to use selection");
                }
                setSelection(WorldEditHelper.getSelectedRegion(player));
            } else if (param.equals("time") || param.equals("since")) {
                since = values.length > 0 ? parseTimeSpec(values) : defaultTime;
                if (since == -1) {
                    throw new IllegalArgumentException("Failed to parse time spec for '" + param + "'");
                }
            } else if (param.equals("before")) {
                before = values.length > 0 ? parseTimeSpec(values) : defaultTime;
                if (before == -1) {
                    throw new IllegalArgumentException("Faile to parse time spec for '" + param + "'");
                }
            } else if (param.equals("sum")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                if (values[0].startsWith("p")) {
                    sum = SummarizationMode.PLAYERS;
                } else if (values[0].startsWith("b") || values[0].startsWith("e")) {
                    sum = SummarizationMode.TYPES;
                } else if (values[0].startsWith("n")) {
                    sum = SummarizationMode.NONE;
                } else {
                    throw new IllegalArgumentException("Wrong summarization mode");
                }
            } else if (param.equals("created")) {
                bct = BlockChangeType.CREATED;
            } else if (param.equals("destroyed")) {
                bct = BlockChangeType.DESTROYED;
            } else if (param.equals("both")) {
                bct = BlockChangeType.BOTH;
            } else if (param.equals("chestaccess")) {
                bct = BlockChangeType.CHESTACCESS;
            } else if (param.equals("chat")) {
                bct = BlockChangeType.CHAT;
            } else if (param.equals("kills")) {
                bct = BlockChangeType.KILLS;
            } else if (param.equals("entities") || param.equals("entity")) {
                bct = BlockChangeType.ENTITIES;
                if (values.length > 0) {
                    for (String entityTypeName : values) {
                        if (entityTypeName.startsWith("!")) {
                            excludeBlocksEntitiesMode = true;
                            entityTypeName = entityTypeName.substring(1);
                        }

                        final EntityType entityType = BukkitUtils.matchEntityType(entityTypeName);
                        if (entityType == null) {
                            throw new IllegalArgumentException("No entity type matching: '" + entityTypeName + "'");
                        }
                        entityTypes.add(entityType);
                    }
                }
            } else if (param.equals("all")) {
                bct = BlockChangeType.ALL;
            } else if (param.equals("limit")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
                }
                if (!isInt(values[0])) {
                    throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
                }
                limit = Integer.parseInt(values[0]);
            } else if (param.equals("world")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
                }
                final World w = sender.getServer().getWorld(values[0].replace("\"", ""));
                if (w == null) {
                    throw new IllegalArgumentException("There is no world called '" + values[0] + "'");
                }
                world = w;
            } else if (param.equals("asc")) {
                order = Order.ASC;
            } else if (param.equals("desc")) {
                order = Order.DESC;
            } else if (param.equals("coords")) {
                needCoords = true;
            } else if (param.equals("silent")) {
                silent = true;
            } else if (param.equals("force")) {
                forceReplace = true;
            } else if (param.equals("nocache")) {
                noCache = true;
            } else if (param.equals("search") || param.equals("match")) {
                if (values.length == 0) {
                    throw new IllegalArgumentException("No arguments for '" + param + "'");
                }
                match = join(values, " ");
            } else if (param.equals("loc") || param.equals("location")) {
                final String[] vectors = values.length == 1 ? values[0].split(":") : values;
                if (vectors.length != 3) {
                    throw new IllegalArgumentException("Wrong count arguments for '" + param + "'");
                }
                for (final String vec : vectors) {
                    if (!isInt(vec)) {
                        throw new IllegalArgumentException("Not a number: '" + vec + "'");
                    }
                }
                loc = new Location(null, Integer.valueOf(vectors[0]), Integer.valueOf(vectors[1]), Integer.valueOf(vectors[2]));
                radius = 0;
            } else {
                throw new IllegalArgumentException("Not a valid argument: '" + param + "'");
            }
            i += values.length;
        }
        if (validate) {
            validate();
        }
        if (session != null && !noCache) {
            session.lastQuery = clone();
        }
    }

    public void validate() {
        if (bct == BlockChangeType.KILLS) {
            if (world == null) {
                throw new IllegalArgumentException("No world specified");
            }
            if (!getWorldConfig(world).isLogging(Logging.KILL)) {
                throw new IllegalArgumentException("Kill logging not enabled for world '" + world.getName() + "'");
            }
            if (sum != SummarizationMode.NONE && sum != SummarizationMode.PLAYERS) {
                throw new IllegalArgumentException("Invalid summarization for kills");
            }
        }
        if (!prepareToolQuery && bct != BlockChangeType.CHAT) {
            if (world == null) {
                throw new IllegalArgumentException("No world specified");
            }
            if (!isLogged(world)) {
                throw new IllegalArgumentException("This world ('" + world.getName() + "') isn't logged");
            }
        }
        if (bct == BlockChangeType.CHAT) {
            if (!(isLogging(Logging.CHAT) || isLogging(Logging.PLAYER_COMMANDS) || isLogging(Logging.CONSOLE_COMMANDS) || isLogging(Logging.COMMANDBLOCK_COMMANDS))) {
                throw new IllegalArgumentException("Chat is not logged");
            }
            if (sum != SummarizationMode.NONE) {
                throw new IllegalArgumentException("Invalid summarization for chat");
            }
        }
    }

    public void setLocation(Location loc) {
        this.loc = loc;
        this.sel = null;
        world = loc.getWorld();
    }

    public void setSelection(CuboidRegion container) {
        this.sel = container;
        this.loc = null;
        world = sel.getWorld();
    }

    public void setPlayer(String playerName) {
        players.clear();
        players.add(playerName);
    }

    @Override
    public QueryParams clone() {
        try {
            final QueryParams params = (QueryParams) super.clone();
            params.players = new ArrayList<>(players);
            params.killers = new ArrayList<>(killers);
            params.victims = new ArrayList<>(victims);
            params.types = new ArrayList<>(types);
            params.typeTags = new ArrayList<>(typeTags);
            params.entityTypes = new ArrayList<>(entityTypes);
            params.loc = loc == null ? null : loc.clone();
            params.sel = sel == null ? null : sel.clone();
            return params;
        } catch (final CloneNotSupportedException ex) {
            throw new Error("QueryParams should be cloneable", ex);
        }
    }

    private Set<Integer> getTypeIds() {
        HashSet<Integer> typeIds = new HashSet<>();
        for (Material type : types) {
            if (type != null) {
                Integer id = MaterialConverter.getExistingMaterialId(type);
                if (id != null) {
                    typeIds.add(id);
                }
            }
        }
        for (Tag<Material> tag : typeTags) {
            if (tag != null) {
                for (Material type : tag.getValues()) {
                    Integer id = MaterialConverter.getExistingMaterialId(type);
                    if (id != null) {
                        typeIds.add(id);
                    }
                }
            }
        }
        // add invalid id, so the type list is not ignored
        if ((!types.isEmpty() || !typeTags.isEmpty()) && typeIds.isEmpty()) {
            typeIds.add(-1);
        }
        return typeIds;
    }

    private Set<Integer> getEntityTypeIds() {
        HashSet<Integer> typeIds = new HashSet<>();
        for (EntityType type : entityTypes) {
            Integer id = EntityTypeConverter.getExistingEntityTypeId(type);
            if (id != null) {
                typeIds.add(id);
            }
        }
        // add invalid id, so the type list is not ignored
        if (!entityTypes.isEmpty() && typeIds.isEmpty()) {
            typeIds.add(-1);
        }
        return typeIds;
    }

    private static String[] getValues(List<String> args, int offset, int minParams) {
        // The variable i will store the last value's index
        int i;
        // Iterate over the all the values from the offset up till the end
        for (i = offset; i < args.size(); i++) {
            // We found a keyword, break here since anything after this isn't a value.
            if (i >= offset + minParams && isKeyWord(args.get(i))) {
                break;
            }
        }
        // If there are no values, i.e there is a keyword immediately after the offset
        // return an empty string array
        if (i == offset) {
            return new String[0];
        }

        final String[] values = new String[i - offset];
        for (int j = offset; j < i; j++) {
            String value = args.get(j);

            // If the value is encapsulated in quotes, strip them
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            values[j - offset] = value;
        }
        return values;
    }

    public void merge(QueryParams p) {
        players.addAll(p.players);
        killers.addAll(p.killers);
        victims.addAll(p.victims);
        excludePlayersMode = p.excludePlayersMode;
        types.addAll(p.types);
        typeTags.addAll(p.typeTags);
        entityTypes.addAll(p.entityTypes);
        loc = p.loc == null ? null : p.loc.clone();
        radius = p.radius;
        sel = p.sel == null ? null : p.sel.clone();
        if (p.since != 0 || since != defaultTime) {
            since = p.since;
        }
        before = p.before;
        sum = p.sum;
        bct = p.bct;
        limit = p.limit;
        world = p.world;
        order = p.order;
        match = p.match;
    }

    public static enum BlockChangeType {
        ALL,
        BOTH,
        CHESTACCESS,
        CREATED,
        DESTROYED,
        CHAT,
        KILLS,
        ENTITIES,
        ENTITIES_CREATED,
        ENTITIES_KILLED,
    }

    public static enum Order {
        ASC,
        DESC
    }

    public static enum SummarizationMode {
        NONE,
        PLAYERS,
        TYPES
    }
}
