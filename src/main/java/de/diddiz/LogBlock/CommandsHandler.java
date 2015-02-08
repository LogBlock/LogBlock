package de.diddiz.LogBlock;

import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.QueryParams.Order;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.html.MessageHelper;
import de.diddiz.LogBlockQuestioner.LogBlockQuestioner;
import de.diddiz.util.Block;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.giveTool;
import static de.diddiz.util.BukkitUtils.saveSpawnHeight;
import static de.diddiz.util.Utils.isInt;
import static de.diddiz.util.Utils.listing;
import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class CommandsHandler implements CommandExecutor
{
	private final LogBlock logblock;
	private final BukkitScheduler scheduler;
	private final LogBlockQuestioner questioner;

	CommandsHandler(LogBlock logblock) {
		this.logblock = logblock;
		scheduler = logblock.getServer().getScheduler();
		questioner = (LogBlockQuestioner)logblock.getServer().getPluginManager().getPlugin("LogBlockQuestioner");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		try {
			if (args.length == 0) {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "LogBlock v" + logblock.getDescription().getVersion() + " by DiddiZ");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Type /lb help for help");
			} else {
				final String command = args[0].toLowerCase();
				if (command.equals("help")) {
					sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Help:");
					sender.sendMessage(ChatColor.GOLD + "For the commands list type '/lb commands'");
					sender.sendMessage(ChatColor.GOLD + "For the parameters list type '/lb params'");
					sender.sendMessage(ChatColor.GOLD + "For the list of permissions you got type '/lb permissions'");
				} else if (command.equals("commands")) {
					sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Commands:");
					sender.sendMessage(ChatColor.GOLD + "/lb tool -- Gives you the lb tool");
					sender.sendMessage(ChatColor.GOLD + "/lb tool [on|off] -- Enables/Disables tool");
					sender.sendMessage(ChatColor.GOLD + "/lb tool [params] -- Sets the tool lookup query");
					sender.sendMessage(ChatColor.GOLD + "/lb tool default -- Sets the tool lookup query to default");
					sender.sendMessage(ChatColor.GOLD + "/lb toolblock -- Analog to tool");
					sender.sendMessage(ChatColor.GOLD + "/lb hide -- Hides you from log");
					sender.sendMessage(ChatColor.GOLD + "/lb rollback [params] -- Rollback");
					sender.sendMessage(ChatColor.GOLD + "/lb redo [params] -- Redo");
					sender.sendMessage(ChatColor.GOLD + "/lb tp [params] -- Teleports you to the location of griefing");
					sender.sendMessage(ChatColor.GOLD + "/lb writelogfile [params] -- Writes a log file");
					sender.sendMessage(ChatColor.GOLD + "/lb lookup [params] -- Lookup");
					sender.sendMessage(ChatColor.GOLD + "/lb prev|next -- Browse lookup result pages");
					sender.sendMessage(ChatColor.GOLD + "/lb page -- Shows a specific lookup result page");
					sender.sendMessage(ChatColor.GOLD + "/lb me -- Displays your stats");
					sender.sendMessage(ChatColor.GOLD + "Look at github.com/LogBlock/LogBlock/wiki/Commands for the full commands reference");
				} else if (command.equals("params")) {
					sender.sendMessage(ChatColor.DARK_AQUA + "LogBlock Query Parameters:");
					sender.sendMessage(ChatColor.GOLD + "Use doublequotes to escape a keyword: world \"world\"");
					sender.sendMessage(ChatColor.GOLD + "player [name1] <name2> <name3> -- List of players");
					sender.sendMessage(ChatColor.GOLD + "block [type1] <type2> <type3> -- List of block types");
					sender.sendMessage(ChatColor.GOLD + "created, destroyed -- Show only created/destroyed blocks");
					sender.sendMessage(ChatColor.GOLD + "chestaccess -- Show only chest accesses");
					sender.sendMessage(ChatColor.GOLD + "area <radius> -- Area around you");
					sender.sendMessage(ChatColor.GOLD + "selection, sel -- Inside current WorldEdit selection");
					sender.sendMessage(ChatColor.GOLD + "world [worldname] -- Changes the world");
					sender.sendMessage(ChatColor.GOLD + "time [number] [minutes|hours|days] -- Limits time");
					sender.sendMessage(ChatColor.GOLD + "since <dd.MM.yyyy> <HH:mm:ss> -- Limits time to a fixed point");
					sender.sendMessage(ChatColor.GOLD + "before <dd.MM.yyyy> <HH:mm:ss> -- Affects only blocks before a fixed time");
					sender.sendMessage(ChatColor.GOLD + "limit <row count> -- Limits the result to count of rows");
					sender.sendMessage(ChatColor.GOLD + "sum [none|blocks|players] -- Sums the result");
					sender.sendMessage(ChatColor.GOLD + "asc, desc -- Changes the order of the displayed log");
					sender.sendMessage(ChatColor.GOLD + "coords -- Shows coordinates for each block");
					sender.sendMessage(ChatColor.GOLD + "silent -- Displays lesser messages");
				} else if (command.equals("permissions")) {
					sender.sendMessage(ChatColor.DARK_AQUA + "You've got the following permissions:");
					for (final String permission : new String[]{"me", "lookup", "tp", "rollback", "clearlog", "hide", "ignoreRestrictions", "spawnTools"})
						if (logblock.hasPermission(sender, "logblock." + permission))
							sender.sendMessage(ChatColor.GOLD + "logblock." + permission);
					for (final Tool tool : toolsByType.values())
						if (logblock.hasPermission(sender, "logblock.tools." + tool.name))
							sender.sendMessage(ChatColor.GOLD + "logblock.tools." + tool.name);
				} else if (command.equals("logging")) {
					if (logblock.hasPermission(sender, "logblock.lookup")) {
						World world = null;
						if (args.length > 1)
							world = getServer().getWorld(args[1]);
						else if (sender instanceof Player)
							world = ((Player)sender).getWorld();
						if (world != null) {
							final WorldConfig wcfg = getWorldConfig(world.getName());
							if (wcfg != null) {
								sender.sendMessage(ChatColor.DARK_AQUA + "Currently logging in " + world.getName() + ":");
								final List<String> logging = new ArrayList<String>();
								for (final Logging l : Logging.values())
									if (wcfg.isLogging(l))
										logging.add(l.toString());
								sender.sendMessage(ChatColor.GOLD + listing(logging, ", ", " and "));
							} else {
								sender.sendMessage(ChatColor.RED + "World not logged: '" + world.getName() + "'");
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Make the world name is listed at loggedWorlds in config. World names are case sensitive and must contains the path (if any), exactly like in the message above.");
							}
						} else
							sender.sendMessage(ChatColor.RED + "No world specified");
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (toolsByName.get(command) != null) {
					final Tool tool = toolsByName.get(command);
					if (logblock.hasPermission(sender, "logblock.tools." + tool.name)) {
						if (sender instanceof Player) {
							final Player player = (Player)sender;
							final Session session = Session.getSession(player.getName());
							final ToolData toolData = session.toolData.get(tool);
							if (args.length == 1) {
								if (logblock.hasPermission(player, "logblock.spawnTools")) {
									giveTool(player, tool.item);
									session.toolData.get(tool).enabled = true;
								} else
									sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
							} else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on")) {
								toolData.enabled = true;
								player.sendMessage(ChatColor.GREEN + "Tool enabled.");
							} else if (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("off")) {
								toolData.enabled = false;
								player.getInventory().removeItem(new ItemStack(tool.item, 1));
								player.sendMessage(ChatColor.GREEN + "Tool disabled.");
							} else if (args[1].equalsIgnoreCase("mode")) {
								if (args.length == 3) {
									final ToolMode mode;
									try {
										mode = ToolMode.valueOf(args[2].toUpperCase());
									} catch (final IllegalArgumentException ex) {
										sender.sendMessage(ChatColor.RED + "Can't find mode " + args[2]);
										return true;
									}
									if (logblock.hasPermission(player, mode.getPermission())) {
										toolData.mode = mode;
										sender.sendMessage(ChatColor.GREEN + "Tool mode set to " + args[2]);
									} else
										sender.sendMessage(ChatColor.RED + "You aren't allowed to use mode " + args[2]);
								} else
									player.sendMessage(ChatColor.RED + "No mode specified");
							} else if (args[1].equalsIgnoreCase("default")) {
								toolData.params = tool.params.clone();
								toolData.mode = tool.mode;
								sender.sendMessage(ChatColor.GREEN + "Tool set to default.");
							} else if (logblock.hasPermission(player, "logblock.lookup"))
								try {
									final QueryParams params = tool.params.clone();
									params.parseArgs(sender, argsToList(args, 1));
									toolData.params = params;
									sender.sendMessage(ChatColor.GREEN + "Set tool query to: " + params.getTitle());
								} catch (final Exception ex) {
									sender.sendMessage(ChatColor.RED + ex.getMessage());
								}
							else
								sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
						} else
							sender.sendMessage(ChatColor.RED + "You have to be a player.");
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (command.equals("hide")) {
					if (sender instanceof Player) {
						if (logblock.hasPermission(sender, "logblock.hide")) {
							if (Consumer.hide((Player)sender))
								sender.sendMessage(ChatColor.GREEN + "You are now hidden and aren't logged. Type '/lb hide' again to unhide");
							else
								sender.sendMessage(ChatColor.GREEN + "You aren't hidden anylonger.");
						} else
							sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
					} else
						sender.sendMessage(ChatColor.RED + "You have to be a player.");
				} else if (command.equals("page")) {
					if (args.length == 2 && isInt(args[1]))
						showPage(sender, Integer.valueOf(args[1]));
					else
						sender.sendMessage(ChatColor.RED + "You have to specify a page");
				} else if (command.equals("next") || command.equals("+"))
					showPage(sender, getSession(sender).page + 1);
				else if (command.equals("prev") || command.equals("-"))
					showPage(sender, getSession(sender).page - 1);
				else if (args[0].equalsIgnoreCase("savequeue")) {
					if (logblock.hasPermission(sender, "logblock.rollback"))
						new CommandSaveQueue(sender, null, true);
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (args[0].equalsIgnoreCase("queuesize")) {
					if (logblock.hasPermission(sender, "logblock.rollback"))
						sender.sendMessage("Current queue size: " + logblock.getConsumer().getQueueSize());
					else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (command.equals("rollback") || command.equals("undo") || command.equals("rb")) {
					if (logblock.hasPermission(sender, "logblock.rollback")) {
						final QueryParams params = new QueryParams(logblock);
						params.since = defaultTime;
						params.bct = BlockChangeType.ALL;
						params.parseArgs(sender, argsToList(args, 1));
						new CommandRollback(sender, params, true);
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (command.equals("redo")) {
					if (logblock.hasPermission(sender, "logblock.rollback")) {
						final QueryParams params = new QueryParams(logblock);
						params.since = defaultTime;
						params.bct = BlockChangeType.ALL;
						params.parseArgs(sender, argsToList(args, 1));
						new CommandRedo(sender, params, true);
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
				} else if (command.equals("me")) {
					if (sender instanceof Player) {
						if (logblock.hasPermission(sender, "logblock.me")) {
							final Player player = (Player)sender;
							if (Config.isLogged(player.getWorld())) {
								final QueryParams params = new QueryParams(logblock);
								params.setPlayer(player.getName());
								params.world = player.getWorld();
								player.sendMessage("Total block changes: " + logblock.getCount(params));
								params.sum = SummarizationMode.TYPES;
								new CommandLookup(sender, params, true);
							} else
								sender.sendMessage(ChatColor.RED + "This world isn't logged");
						} else
							sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
					} else
						sender.sendMessage(ChatColor.RED + "You have to be a player.");
				} else if (command.equals("writelogfile")) {
					if (logblock.hasPermission(sender, "logblock.rollback")) {
						final QueryParams params = new QueryParams(logblock);
						params.limit = -1;
						params.bct = BlockChangeType.ALL;
						params.parseArgs(sender, argsToList(args, 1));
						new CommandWriteLogFile(sender, params, true);
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
				} else if (command.equals("clearlog")) {
					if (logblock.hasPermission(sender, "logblock.clearlog")) {
						final QueryParams params = new QueryParams(logblock, sender, argsToList(args, 1));
						params.bct = BlockChangeType.ALL;
						params.limit = -1;
						new CommandClearLog(sender, params, true);
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
				} else if (command.equals("tp")) {
					if (sender instanceof Player) {
						if (logblock.hasPermission(sender, "logblock.tp"))
							if (args.length == 2 || isInt(args[1])) {
								final int pos = Integer.parseInt(args[1]) - 1;
								final Player player = (Player)sender;
								final Session session = getSession(player);
								if (session.lookupCache != null)
									if (pos >= 0 && pos < session.lookupCache.length) {
										final Location loc = session.lookupCache[pos].getLocation();
										if (loc != null) {
											player.teleport(new Location(loc.getWorld(), loc.getX() + 0.5, saveSpawnHeight(loc), loc.getZ() + 0.5, player.getLocation().getYaw(), 90));
											player.sendMessage(ChatColor.LIGHT_PURPLE + "Teleported to " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
										} else
											sender.sendMessage(ChatColor.RED + "There is no location associated with that. Did you forget coords parameter?");
									} else
										sender.sendMessage(ChatColor.RED + "'" + args[1] + " is out of range");
								else
									sender.sendMessage(ChatColor.RED + "You havn't done a lookup yet");
							} else
								new CommandTeleport(sender, new QueryParams(logblock, sender, argsToList(args, 1)), true);
						else
							sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
					} else
						sender.sendMessage(ChatColor.RED + "You have to be a player.");
				} else if (command.equals("lookup") || QueryParams.isKeyWord(args[0])) {
					if (logblock.hasPermission(sender, "logblock.lookup")) {
						final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
						if (command.equals("lookup"))
							argsList.remove(0);
						new CommandLookup(sender, new QueryParams(logblock, sender, argsList), true);
					} else
						sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
				} else
					sender.sendMessage(ChatColor.RED + "Unknown command '" + args[0] + "'");
			}
		} catch (final IllegalArgumentException ex) {
			sender.sendMessage(ChatColor.RED + ex.getMessage());
		} catch (final ArrayIndexOutOfBoundsException ex) {
			sender.sendMessage(ChatColor.RED + "Not enough arguments given");
		} catch (final Exception ex) {
			sender.sendMessage(ChatColor.RED + "Error, check server.log");
			getLogger().log(Level.WARNING, "Exception in commands handler: ", ex);
		}
		return true;
	}

	private static void showPage(CommandSender sender, int page) {
		final Session session = getSession(sender);
		if (session.lookupCache != null && session.lookupCache.length > 0) {
			final int startpos = (page - 1) * linesPerPage;
			if (page > 0 && startpos <= session.lookupCache.length - 1) {
				final int stoppos = startpos + linesPerPage >= session.lookupCache.length ? session.lookupCache.length - 1 : startpos + linesPerPage - 1;
				final int numberOfPages = (int)Math.ceil(session.lookupCache.length / (double)linesPerPage);
				if (numberOfPages != 1) {
					String format = "<color name=\"dark_aqua\">Page %1$d/%2$d</color>";
					if (page > 1) {
						format += " " + MessageHelper.button("/lb prev", "<", "blue", true);
					}
					if (page < numberOfPages) {
						format += " " + MessageHelper.button("/lb next", ">", "blue", true);
					}
					MessageHelper.sendMessage(sender, String.format(format, page, numberOfPages));
				}
				for (int i = startpos; i <= stoppos; i++)
					MessageHelper.sendMessage(sender, String.format(session.lookupCache[i].getLocation() != null ? "<color name=\"gold\" onClick=\"run_command('/lb tp %1$d')\" onHover=\"show_text('Click to run\n&lt;color name=&quot;blue&quot;>/lb tp %1$d&lt;/color>')\">%2$s</color>" : "<color name=\"gold\">%2$s</color>", i + 1, session.lookupCache[i].getXmlMessage()));
				session.page = page;
			} else
				sender.sendMessage(ChatColor.RED + "There isn't a page '" + page + "'");
		} else
			sender.sendMessage(ChatColor.RED + "No blocks in lookup cache");
	}

	private boolean checkRestrictions(CommandSender sender, QueryParams params) {
		if (sender.isOp() || logblock.hasPermission(sender, "logblock.ignoreRestrictions"))
			return true;
		if (rollbackMaxTime > 0 && (params.before > 0 || params.since > rollbackMaxTime)) {
			sender.sendMessage(ChatColor.RED + "You are not allowed to rollback more than " + rollbackMaxTime + " minutes");
			return false;
		}
		if (rollbackMaxArea > 0 && (params.sel == null && params.loc == null || params.radius > rollbackMaxArea || params.sel != null && (params.sel.getSelection().getLength() > rollbackMaxArea || params.sel.getSelection().getWidth() > rollbackMaxArea))) {
			sender.sendMessage(ChatColor.RED + "You are not allowed to rollback an area larger than " + rollbackMaxArea + " blocks");
			return false;
		}
		return true;
	}

	public abstract class AbstractCommand implements Runnable, Closeable
	{
		protected CommandSender sender;
		protected QueryParams params;
		protected Connection conn = null;
		protected Statement state = null;
		protected ResultSet rs = null;

		protected AbstractCommand(CommandSender sender, QueryParams params, boolean async) throws Exception {
			this.sender = sender;
			this.params = params;
			if (async) {
				if (scheduler.scheduleAsyncDelayedTask(logblock, this) == -1)
					throw new Exception("Failed to schedule the command");
			} else
				run();
		}

		@Override
		public final void close() {
			try {
				if (conn != null)
					conn.close();
				if (state != null)
					state.close();
				if (rs != null)
					rs.close();
			} catch (final SQLException ex) {
				getLogger().log(Level.SEVERE, "[CommandsHandler] SQL exception on close", ex);
			}
		}
	}

	public class CommandLookup extends AbstractCommand
	{
		public CommandLookup(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			try {
				if (params.bct == BlockChangeType.CHAT) {
					params.needDate = true;
					params.needPlayer = true;
					params.needMessage = true;
				} else if (params.bct == BlockChangeType.KILLS) {
					params.needDate = true;
					params.needPlayer = true;
					params.needKiller = true;
					params.needVictim = true;
					params.needWeapon = true;
				} else {
					params.needDate = true;
					params.needType = true;
					params.needData = true;
					params.needPlayer = true;
					if (params.types.isEmpty() || Block.inList(params.types, 63) || Block.inList(params.types, 68))
						params.needSignText = true;
					if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL)
						params.needChestAccess = true;
				}
				conn = logblock.getConnection();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				state = conn.createStatement();
				rs = state.executeQuery(params.getQuery());
				sender.sendMessage(ChatColor.DARK_AQUA + params.getTitle() + ":");
				if (rs.next()) {
					rs.beforeFirst();
					final List<LookupCacheElement> blockchanges = new ArrayList<LookupCacheElement>();
					final LookupCacheElementFactory factory = new LookupCacheElementFactory(params, sender instanceof Player ? 2 / 3f : 1);
					while (rs.next())
						blockchanges.add(factory.getLookupCacheElement(rs));
					getSession(sender).lookupCache = blockchanges.toArray(new LookupCacheElement[blockchanges.size()]);
					if (blockchanges.size() > linesPerPage)
						sender.sendMessage(ChatColor.DARK_AQUA.toString() + blockchanges.size() + " changes found." + (blockchanges.size() == linesLimit ? " Use 'limit -1' to see all changes." : ""));
					if (params.sum != SummarizationMode.NONE)
						if (params.bct == BlockChangeType.KILLS && params.sum == SummarizationMode.PLAYERS)
							sender.sendMessage(ChatColor.GOLD + "Kills - Killed - Player");
						else
							sender.sendMessage(ChatColor.GOLD + "Created - Destroyed - " + (params.sum == SummarizationMode.TYPES ? "Block" : "Player"));
					showPage(sender, 1);
				} else {
					sender.sendMessage(ChatColor.DARK_AQUA + "No results found.");
					getSession(sender).lookupCache = null;
				}
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[Lookup] " + params.getQuery() + ": ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandWriteLogFile extends AbstractCommand
	{
		public CommandWriteLogFile(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			File file = null;
			try {
				if (params.bct == BlockChangeType.CHAT) {
					params.needDate = true;
					params.needPlayer = true;
					params.needMessage = true;
				} else {
					params.needDate = true;
					params.needType = true;
					params.needData = true;
					params.needPlayer = true;
					if (params.types.isEmpty() || Block.inList(params.types, 63) || Block.inList(params.types, 68))
						params.needSignText = true;
					if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL)
						params.needChestAccess = true;
				}
				conn = logblock.getConnection();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				state = conn.createStatement();
				file = new File("plugins/LogBlock/log/" + params.getTitle().replace(":", ".") + ".log");
				sender.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
				rs = state.executeQuery(params.getQuery());
				file.getParentFile().mkdirs();
				file.createNewFile();
				final FileWriter writer = new FileWriter(file);
				final String newline = System.getProperty("line.separator");
				file.getParentFile().mkdirs();
				int counter = 0;
				if (params.sum != SummarizationMode.NONE)
					writer.write("Created - Destroyed - " + (params.sum == SummarizationMode.TYPES ? "Block" : "Player") + newline);
				final LookupCacheElementFactory factory = new LookupCacheElementFactory(params, sender instanceof Player ? 2 / 3f : 1);
				while (rs.next()) {
					writer.write(factory.getLookupCacheElement(rs).getMessage() + newline);
					counter++;
				}
				writer.close();
				sender.sendMessage(ChatColor.GREEN + "Wrote " + counter + " lines.");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[WriteLogFile] " + params.getQuery() + " (file was " + file.getAbsolutePath() + "): ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandSaveQueue extends AbstractCommand
	{
		public CommandSaveQueue(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			final Consumer consumer = logblock.getConsumer();
			if (consumer.getQueueSize() > 0) {
				sender.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
				int lastSize = -1, fails = 0;
				while (consumer.getQueueSize() > 0) {
					fails = lastSize == consumer.getQueueSize() ? fails + 1 : 0;
					if (fails > 10) {
						sender.sendMessage(ChatColor.RED + "Unable to save queue");
						return;
					}
					lastSize = consumer.getQueueSize();
					consumer.run();
				}
				sender.sendMessage(ChatColor.GREEN + "Queue saved successfully");
			}
		}
	}

	public class CommandTeleport extends AbstractCommand
	{
		public CommandTeleport(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			try {
				params.needCoords = true;
				if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL)
					params.needChestAccess = true;
				params.limit = 1;
				params.sum = SummarizationMode.NONE;
				conn = logblock.getConnection();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				state = conn.createStatement();
				rs = state.executeQuery(params.getQuery());
				if (rs.next()) {
					final Player player = (Player)sender;
					final int y = rs.getInt("y");
					final Location loc = new Location(params.world, rs.getInt("x") + 0.5, y, rs.getInt("z") + 0.5, player.getLocation().getYaw(), 90);

					// Teleport the player sync because omg thread safety
					logblock.getServer().getScheduler().scheduleSyncDelayedTask(logblock, new Runnable() {
						@Override
						public void run() {
							final int y2 = saveSpawnHeight(loc);
							loc.setY(y2);
							player.teleport(loc);
							sender.sendMessage(ChatColor.GREEN + "You were teleported " + Math.abs(y2 - y) + " blocks " + (y2 - y > 0 ? "above" : "below"));
						}
					});
				} else
					sender.sendMessage(ChatColor.RED + "No block change found to teleport to");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[Teleport] " + params.getQuery() + ": ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandRollback extends AbstractCommand
	{
		public CommandRollback(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			try {
				params.needCoords = true;
				params.needType = true;
				params.needData = true;
				params.needSignText = true;
				params.needChestAccess = true;
				params.order = Order.DESC;
				params.sum = SummarizationMode.NONE;
				conn = logblock.getConnection();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				state = conn.createStatement();
				if (!checkRestrictions(sender, params))
					return;
				if (logblock.getConsumer().getQueueSize() > 0)
					new CommandSaveQueue(sender, null, false);
				if (!params.silent)
					sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
				rs = state.executeQuery(params.getQuery());
                final WorldEditor editor = new WorldEditor(logblock, params.world);

				while (rs.next())
					editor.queueEdit(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("replaced"), rs.getInt("type"), rs.getByte("data"), rs.getString("signtext"), rs.getShort("itemtype"), rs.getShort("itemamount"), rs.getShort("itemdata"));
				final int changes = editor.getSize();
                if (changes > 10000) {
                    editor.setSender(sender);
                }
				if (!params.silent)
					sender.sendMessage(ChatColor.GREEN.toString() + changes + " blocks found.");
				if (changes == 0) {
					if (!params.silent)
						sender.sendMessage(ChatColor.RED + "Rollback aborted");
					return;
				}
				if (!params.silent && askRollbacks && questioner != null && sender instanceof Player && !questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
					sender.sendMessage(ChatColor.RED + "Rollback aborted");
					return;
				}
				editor.start();
				getSession(sender).lookupCache = editor.errors;
				sender.sendMessage(ChatColor.GREEN + "Rollback finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
				if (!params.silent && askClearLogAfterRollback && logblock.hasPermission(sender, "logblock.clearlog") && questioner != null && sender instanceof Player) {
					Thread.sleep(1000);
					if (questioner.ask((Player)sender, "Do you want to delete the rollbacked log?", "yes", "no").equals("yes")) {
						params.silent = true;
						new CommandClearLog(sender, params, false);
					} else
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Clearlog cancelled");
				}
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[Rollback] " + params.getQuery() + ": ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandRedo extends AbstractCommand
	{
		public CommandRedo(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			try {
				params.needCoords = true;
				params.needType = true;
				params.needData = true;
				params.needSignText = true;
				params.needChestAccess = true;
				params.order = Order.ASC;
				params.sum = SummarizationMode.NONE;
				conn = logblock.getConnection();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				state = conn.createStatement();
				if (!checkRestrictions(sender, params))
					return;
				rs = state.executeQuery(params.getQuery());
				if (!params.silent)
					sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
				final WorldEditor editor = new WorldEditor(logblock, params.world);
				while (rs.next())
					editor.queueEdit(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getString("signtext"), rs.getShort("itemtype"), (short)-rs.getShort("itemamount"), rs.getShort("itemdata"));
				final int changes = editor.getSize();
				if (!params.silent)
					sender.sendMessage(ChatColor.GREEN.toString() + changes + " blocks found.");
				if (changes == 0) {
					if (!params.silent)
						sender.sendMessage(ChatColor.RED + "Redo aborted");
					return;
				}
				if (!params.silent && askRedos && questioner != null && sender instanceof Player && !questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
					sender.sendMessage(ChatColor.RED + "Redo aborted");
					return;
				}
				editor.start();
				sender.sendMessage(ChatColor.GREEN + "Redo finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[Redo] " + params.getQuery() + ": ", ex);
			} finally {
				close();
			}
		}
	}

	public class CommandClearLog extends AbstractCommand
	{
		public CommandClearLog(CommandSender sender, QueryParams params, boolean async) throws Exception {
			super(sender, params, async);
		}

		@Override
		public void run() {
			try {
				conn = logblock.getConnection();
				state = conn.createStatement();
				if (conn == null) {
					sender.sendMessage(ChatColor.RED + "MySQL connection lost");
					return;
				}
				if (!checkRestrictions(sender, params))
					return;
				final File dumpFolder = new File(logblock.getDataFolder(), "dump");
				if (!dumpFolder.exists())
					dumpFolder.mkdirs();
				final String time = new SimpleDateFormat("yyMMddHHmmss").format(System.currentTimeMillis());
				int deleted;
				final String table = params.getTable();
				final String join = params.players.size() > 0 ? "INNER JOIN `lb-players` USING (playerid) " : "";
				rs = state.executeQuery("SELECT count(*) FROM `" + table + "` " + join + params.getWhere());
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (!params.silent && askClearLogs && sender instanceof Player && questioner != null) {
						sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
						sender.sendMessage(ChatColor.GREEN.toString() + deleted + " blocks found.");
						if (!questioner.ask((Player)sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
							sender.sendMessage(ChatColor.RED + "ClearLog aborted");
							return;
						}
					}
					if (dumpDeletedLog)
						try {
							state.execute("SELECT * FROM `" + table + "` " + join + params.getWhere() + "INTO OUTFILE '" + new File(dumpFolder, time + " " + table + " " + params.getTitle().replace(":", ".") + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
						} catch (final SQLException ex) {
							sender.sendMessage(ChatColor.RED + "Error while dumping log. Make sure your MySQL user has access to the LogBlock folder, or disable clearlog.dumpDeletedLog");
							getLogger().log(Level.SEVERE, "[ClearLog] Exception while dumping log: ", ex);
							return;
						}
					state.execute("DELETE `" + table + "` FROM `" + table + "` " + join + params.getWhere());
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + ". Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (dumpDeletedLog)
						state.execute("SELECT id, signtext FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, time + " " + table + "-sign " + params.getTitle() + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-sign` FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + "-sign. Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				if ((deleted = rs.getInt(1)) > 0) {
					if (dumpDeletedLog)
						state.execute("SELECT id, itemtype, itemamount, itemdata FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, time + " " + table + "-chest " + params.getTitle() + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-chest` FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					sender.sendMessage(ChatColor.GREEN + "Cleared out table " + table + "-chest. Deleted " + deleted + " entries.");
				}
			} catch (final Exception ex) {
				sender.sendMessage(ChatColor.RED + "Exception, check error log");
				getLogger().log(Level.SEVERE, "[ClearLog] Exception: ", ex);
			} finally {
				close();
			}
		}
	}

	private static List<String> argsToList(String[] arr, int offset) {
		final List<String> list = new ArrayList<String>(Arrays.asList(arr));
		for (int i = 0; i < offset; i++)
			list.remove(0);
		return list;
	}
}
