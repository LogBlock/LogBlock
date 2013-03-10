package de.diddiz.LogBlock;

import de.diddiz.util.Block;
import de.diddiz.worldedit.RegionContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.BukkitUtils.getBlockEquivalents;
import static de.diddiz.util.MaterialName.materialName;
import static de.diddiz.util.Utils.*;

public final class QueryParams implements Cloneable
{
	private static final Set<Integer> keywords = new HashSet<Integer>(Arrays.asList("player".hashCode(), "area".hashCode(), "selection".hashCode(), "sel".hashCode(), "block".hashCode(), "type".hashCode(), "sum".hashCode(), "destroyed".hashCode(), "created".hashCode(), "chestaccess".hashCode(), "all".hashCode(), "time".hashCode(), "since".hashCode(), "before".hashCode(), "limit".hashCode(), "world".hashCode(), "asc".hashCode(), "desc".hashCode(), "last".hashCode(), "coords".hashCode(), "silent".hashCode(), "chat".hashCode(), "search".hashCode(), "match".hashCode(), "loc".hashCode(), "location".hashCode(), "kills".hashCode(), "killer".hashCode(), "victim".hashCode()));
	public BlockChangeType bct = BlockChangeType.BOTH;
	public int limit = -1, before = 0, since = 0, radius = -1;
	public Location loc = null;
	public Order order = Order.DESC;
	public List<String> players = new ArrayList<String>();
	public List<String> killers = new ArrayList<String>();
	public List<String> victims = new ArrayList<String>();
	public boolean excludePlayersMode = false, excludeKillersMode = false, excludeVictimsMode = false, prepareToolQuery = false, silent = false;
	public RegionContainer sel = null;
	public SummarizationMode sum = SummarizationMode.NONE;
	public List<Block> types = new ArrayList<Block>();
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
			if (needCount)
				select += "COUNT(*) AS count";
			else {
				if (needId)
					select += "id, ";
				if (needDate)
					select += "date, ";
				if (needPlayer)
					select += "playername, ";
				if (needMessage)
					select += "message, ";
				select = select.substring(0, select.length() - 2);
			}
			String from = "FROM `lb-chat` ";

