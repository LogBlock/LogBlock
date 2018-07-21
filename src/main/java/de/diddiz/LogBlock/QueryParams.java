package de.diddiz.LogBlock;

import de.diddiz.util.Utils;
import de.diddiz.worldedit.RegionContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.*;

public final class QueryParams implements Cloneable {
    private static final Set<Integer> keywords = new HashSet<Integer>(Arrays.asList("player".hashCode(), "area".hashCode(), "selection".hashCode(), "sel".hashCode(), "block".hashCode(), "type".hashCode(), "sum".hashCode(), "destroyed".hashCode(), "created".hashCode(), "chestaccess".hashCode(), "all".hashCode(), "time".hashCode(), "since".hashCode(), "before".hashCode(), "limit".hashCode(), "world".hashCode(), "asc".hashCode(), "desc".hashCode(), "last".hashCode(), "coords".hashCode(), "silent".hashCode(), "chat".hashCode(), "search".hashCode(), "match".hashCode(), "loc".hashCode(), "location".hashCode(), "kills".hashCode(), "killer".hashCode(), "victim".hashCode(), "both".hashCode()));
    public BlockChangeType bct = BlockChangeType.BOTH;
    public int limit = -1, before = 0, since = 0, radius = -1;
    public Location loc = null;
    public Order order = Order.DESC;
    public List<String> players = new ArrayList<String>();
    public List<String> killers = new ArrayList<String>();
    public List<String> victims = new ArrayList<String>();
    public boolean excludePlayersMode = false, excludeKillersMode = false, excludeVictimsMode = false, excludeBlocksMode = false, prepareToolQuery = false, silent = false;
    public RegionContainer sel = null;
    public SummarizationMode sum = SummarizationMode.NONE;
    public List<Material> types = new ArrayList<Material>();
    public List<Integer> typeIds = new ArrayList<Integer>();
    public World world = null;
    public String match = null;
    public boolean needCount = false, needId = false, needDate = false, needType = false, needData = false, needPlayer = false, needCoords = false, needSignText = false, needChestAccess = false, needMessage = false, needKiller = false, needVictim = false, needWeapon = false;
    private final LogBlock logblock;

    public QueryParams(LogBlock logblock) {
        this.logblock = logblock;
    }

    public QueryParams(LogBlock logblock, CommandSender sender, List<String> args) throws IllegalArgumentException {
        this.logblock = logblock;
        parseArgs(sender, args);
    }

    public static boolean isKeyWord(String param) {
        return keywords.contains(param.toLowerCase().hashCode());
    }

    public String getLimit() {
        return limit > 0 ? "LIMIT " + limit : "";
    }