			if (needPlayer || players.size() > 0)
				from += "INNER JOIN `lb-players` USING (playerid) ";
			return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
		}
		if (bct == BlockChangeType.KILLS) {
			if (sum == SummarizationMode.NONE) {
				String select = "SELECT ";
				if (needCount)
					select += "COUNT(*) AS count";
				else {
					if (needId)
						select += "id, ";
					if (needDate)
						select += "date, ";
					if (needPlayer || needKiller)
						select += "killers.playername as killer, ";
					if (needPlayer || needVictim)
						select += "victims.playername as victim, ";
					if (needWeapon)
						select += "weapon, ";
					if (needCoords)
						select += "x, y, z, ";
					select = select.substring(0, select.length() - 2);
				}
				String from = "FROM `" + getTable() + "-kills` ";

				if (needPlayer || needKiller || killers.size() > 0)
					from += "INNER JOIN `lb-players` as killers ON (killer=killers.playerid) ";

				if (needPlayer || needVictim || victims.size() > 0)
					from += "INNER JOIN `lb-players` as victims ON (victim=victims.playerid) ";

				return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
			} else if (sum == SummarizationMode.PLAYERS)
				return "SELECT playername, SUM(kills) AS kills, SUM(killed) AS killed FROM ((SELECT killer AS playerid, count(*) AS kills, 0 as killed FROM `" + getTable() + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY killer) UNION (SELECT victim AS playerid, 0 as kills, count(*) AS killed FROM `" + getTable() + "-kills` INNER JOIN `lb-players` as killers ON (killer=killers.playerid) INNER JOIN `lb-players` as victims ON (victim=victims.playerid) " + getWhere(BlockChangeType.KILLS) + "GROUP BY victim)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(kills) + SUM(killed) " + order + " " + getLimit();
		}
		if (sum == SummarizationMode.NONE) {
			String select = "SELECT ";
			if (needCount)
				select += "COUNT(*) AS count";
			else {
				if (needId)
					select += "`" + getTable() + "`.id, ";
				if (needDate)
					select += "date, ";
				if (needType)
					select += "replaced, type, ";
				if (needData)
					select += "data, ";
				if (needPlayer)
					select += "playername, ";
				if (needCoords)
					select += "x, y, z, ";
				if (needSignText)
					select += "signtext, ";
				if (needChestAccess)
					select += "itemtype, itemamount, itemdata, ";
				select = select.substring(0, select.length() - 2);
			}
			String from = "FROM `" + getTable() + "` ";
			if (needPlayer || players.size() > 0)
				from += "INNER JOIN `lb-players` USING (playerid) ";
			if (needSignText)
				from += "LEFT JOIN `" + getTable() + "-sign` USING (id) ";
			if (needChestAccess)
				from += "LEFT JOIN `" + getTable() + "-chest` USING (id) ";
			return select + " " + from + getWhere() + "ORDER BY date " + order + ", id " + order + " " + getLimit();
		} else if (sum == SummarizationMode.TYPES)
			return "SELECT type, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT type, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.CREATED) + "GROUP BY type) UNION (SELECT replaced AS type, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "` INNER JOIN `lb-players` USING (playerid) " + getWhere(BlockChangeType.DESTROYED) + "GROUP BY replaced)) AS t GROUP BY type ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
		else
			return "SELECT playername, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM `" + getTable() + "` " + getWhere(BlockChangeType.CREATED) + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM `" + getTable() + "` " + getWhere(BlockChangeType.DESTROYED) + "GROUP BY playerid)) AS t INNER JOIN `lb-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
	}

	public String getTable() {
		return getWorldConfig(world).table;
	}

	public String getTitle() {
		final StringBuilder title = new StringBuilder();
		if (bct == BlockChangeType.CHESTACCESS)
			title.append("chest accesses ");
		else if (bct == BlockChangeType.CHAT)
			title.append("chat messages ");
		else if (bct == BlockChangeType.KILLS)
			title.append("kills ");
		else {
			if (!types.isEmpty()) {
				final String[] blocknames = new String[types.size()];
				for (int i = 0; i < types.size(); i++)
					blocknames[i] = materialName(types.get(i).getBlock());
				title.append(listing(blocknames, ", ", " and ")).append(" ");
			} else
				title.append("block ");
			if (bct == BlockChangeType.CREATED)
				title.append("creations ");
			else if (bct == BlockChangeType.DESTROYED)
				title.append("destructions ");
			else
				title.append("changes ");
		}
		if (killers.size() > 10)
			title.append(excludeKillersMode ? "without" : "from").append(" many killers ");
		else if (!killers.isEmpty())
			title.append(excludeKillersMode ? "without" : "from").append(" ").append(listing(killers.toArray(new String[killers.size()]), ", ", " and ")).append(" ");
		if (victims.size() > 10)
			title.append(excludeVictimsMode ? "without" : "of").append(" many victims ");
		else if (!victims.isEmpty())
			title.append(excludeVictimsMode ? "without" : "of").append(" victim").append(victims.size() != 1 ? "s" : "").append(" ").append(listing(victims.toArray(new String[victims.size()]), ", ", " and ")).append(" ");
		if (players.size() > 10)
			title.append(excludePlayersMode ? "without" : "from").append(" many players ");
		else if (!players.isEmpty())
			title.append(excludePlayersMode ? "without" : "from").append(" player").append(players.size() != 1 ? "s" : "").append(" ").append(listing(players.toArray(new String[players.size()]), ", ", " and ")).append(" ");
		if (match != null && match.length() > 0)
			title.append("matching '").append(match).append("' ");
		if (before > 0 && since > 0)
			title.append("between ").append(since).append(" and ").append(before).append(" minutes ago ");
		else if (since > 0)
			title.append("in the last ").append(since).append(" minutes ");
		else if (before > 0)
			title.append("more than ").append(before * -1).append(" minutes ago ");
		if (loc != null) {
			if (radius > 0)
				title.append("within ").append(radius).append(" blocks of ").append(prepareToolQuery ? "clicked block" : "you").append(" ");
			else if (radius == 0)
				title.append("at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ()).append(" ");
		} else if (sel != null)
			title.append(prepareToolQuery ? "at double chest " : "inside selection ");
		else if (prepareToolQuery)
			if (radius > 0)
				title.append("within ").append(radius).append(" blocks of clicked block ");
			else if (radius == 0)
				title.append("at clicked block ");
		if (world != null && !(sel != null && prepareToolQuery))
			title.append("in ").append(friendlyWorldname(world.getName())).append(" ");
		if (sum != SummarizationMode.NONE)
			title.append("summed up by ").append(sum == SummarizationMode.TYPES ? "blocks" : "players").append(" ");
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
				if (match.length() > 3 && !unlike || match.length() > 4)
					where.append("MATCH (message) AGAINST ('").append(match).append("' IN BOOLEAN MODE) AND ");
				else
					where.append("message ").append(unlike ? "NOT " : "").append("LIKE '%").append(unlike ? match.substring(1) : match).append("%' AND ");
			}
		} else if (blockChangeType == BlockChangeType.KILLS) {
			if (!players.isEmpty())
				if (!excludePlayersMode) {
					where.append('(');
					for (final String killerName : players)
						where.append("killers.playername = '").append(killerName).append("' OR ");
					for (final String victimName : players)
						where.append("victims.playername = '").append(victimName).append("' OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				} else {
					for (final String killerName : players)
						where.append("killers.playername != '").append(killerName).append("' AND ");
					for (final String victimName : players)
						where.append("victims.playername != '").append(victimName).append("' AND ");
				}

			if (!killers.isEmpty())
				if (!excludeKillersMode) {
					where.append('(');
					for (final String killerName : killers)
						where.append("killers.playername = '").append(killerName).append("' OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				} else
					for (final String killerName : killers)
						where.append("killers.playername != '").append(killerName).append("' AND ");

			if (!victims.isEmpty())
				if (!excludeVictimsMode) {
					where.append('(');
					for (final String victimName : victims)
						where.append("victims.playername = '").append(victimName).append("' OR ");
					where.delete(where.length() - 4, where.length());
					where.append(") AND ");
				} else
					for (final String victimName : victims)
						where.append("victims.playername != '").append(victimName).append("' AND ");

            if (loc != null) {
                if (radius == 0)
                    compileLocationQuery(
                            where,
                            loc.getBlockX(), loc.getBlockX(),
                            loc.getBlockY(), loc.getBlockY(),
                            loc.getBlockZ(), loc.getBlockZ()
                            );

                else if (radius > 0)
                    compileLocationQuery(
                            where,
                            loc.getBlockX() - radius + 1, loc.getBlockX() + radius - 1,
                            loc.getBlockY() - radius + 1, loc.getBlockY() + radius - 1,
                            loc.getBlockZ() - radius + 1, loc.getBlockZ() + radius - 1
                            );

            } else if (sel != null)
                compileLocationQuery(
                        where,
                        sel.getSelection().getMinimumPoint().getBlockX(), sel.getSelection().getMaximumPoint().getBlockX(),
                        sel.getSelection().getMinimumPoint().getBlockY(), sel.getSelection().getMaximumPoint().getBlockY(),
                        sel.getSelection().getMinimumPoint().getBlockZ(), sel.getSelection().getMaximumPoint().getBlockZ()
                        );

		} else {
			switch (blockChangeType) {
				case ALL:
					if (!types.isEmpty()) {
						where.append('(');
						for (final Block block : types) {
							where.append("((type = ").append(block.getBlock()).append(" OR replaced = ").append(block.getBlock());
							if (block.getData() != -1) {
								where.append(") AND data = ").append(block.getData());
							} else {
								where.append(")");
							}
							where.append(") OR ");
						}
						where.delete(where.length() - 4, where.length() - 1);
						where.append(") AND ");
					}
					break;
				case BOTH:
					if (!types.isEmpty()) {
						where.append('(');
						for (final Block block : types) {
							where.append("((type = ").append(block.getBlock()).append(" OR replaced = ").append(block.getBlock());
							if (block.getData() != -1) {
								where.append(") AND data = ").append(block.getData());
							} else {
								where.append(")");
							}
							where.append(") OR ");
						}
						where.delete(where.length() - 4, where.length());
						where.append(") AND ");
					}
					where.append("type != replaced AND ");
					break;
				case CREATED:
					if (!types.isEmpty()) {
						where.append('(');
						for (final Block block : types) {
							where.append("((type = ").append(block.getBlock());
							if (block.getData() != -1) {
								where.append(") AND data = ").append(block.getData());
							} else {
								where.append(")");
							}
							where.append(") OR ");
						}
						where.delete(where.length() - 4, where.length());
						where.append(") AND ");
					} else
						where.append("type != 0 AND ");
					where.append("type != replaced AND ");
					break;
				case DESTROYED:
					if (!types.isEmpty()) {
						where.append('(');
						for (final Block block : types) {
							where.append("((replaced = ").append(block.getBlock());
							if (block.getData() != -1) {
								where.append(") AND data = ").append(block.getData());
							} else {
								where.append(")");
							}
							where.append(") OR ");
						}
						where.delete(where.length() - 4, where.length());
						where.append(") AND ");
					} else
						where.append("replaced != 0 AND ");
					where.append("type != replaced AND ");
					break;
				case CHESTACCESS:
					where.append("(type = 23 OR type = 54 OR type = 61 OR type = 62) AND type = replaced AND ");
					if (!types.isEmpty()) {
						where.append('(');
						for (final Block block : types) {
							where.append("((itemtype = ").append(block.getBlock());
							if (block.getData() != -1) {
								where.append(") AND itemdata = ").append(block.getData());
							} else {
								where.append(")");
							}
							where.append(") OR ");
						}
						where.delete(where.length() - 4, where.length());
						where.append(") AND ");
					}
					break;
			}
            if (loc != null) {
                if (radius == 0)
                    compileLocationQuery(
                            where,
                            loc.getBlockX(), loc.getBlockX(),
                            loc.getBlockY(), loc.getBlockY(),
                            loc.getBlockZ(), loc.getBlockZ()
                            );

                else if (radius > 0)
                    compileLocationQuery(
                            where,
                            loc.getBlockX() - radius + 1, loc.getBlockX() + radius - 1,
                            loc.getBlockY() - radius + 1, loc.getBlockY() + radius - 1,
                            loc.getBlockZ() - radius + 1, loc.getBlockZ() + radius - 1
                            );

            } else if (sel != null)
                compileLocationQuery(
                        where,
                        sel.getSelection().getMinimumPoint().getBlockX(), sel.getSelection().getMaximumPoint().getBlockX(),
                        sel.getSelection().getMinimumPoint().getBlockY(), sel.getSelection().getMaximumPoint().getBlockY(),
                        sel.getSelection().getMinimumPoint().getBlockZ(), sel.getSelection().getMaximumPoint().getBlockZ()
                        );

		}
		if (!players.isEmpty() && sum != SummarizationMode.PLAYERS && blockChangeType != BlockChangeType.KILLS)
			if (!excludePlayersMode) {
				where.append('(');
				for (final String playerName : players)
					where.append("playername = '").append(playerName).append("' OR ");
				where.delete(where.length() - 4, where.length());
				where.append(") AND ");
			} else
				for (final String playerName : players)
					where.append("playername != '").append(playerName).append("' AND ");
		if (since > 0)
			where.append("date > date_sub(now(), INTERVAL ").append(since).append(" MINUTE) AND ");
		if (before > 0)
			where.append("date < date_sub(now(), INTERVAL ").append(before).append(" MINUTE) AND ");
		if (where.length() > 6)
			where.delete(where.length() - 4, where.length());
		else
			where.delete(0, where.length());
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
        
        if (min == max)
            where.append(locValue).append(" = ").append(min);
        else if (max - min > 50)
            where.append(locValue).append(" >= ").append(min).append(" AND ").append(locValue).append(" <= ").append(max);
        else {
            where.append(locValue).append(" in (");
            for (int c = min; c < max; c++) {
                where.append(c).append(",");
            }
            where.append(max);
            where.append(")");
        }
    }

	public void parseArgs(CommandSender sender, List<String> args) throws IllegalArgumentException {
		if (args == null || args.isEmpty())
			throw new IllegalArgumentException("No parameters specified.");
		final Player player = sender instanceof Player ? (Player)sender : null;
		final Session session = prepareToolQuery ? null : getSession(sender);
		if (player != null && world == null)
			world = player.getWorld();
		for (int i = 0; i < args.size(); i++) {
			final String param = args.get(i).toLowerCase();
			final String[] values = getValues(args, i + 1);
			if (param.equals("last")) {
				if (session.lastQuery == null)
					throw new IllegalArgumentException("This is your first command, you can't use last.");
				merge(session.lastQuery);
			} else if (param.equals("player")) {
				if (values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String playerName : values)
					if (playerName.length() > 0) {
						if (playerName.contains("!"))
							excludePlayersMode = true;
						if (playerName.contains("\""))
							players.add(playerName.replaceAll("[^a-zA-Z0-9_]", ""));
						else {
							final List<Player> matches = logblock.getServer().matchPlayer(playerName);
							if (matches.size() > 1)
								throw new IllegalArgumentException("Ambiguous playername '" + param + "'");
							players.add(matches.size() == 1 ? matches.get(0).getName() : playerName.replaceAll("[^a-zA-Z0-9_]", ""));
						}
					}
			} else if (param.equals("killer")) {
				if (values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String killerName : values)
					if (killerName.length() > 0) {
						if (killerName.contains("!"))
							excludeVictimsMode = true;
						if (killerName.contains("\""))
							killers.add(killerName.replaceAll("[^a-zA-Z0-9_]", ""));
						else {
							final List<Player> matches = logblock.getServer().matchPlayer(killerName);
							if (matches.size() > 1)
								throw new IllegalArgumentException("Ambiguous victimname '" + param + "'");
							killers.add(matches.size() == 1 ? matches.get(0).getName() : killerName.replaceAll("[^a-zA-Z0-9_]", ""));
						}
					}
			} else if (param.equals("victim")) {
				if (values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String victimName : values)
					if (victimName.length() > 0) {
						if (victimName.contains("!"))
							excludeVictimsMode = true;
						if (victimName.contains("\""))
							victims.add(victimName.replaceAll("[^a-zA-Z0-9_]", ""));
						else {
							final List<Player> matches = logblock.getServer().matchPlayer(victimName);
							if (matches.size() > 1)
								throw new IllegalArgumentException("Ambiguous victimname '" + param + "'");
							victims.add(matches.size() == 1 ? matches.get(0).getName() : victimName.replaceAll("[^a-zA-Z0-9_]", ""));
						}
					}
			} else if (param.equals("weapon")) {
				if (values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String weaponName : values) {
					Material mat = Material.matchMaterial(weaponName);
					if (mat == null)
						try {
							mat = Material.getMaterial(Integer.parseInt(weaponName));
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("Data type not a valid number: '" + weaponName + "'");
						}
					if (mat == null)
						throw new IllegalArgumentException("No material matching: '" + weaponName + "'");
					types.add(new Block(mat.getId(), -1));
				}
			} else if (param.equals("block") || param.equals("type")) {
				if (values.length < 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				for (final String blockName : values) {
					if (blockName.contains(":")) {
						String[] blockNameSplit = blockName.split(":");
						if (blockNameSplit.length > 2)
							throw new IllegalArgumentException("No material matching: '" + blockName + "'");
						final int data;
						try {
							data = Integer.parseInt(blockNameSplit[1]);
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("Data type not a valid number: '" + blockNameSplit[1] + "'");
						}
						if (data > 255 || data < 0)
							throw new IllegalArgumentException("Data type out of range (0-255): '" + data + "'");
						final Material mat = Material.matchMaterial(blockNameSplit[0]);
						if (mat == null)
							throw new IllegalArgumentException("No material matching: '" + blockName + "'");
						types.add(new Block(mat.getId(), data));
					} else {
						final Material mat = Material.matchMaterial(blockName);
						if (mat == null)
							throw new IllegalArgumentException("No material matching: '" + blockName + "'");
						types.add(new Block(mat.getId(), -1));
					}
				}
			} else if (param.equals("area")) {
				if (player == null && !prepareToolQuery)
					throw new IllegalArgumentException("You have to ba a player to use area");
				if (values.length == 0) {
					radius = defaultDist;
					if (!prepareToolQuery)
						loc = player.getLocation();
				} else {
					if (!isInt(values[0]))
						throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
					radius = Integer.parseInt(values[0]);
					if (!prepareToolQuery)
						loc = player.getLocation();
				}
			} else if (param.equals("selection") || param.equals("sel")) {
				if (player == null)
					throw new IllegalArgumentException("You have to ba a player to use selection");
				final Plugin we = player.getServer().getPluginManager().getPlugin("WorldEdit");
				if (we != null) {
					setSelection(RegionContainer.fromPlayerSelection(player, we));
				} else {
					throw new IllegalArgumentException("WorldEdit not found!");
				}
			} else if (param.equals("time") || param.equals("since")) {
				since = values.length > 0 ? parseTimeSpec(values) : defaultTime;
				if (since == -1)
					throw new IllegalArgumentException("Failed to parse time spec for '" + param + "'");
			} else if (param.equals("before")) {
				before = values.length > 0 ? parseTimeSpec(values) : defaultTime;
				if (before == -1)
					throw new IllegalArgumentException("Faile to parse time spec for '" + param + "'");
			} else if (param.equals("sum")) {
				if (values.length != 1)
					throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
				if (values[0].startsWith("p"))
					sum = SummarizationMode.PLAYERS;
				else if (values[0].startsWith("b"))
					sum = SummarizationMode.TYPES;
				else if (values[0].startsWith("n"))
					sum = SummarizationMode.NONE;
				else
					throw new IllegalArgumentException("Wrong summarization mode");
			} else if (param.equals("created"))
				bct = BlockChangeType.CREATED;
			else if (param.equals("destroyed"))
				bct = BlockChangeType.DESTROYED;
			else if (param.equals("chestaccess"))
				bct = BlockChangeType.CHESTACCESS;
			else if (param.equals("chat"))
				bct = BlockChangeType.CHAT;
			else if (param.equals("kills")) {
				bct = BlockChangeType.KILLS;
			}
			else if (param.equals("all"))
				bct = BlockChangeType.ALL;
			else if (param.equals("limit")) {
				if (values.length != 1)
					throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
				if (!isInt(values[0]))
					throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
				limit = Integer.parseInt(values[0]);
			} else if (param.equals("world")) {
				if (values.length != 1)
					throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
				final World w = sender.getServer().getWorld(values[0].replace("\"", ""));
				if (w == null)
					throw new IllegalArgumentException("There is no world called '" + values[0] + "'");
				world = w;
			} else if (param.equals("asc"))
				order = Order.ASC;
			else if (param.equals("desc"))
				order = Order.DESC;
			else if (param.equals("coords"))
				needCoords = true;
			else if (param.equals("silent"))
				silent = true;
			else if (param.equals("search") || param.equals("match")) {
				if (values.length == 0)
					throw new IllegalArgumentException("No arguments for '" + param + "'");
				match = join(values, " ").replace("\\", "\\\\").replace("'", "\\'");
			} else if (param.equals("loc") || param.equals("location")) {
				final String[] vectors = values.length == 1 ? values[0].split(":") : values;
				if (vectors.length != 3)
					throw new IllegalArgumentException("Wrong count arguments for '" + param + "'");
				for (final String vec : vectors)
					if (!isInt(vec))
						throw new IllegalArgumentException("Not a number: '" + vec + "'");
				loc = new Location(null, Integer.valueOf(vectors[0]), Integer.valueOf(vectors[1]), Integer.valueOf(vectors[2]));
				radius = 0;
			} else
				throw new IllegalArgumentException("Not a valid argument: '" + param + "'");
			i += values.length;
		}
		if (bct == BlockChangeType.KILLS && !getWorldConfig(world).isLogging(Logging.KILL))
			throw new IllegalArgumentException("Kill logging not enabled for world '" + world.getName() + "'");
		if (types.size() > 0)
			for (final Set<Integer> equivalent : getBlockEquivalents()) {
				boolean found = false;
				for (final Block block : types)
					if (equivalent.contains(block.getBlock())) {
						found = true;
						break;
					}
				if (found)
					for (final Integer type : equivalent)
						if (!Block.inList(types, type))
							types.add(new Block(type, -1));
			}
		if (!prepareToolQuery && bct != BlockChangeType.CHAT) {
			if (world == null)
				throw new IllegalArgumentException("No world specified");
			if (!isLogged(world))
				throw new IllegalArgumentException("This world ('" + world.getName() + "') isn't logged");
		}
		if (session != null)
			session.lastQuery = clone();
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
			final QueryParams params = (QueryParams)super.clone();
			params.players = new ArrayList<String>(players);
			params.types = new ArrayList<Block>(types);
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
		// If the offset equals to the last value index, return an empty string array
		if (i == offset) {
			return new String[0];
		}
		// Instantiate a new string array with the total indexes required
		final List<String> values = new ArrayList<String>();
		// Buffer for the value
		String value = "";
		// Iterate over the offset up till the last index value
		for (int j = offset; j < i; j++) {
			// If the value starts with a double quote or we're already dealing with a quoted value
			if (args.get(j).startsWith("\"") || !value.equals("")) {
				// If the value doesn't end with a double quote
				if (!args.get(j).endsWith("\"")) {
					// Add the argument to the value buffer after stripping out the initial quote
					
					// If the argument starts with a quote we wanna strip that out otherwise add it normally
					if (args.get(j).startsWith("\"")) {
						value += args.get(j).substring(1) + " ";
					} else {
						value += args.get(j) + " ";
					}
					
				} else {
					// The value ends with a double quote
					
					// If the argument starts with a double quote we wanna strip that out too along with the end quote
					if (args.get(j).startsWith("\"")) {
						value += args.get(j).substring(0, args.get(j).length() - 1).substring(1);
					} else {
					// Looks like its just the end quote here, just need to strip that out
						value += args.get(j).substring(0, args.get(j).length() - 1);
					}
					// Add the value to the main values list
					values.add(value);
					// Reset the buffer
					value = "";
				}
			} else {
				// Set the value in the array to be returned to the one from the main arguments list
				values.add(args.get(j));
			}
		}
		// Return the values array
		return values.toArray(new String[values.size()]);
	}

	public void merge(QueryParams p) {
		players = p.players;
		excludePlayersMode = p.excludePlayersMode;
		types = p.types;
		loc = p.loc;
		radius = p.radius;
		sel = p.sel;
		if (p.since != 0 || since != defaultTime)
			since = p.since;
		before = p.before;
		sum = p.sum;
		bct = p.bct;
		limit = p.limit;
		world = p.world;
		order = p.order;
		match = p.match;
	}

	public static enum BlockChangeType
	{
		ALL, BOTH, CHESTACCESS, CREATED, DESTROYED, CHAT, KILLS
	}

	public static enum Order
	{
		ASC, DESC
	}

	public static enum SummarizationMode
	{
		NONE, PLAYERS, TYPES
	}
}