    public String getQuery() {
        if (bct == BlockChangeType.CHAT) {
            String select = "SELECT ";
            if (needCount) {
                select += "COUNT(*) AS count";
            } else {
                if (needId) {
                    select += "id, ";
                }
                if (needDate) {
                    select += "date, ";
                }
                if (needPlayer) {
                    select += "playername, UUID,";
                }
                if (needMessage) {
                    select += "message, ";
                }
                select = select.substring(0, select.length() - 2);
            }
            String from = "FROM `lb-chat` ";

            if (needPlayer || players.size() > 0) {
                from += "INNER JOIN `lb-players` USING (playerid) ";
            }
            return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
        }
        if (bct == BlockChangeType.KILLS) {
            if (sum == SummarizationMode.NONE) {
                String select = "SELECT ";
                if (needCount) {
                    select += "COUNT(*) AS count";
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
                    if (needWeapon) {
                        select += "weapon, ";
                    }
                    if (needCoords) {
                        select += "x, y, z, ";
                    }
                    select = select.substring(0, select.length() - 2);
                }
                String from = "FROM `" + getTable() + "-kills` ";

                if (needPlayer || needKiller || killers.size() > 0) {
                    from += "INNER JOIN `lb-players` as killers ON (killer=killers.playerid) ";
                }

                if (needPlayer || needVictim || victims.size() > 0) {
                    from += "INNER JOIN `lb-players` as victims ON (victim=victims.playerid) ";
                }

                return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
            } else if (sum == SummarizationMode.PLAYERS) {
                return "SELECT playername, UUID, SUM(kills) AS kills, SUM(killed) AS killed FROM ((SELECT killer AS playerid, count(*) AS kills, 0 as killed FROM `" + getTable() + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY killer) UNION (SELECT victim AS playerid, 0 as kills, count(*) AS killed FROM `" + getTable() + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY victim)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(kills) + SUM(killed) " + order + " " + getLimit();
            }
        }
        if (sum == SummarizationMode.NONE) {
            String select = "SELECT ";
            if (needCount) {
                select += "COUNT(*) AS count";
            } else {
                if (needId) {
                    select += "`" + getTable() + "`-blocks.id, ";
                }
                if (needDate) {
                    select += "date, ";
                }
                if (needType) {
                    select += "replaced, type, ";
                }
                if (needData) {
                    select += "replacedData, typeData, ";
                }
                if (needPlayer) {
                    select += "playername, UUID, ";
                }
                if (needCoords) {
                    select += "x, y, z, ";
                }
                if (needSignText) {
                    select += "signtext, ";
                }
                if (needChestAccess) {
                    select += "item, itemremove, ";
                }
                select = select.substring(0, select.length() - 2);
            }
            String from = "FROM `" + getTable() + "-blocks` ";
            if (needPlayer || players.size() > 0) {
                from += "INNER JOIN `lb-players` USING (playerid) ";
            }
            if (needSignText) {
                from += "LEFT JOIN `" + getTable() + "-sign` USING (id) ";
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
            return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
        } else if (sum == SummarizationMode.TYPES) {
            return "SELECT type, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT type, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-blocks` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.CREATED) + "GROUP BY type) UNION (SELECT replaced AS type, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-blocks` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.DESTROYED) + "GROUP BY replaced)) AS t GROUP BY type ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
        } else {
            return "SELECT playername, UUID, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "-blocks` " + getWhere(BlockChangeType.CREATED) + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "-blocks` " + getWhere(BlockChangeType.DESTROYED) + "GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
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
        } else {
            if (!types.isEmpty()) {
                if (excludeBlocksMode) {
                    title.append("all blocks except ");
                }
                final String[] blocknames = new String[types.size()];
                for (int i = 0; i < types.size(); i++) {
                    blocknames[i] = types.get(i).name();
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
            title.append("more than ").append(before * -1).append(" minutes ago ");
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
            title.append("summed up by ").append(sum == SummarizationMode.TYPES ? "blocks" : "players").append(" ");
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
                    where.append("MATCH (message) AGAINST ('").append(match).append("' IN BOOLEAN MODE) AND ");
                } else {
                    where.append("message ").append(unlike ? "NOT " : "").append("LIKE '%").append(unlike ? match.substring(1) : match).append("%' AND ");
                }
            }
        } else if (blockChangeType == BlockChangeType.KILLS) {
            if (!players.isEmpty()) {
                if (!excludePlayersMode) {
                    where.append('(');
                    for (final String killerName : players) {
                        where.append("killers.playername = '").append(killerName).append("' OR ");
                    }
                    for (final String victimName : players) {
                        where.append("victims.playername = '").append(victimName).append("' OR ");
                    }
                    where.delete(where.length() - 4, where.length());
                    where.append(") AND ");
                } else {
                    for (final String killerName : players) {
                        where.append("killers.playername != '").append(killerName).append("' AND ");
                    }
                    for (final String victimName : players) {
                        where.append("victims.playername != '").append(victimName).append("' AND ");
                    }
                }
            }

            if (!killers.isEmpty()) {
                if (!excludeKillersMode) {
                    where.append('(');
                    for (final String killerName : killers) {
                        where.append("killers.playername = '").append(killerName).append("' OR ");
                    }
                    where.delete(where.length() - 4, where.length());
                    where.append(") AND ");
                } else {
                    for (final String killerName : killers) {
                        where.append("killers.playername != '").append(killerName).append("' AND ");
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

            if (loc != null) {
                if (radius == 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX(), loc.getBlockX(),
                            loc.getBlockY(), loc.getBlockY(),
                            loc.getBlockZ(), loc.getBlockZ()
                    );
                } else if (radius > 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX() - radius + 1, loc.getBlockX() + radius - 1,
                            loc.getBlockY() - radius + 1, loc.getBlockY() + radius - 1,
                            loc.getBlockZ() - radius + 1, loc.getBlockZ() + radius - 1
                    );
                }

            } else if (sel != null) {
                compileLocationQuery(
                        where,
                        sel.getSelection().getMinimumPoint().getBlockX(), sel.getSelection().getMaximumPoint().getBlockX(),
                        sel.getSelection().getMinimumPoint().getBlockY(), sel.getSelection().getMaximumPoint().getBlockY(),
                        sel.getSelection().getMinimumPoint().getBlockZ(), sel.getSelection().getMaximumPoint().getBlockZ()
                );
            }

        } else {
            switch (blockChangeType) {
                case ALL:
                    if (!typeIds.isEmpty()) {
                        if (excludeBlocksMode) {
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
                        if (excludeBlocksMode) {
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
                        if (excludeBlocksMode) {
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
                        if (excludeBlocksMode) {
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
                        if (excludeBlocksMode) {
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
            if (loc != null) {
                if (radius == 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX(), loc.getBlockX(),
                            loc.getBlockY(), loc.getBlockY(),
                            loc.getBlockZ(), loc.getBlockZ()
                    );
                } else if (radius > 0) {
                    compileLocationQuery(
                            where,
                            loc.getBlockX() - radius + 1, loc.getBlockX() + radius - 1,
                            loc.getBlockY() - radius + 1, loc.getBlockY() + radius - 1,
                            loc.getBlockZ() - radius + 1, loc.getBlockZ() + radius - 1
                    );
                }

            } else if (sel != null) {
                compileLocationQuery(
                        where,
                        sel.getSelection().getMinimumPoint().getBlockX(), sel.getSelection().getMaximumPoint().getBlockX(),
                        sel.getSelection().getMinimumPoint().getBlockY(), sel.getSelection().getMaximumPoint().getBlockY(),
                        sel.getSelection().getMinimumPoint().getBlockZ(), sel.getSelection().getMaximumPoint().getBlockZ()
                );
            }

        }
        if (!players.isEmpty() && sum != SummarizationMode.PLAYERS && blockChangeType != BlockChangeType.KILLS) {
            if (!excludePlayersMode) {
                where.append('(');
                for (final String playerName : players) {
                    where.append("playername = '").append(playerName).append("' OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(") AND ");
            } else {
                for (final String playerName : players) {
                    where.append("playername != '").append(playerName).append("' AND ");
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
            final String[] values = getValues(args, i + 1);
            if (param.equals("last")) {
                if (session.lastQuery == null) {
                    throw new IllegalArgumentException("This is your first command, you can't use last.");
                }
                merge(session.lastQuery);
            } else if (param.equals("player")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String playerName : values) {
                    if (playerName.length() > 0) {
                        if (playerName.contains("!")) {
                            excludePlayersMode = true;
                        }
                        if (playerName.contains("\"")) {
                            players.add(playerName.replaceAll("[^a-zA-Z0-9_]", ""));
                        } else {
                            final List<Player> matches = logblock.getServer().matchPlayer(playerName);
                            if (matches.size() > 1) {
                                throw new IllegalArgumentException("Ambiguous playername '" + param + "'");
                            }
                            players.add(matches.size() == 1 ? matches.get(0).getName() : playerName.replaceAll("[^a-zA-Z0-9_]", ""));
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
                    Material mat = Material.matchMaterial(weaponName);
                    if (mat == null) {
                        throw new IllegalArgumentException("No material matching: '" + weaponName + "'");
                    }
                    types.add(mat);
                    typeIds.add(MaterialConverter.getOrAddMaterialId(mat.getKey()));
                }
                needWeapon = true;
            } else if (param.equals("block") || param.equals("type")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (String blockName : values) {
                    if (blockName.startsWith("!")) {
                        excludeBlocksMode = true;
                        blockName = blockName.substring(1);
                    }

                    final Material mat = Material.matchMaterial(blockName);
                    if (mat == null) {
                        throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                    }
                    types.add(mat);
                    typeIds.add(MaterialConverter.getOrAddMaterialId(mat.getKey()));
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
                    throw new IllegalArgumentException("You have to ba a player to use selection");
                }
                final Plugin we = player.getServer().getPluginManager().getPlugin("WorldEdit");
                if (we != null) {
                    setSelection(RegionContainer.fromPlayerSelection(player, we));
                } else {
                    throw new IllegalArgumentException("WorldEdit not found!");
                }
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
                } else if (values[0].startsWith("b")) {
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
            } else if (param.equals("search") || param.equals("match")) {
                if (values.length == 0) {
                    throw new IllegalArgumentException("No arguments for '" + param + "'");
                }
                match = mysqlTextEscape(join(values, " "));
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
        if (bct == BlockChangeType.KILLS) {
            if (world == null) {
                throw new IllegalArgumentException("No world specified");
            }
            if (!getWorldConfig(world).isLogging(Logging.KILL)) {
                throw new IllegalArgumentException("Kill logging not enabled for world '" + world.getName() + "'");
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
        if (session != null) {
            session.lastQuery = clone();
        }
    }

    public void setLocation(Location loc) {
        this.loc = loc;
        world = loc.getWorld();
    }

    public void setSelection(RegionContainer container) {
        this.sel = container;
        world = sel.getSelection().getWorld();
    }

    public void setPlayer(String playerName) {
        players.clear();
        players.add(playerName);
    }

    @Override
    protected QueryParams clone() {
        try {
            final QueryParams params = (QueryParams) super.clone();
            params.players = new ArrayList<String>(players);
            params.typeIds = new ArrayList<Integer>(typeIds);
            params.types = new ArrayList<Material>(types);
            return params;
        } catch (final CloneNotSupportedException ex) {
        }
        return null;
    }

    private static String[] getValues(List<String> args, int offset) {
        // The variable i will store the last value's index
        int i;
        // Iterate over the all the values from the offset up till the end
        for (i = offset; i < args.size(); i++) {
            // We found a keyword, break here since anything after this isn't a value.
            if (isKeyWord(args.get(i))) {
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
        players = p.players;
        excludePlayersMode = p.excludePlayersMode;
        typeIds = p.typeIds;
        types = p.types;
        loc = p.loc;
        radius = p.radius;
        sel = p.sel;
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
        ALL, BOTH, CHESTACCESS, CREATED, DESTROYED, CHAT, KILLS
    }

    public static enum Order {
        ASC, DESC
    }

    public static enum SummarizationMode {
        NONE, PLAYERS, TYPES
    }
}
