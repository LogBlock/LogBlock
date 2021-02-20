package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.config.Config.askClearLogAfterRollback;
import static de.diddiz.LogBlock.config.Config.askClearLogs;
import static de.diddiz.LogBlock.config.Config.askRedos;
import static de.diddiz.LogBlock.config.Config.askRollbacks;
import static de.diddiz.LogBlock.config.Config.defaultTime;
import static de.diddiz.LogBlock.config.Config.dumpDeletedLog;
import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.linesLimit;
import static de.diddiz.LogBlock.config.Config.linesPerPage;
import static de.diddiz.LogBlock.config.Config.rollbackMaxArea;
import static de.diddiz.LogBlock.config.Config.rollbackMaxTime;
import static de.diddiz.LogBlock.config.Config.toolsByName;
import static de.diddiz.LogBlock.config.Config.toolsByType;
import static de.diddiz.util.BukkitUtils.giveTool;
import static de.diddiz.util.BukkitUtils.saveSpawnHeight;
import static de.diddiz.util.TypeColor.DEFAULT;
import static de.diddiz.util.TypeColor.ERROR;
import static de.diddiz.util.TypeColor.HEADER;
import static de.diddiz.util.Utils.isInt;
import static de.diddiz.util.Utils.listing;

import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.QueryParams.Order;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.Utils;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

public class CommandsHandler implements CommandExecutor {
    private final LogBlock logblock;
    private final BukkitScheduler scheduler;

    CommandsHandler(LogBlock logblock) {
        this.logblock = logblock;
        scheduler = logblock.getServer().getScheduler();
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
                    sender.sendMessage(ChatColor.GOLD + "entities [type1] <type2> <type3> -- List of entity types; can not be combined with blocks");
                    sender.sendMessage(ChatColor.GOLD + "area <radius> -- Area around you");
                    sender.sendMessage(ChatColor.GOLD + "selection, sel -- Inside current WorldEdit selection");
                    sender.sendMessage(ChatColor.GOLD + "world [worldname] -- Changes the world");
                    sender.sendMessage(ChatColor.GOLD + "time [number] [minutes|hours|days] -- Limits time");
                    sender.sendMessage(ChatColor.GOLD + "since <dd.MM.yyyy> <HH:mm:ss> -- Limits time to a fixed point");
                    sender.sendMessage(ChatColor.GOLD + "before <dd.MM.yyyy> <HH:mm:ss> -- Affects only blocks before a fixed time");
                    sender.sendMessage(ChatColor.GOLD + "force -- Forces replacing not matching blocks");
                    sender.sendMessage(ChatColor.GOLD + "limit <row count> -- Limits the result to count of rows");
                    sender.sendMessage(ChatColor.GOLD + "sum [none|blocks|players] -- Sums the result");
                    sender.sendMessage(ChatColor.GOLD + "asc, desc -- Changes the order of the displayed log");
                    sender.sendMessage(ChatColor.GOLD + "coords -- Shows coordinates for each block");
                    sender.sendMessage(ChatColor.GOLD + "nocache -- Don't set the lookup cache");
                    sender.sendMessage(ChatColor.GOLD + "silent -- Displays lesser messages");
                } else if (command.equals("permissions")) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "You've got the following permissions:");
                    for (final String permission : new String[] { "me", "lookup", "tp", "rollback", "clearlog", "hide", "ignoreRestrictions", "spawnTools" }) {
                        if (logblock.hasPermission(sender, "logblock." + permission)) {
                            sender.sendMessage(ChatColor.GOLD + "logblock." + permission);
                        }
                    }
                    for (final Tool tool : toolsByType.values()) {
                        if (logblock.hasPermission(sender, "logblock.tools." + tool.name)) {
                            sender.sendMessage(ChatColor.GOLD + "logblock.tools." + tool.name);
                        }
                    }
                } else if (command.equals("logging")) {
                    if (logblock.hasPermission(sender, "logblock.lookup")) {
                        World world = null;
                        if (args.length > 1) {
                            world = logblock.getServer().getWorld(args[1]);
                        } else if (sender instanceof Player) {
                            world = ((Player) sender).getWorld();
                        }
                        if (world != null) {
                            final WorldConfig wcfg = getWorldConfig(world.getName());
                            if (wcfg != null) {
                                sender.sendMessage(ChatColor.DARK_AQUA + "Currently logging in " + world.getName() + ":");
                                final List<String> logging = new ArrayList<>();
                                for (final Logging l : Logging.values()) {
                                    if (wcfg.isLogging(l)) {
                                        logging.add(l.toString());
                                    }
                                }
                                sender.sendMessage(ChatColor.GOLD + listing(logging, ", ", " and "));
                            } else {
                                sender.sendMessage(ChatColor.RED + "World not logged: '" + world.getName() + "'");
                                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Make the world name is listed at loggedWorlds in config. World names are case sensitive and must contains the path (if any), exactly like in the message above.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "No world specified");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (toolsByName.get(command) != null) {
                    final Tool tool = toolsByName.get(command);
                    if (logblock.hasPermission(sender, "logblock.tools." + tool.name)) {
                        if (sender instanceof Player) {
                            final Player player = (Player) sender;
                            final Session session = Session.getSession(player);
                            final ToolData toolData = session.toolData.get(tool);
                            if (args.length == 1) {
                                if (logblock.hasPermission(player, "logblock.spawnTools")) {
                                    giveTool(player, tool.item);
                                    toolData.enabled = true;
                                } else {
                                    sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                                }
                            } else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on")) {
                                toolData.enabled = true;
                                player.sendMessage(ChatColor.GREEN + "Tool enabled.");
                            } else if (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("off")) {
                                toolData.enabled = false;
                                if (tool.removeOnDisable && logblock.hasPermission(player, "logblock.spawnTools")) {
                                    if (!player.getInventory().removeItem(new ItemStack(tool.item, 1)).isEmpty()) {
                                        // remove the tool from the offhand if it cannot be found in the main hand
                                        ItemStack offhandItem = player.getInventory().getItemInOffHand();
                                        if (offhandItem != null && offhandItem.isSimilar(new ItemStack(tool.item, 1))) {
                                            if (offhandItem.getAmount() <= 1) {
                                                player.getInventory().setItemInOffHand(null);
                                            } else {
                                                offhandItem.setAmount(offhandItem.getAmount() - 1);
                                                player.getInventory().setItemInOffHand(offhandItem);
                                            }
                                        }
                                    }
                                }
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
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "You aren't allowed to use mode " + args[2]);
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "No mode specified");
                                }
                            } else if (args[1].equalsIgnoreCase("default")) {
                                toolData.params = tool.params.clone();
                                toolData.mode = tool.mode;
                                sender.sendMessage(ChatColor.GREEN + "Tool set to default.");
                            } else if (logblock.hasPermission(player, "logblock.lookup")) {
                                try {
                                    final QueryParams params = tool.params.clone();
                                    params.parseArgs(sender, argsToList(args, 1));
                                    toolData.params = params;
                                    sender.sendMessage(ChatColor.GREEN + "Set tool query to: " + params.getTitle());
                                } catch (final Exception ex) {
                                    sender.sendMessage(ChatColor.RED + ex.getMessage());
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You have to be a player.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (command.equals("hide")) {
                    if (sender instanceof Player) {
                        if (logblock.hasPermission(sender, "logblock.hide")) {
                            if (args.length == 2) {
                                if (args[1].equalsIgnoreCase("on")) {
                                    Consumer.hide((Player) sender);
                                    sender.sendMessage(ChatColor.GREEN + "You are now hidden and aren't logged. Type /lb hide to unhide.");
                                } else if (args[1].equalsIgnoreCase("off")) {
                                    Consumer.unHide((Player) sender);
                                    sender.sendMessage(ChatColor.GREEN + "You aren't hidden any longer.");
                                }
                            } else {
                                if (Consumer.toggleHide((Player) sender)) {
                                    sender.sendMessage(ChatColor.GREEN + "You are now hidden and aren't logged. Type '/lb hide' again to unhide.");
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "You aren't hidden any longer.");
                                }
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be a player.");
                    }
                } else if (command.equals("page")) {
                    if (args.length == 2 && isInt(args[1])) {
                        showPage(sender, Integer.valueOf(args[1]));
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to specify a page");
                    }
                } else if (command.equals("next") || command.equals("+")) {
                    showPage(sender, getSession(sender).page + 1);
                } else if (command.equals("prev") || command.equals("-")) {
                    showPage(sender, getSession(sender).page - 1);
                } else if (args[0].equalsIgnoreCase("savequeue")) {
                    if (logblock.hasPermission(sender, "logblock.rollback")) {
                        new CommandSaveQueue(sender, null, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (args[0].equalsIgnoreCase("queuesize")) {
                    if (logblock.hasPermission(sender, "logblock.rollback")) {
                        sender.sendMessage("Current queue size: " + logblock.getConsumer().getQueueSize());
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (command.equals("rollback") || command.equals("undo") || command.equals("rb")) {
                    if (logblock.hasPermission(sender, "logblock.rollback")) {
                        final QueryParams params = new QueryParams(logblock);
                        params.since = defaultTime;
                        params.bct = BlockChangeType.ALL;
                        params.parseArgs(sender, argsToList(args, 1));
                        new CommandRollback(sender, params, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (command.equals("redo")) {
                    if (logblock.hasPermission(sender, "logblock.rollback")) {
                        final QueryParams params = new QueryParams(logblock);
                        params.since = defaultTime;
                        params.bct = BlockChangeType.ALL;
                        params.parseArgs(sender, argsToList(args, 1));
                        new CommandRedo(sender, params, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                    }
                } else if (command.equals("me")) {
                    if (sender instanceof Player) {
                        if (logblock.hasPermission(sender, "logblock.me")) {

                            final Player player = (Player) sender;
                            if (Config.isLogged(player.getWorld())) {
                                scheduler.runTaskAsynchronously(logblock, new Runnable(){

                                    public void run(){
                                    try{
                                    final QueryParams params = new QueryParams(logblock);
                                    params.setPlayer(player.getName());
                                    params.world = player.getWorld();
                                    player.sendMessage("Total block changes: " + logblock.getCount(params));
                                    params.sum = SummarizationMode.TYPES;
                                    new CommandLookup(sender, params, true);}
                                    catch (final Exception ex) {
                                        sender.sendMessage(ChatColor.RED + "Error, check server.log");
                                        logblock.getLogger().log(Level.WARNING, "Exception in commands handler: ", ex);
                                    }
                                }});

                            } else {
                                sender.sendMessage(ChatColor.RED + "This world isn't logged");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You aren't allowed to do this.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be a player.");
                    }
                } else if (command.equals("writelogfile")) {
                    if (logblock.hasPermission(sender, "logblock.rollback")) {
                        final QueryParams params = new QueryParams(logblock);
                        params.limit = -1;
                        params.bct = BlockChangeType.ALL;
                        params.parseArgs(sender, argsToList(args, 1));
                        new CommandWriteLogFile(sender, params, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
                    }
                } else if (command.equals("clearlog")) {
                    if (logblock.hasPermission(sender, "logblock.clearlog")) {
                        final QueryParams params = new QueryParams(logblock);
                        params.limit = -1;
                        params.bct = BlockChangeType.ALL;
                        params.parseArgs(sender, argsToList(args, 1));
                        new CommandClearLog(sender, params, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
                    }
                } else if (command.equals("tp")) {
                    if (sender instanceof Player) {
                        if (logblock.hasPermission(sender, "logblock.tp")) {
                            if (args.length == 2 || isInt(args[1])) {
                                final int pos = Integer.parseInt(args[1]) - 1;
                                final Player player = (Player) sender;
                                final Session session = getSession(player);
                                if (session.lookupCache != null) {
                                    if (pos >= 0 && pos < session.lookupCache.length) {
                                        final Location loc = session.lookupCache[pos].getLocation();
                                        if (loc != null) {
                                            player.teleport(new Location(loc.getWorld(), loc.getX() + 0.5, saveSpawnHeight(loc), loc.getZ() + 0.5, player.getLocation().getYaw(), 90));
                                            player.sendMessage(ChatColor.LIGHT_PURPLE + "Teleported to " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
                                        } else {
                                            sender.sendMessage(ChatColor.RED + "There is no location associated with that. Did you forget coords parameter?");
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "'" + args[1] + " is out of range");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + "You havn't done a lookup yet");
                                }
                            } else {
                                new CommandTeleport(sender, new QueryParams(logblock, sender, argsToList(args, 1)), true);
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be a player.");
                    }
                } else if (command.equals("lookup") || QueryParams.isKeyWord(args[0])) {
                    if (logblock.hasPermission(sender, "logblock.lookup")) {
                        final List<String> argsList = new ArrayList<>(Arrays.asList(args));
                        if (command.equals("lookup")) {
                            argsList.remove(0);
                        }
                        final QueryParams params = new QueryParams(logblock);
                        params.since = defaultTime;
                        params.bct = BlockChangeType.ALL;
                        params.limit = Config.linesLimit;
                        params.parseArgs(sender, argsList);
                        new CommandLookup(sender, params, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You aren't allowed to do this");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown command '" + args[0] + "'");
                }
            }
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (final ArrayIndexOutOfBoundsException ex) {
            sender.sendMessage(ChatColor.RED + "Not enough arguments given");
        } catch (final Exception ex) {
            sender.sendMessage(ChatColor.RED + "Error, check server.log");
            logblock.getLogger().log(Level.WARNING, "Exception in commands handler: ", ex);
        }
        return true;
    }

    private static void showPage(CommandSender sender, int page) {
        showPage(sender, page, getSession(sender).lookupCache, true);
    }

    private static void showPage(CommandSender sender, int page, LookupCacheElement[] lookupElements, boolean setSessionPage) {
        if (lookupElements != null && lookupElements.length > 0) {
            final int startpos = (page - 1) * linesPerPage;
            if (page > 0 && startpos <= lookupElements.length - 1) {
                final int stoppos = startpos + linesPerPage >= lookupElements.length ? lookupElements.length - 1 : startpos + linesPerPage - 1;
                final int numberOfPages = (int) Math.ceil(lookupElements.length / (double) linesPerPage);
                if (numberOfPages != 1) {
                    sender.sendMessage(HEADER + "Page " + page + "/" + numberOfPages);
                }
                for (int i = startpos; i <= stoppos; i++) {
                    TextComponent message = new TextComponent();
                    message.setColor(DEFAULT.getColor());
                    if (lookupElements[i].getLocation() != null) {
                        message.addExtra(new TextComponent("(" + (i + 1) + ") "));
                    }
                    for (BaseComponent component : lookupElements[i].getLogMessage(i + 1)) {
                        message.addExtra(component);
                    }
                    sender.spigot().sendMessage(message);
                }
                if (setSessionPage) {
                    getSession(sender).page = page;
                }
            } else {
                sender.sendMessage(ERROR + "There isn't a page '" + page + "'");
            }
        } else {
            sender.sendMessage(ERROR + "No blocks in lookup cache");
        }
    }

    private boolean checkRestrictions(CommandSender sender, QueryParams params) {
        if (sender.isOp() || logblock.hasPermission(sender, "logblock.ignoreRestrictions")) {
            return true;
        }
        if (rollbackMaxTime > 0 && (params.since <= 0 || params.since > rollbackMaxTime)) {
            sender.sendMessage(ChatColor.RED + "You are not allowed to rollback more than " + rollbackMaxTime + " minutes");
            return false;
        }
        if (rollbackMaxArea > 0 && (params.sel == null && params.loc == null || params.radius > rollbackMaxArea || params.sel != null && (params.sel.getSizeX() > rollbackMaxArea || params.sel.getSizeZ() > rollbackMaxArea))) {
            sender.sendMessage(ChatColor.RED + "You are not allowed to rollback an area larger than " + rollbackMaxArea + " blocks");
            return false;
        }
        return true;
    }

    public abstract class AbstractCommand implements Runnable {
        protected CommandSender sender;
        protected QueryParams params;
        protected Connection conn = null;
        protected Statement state = null;
        protected ResultSet rs = null;

        protected AbstractCommand(CommandSender sender, QueryParams params, boolean async) throws Exception {
            this.sender = sender;
            this.params = params == null ? null : params.clone();
            if (async) {
                scheduler.runTaskAsynchronously(logblock, this);
            } else {
                run();
            }
        }

        public final void close() {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (state != null) {
                    state.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (final SQLException ex) {
                if (logblock.isCompletelyEnabled()) {
                    logblock.getLogger().log(Level.SEVERE, "[CommandsHandler] SQL exception on close", ex);
                }
            }
        }
    }

    public class CommandLookup extends AbstractCommand {
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
                    if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL) {
                        params.needChestAccess = true;
                    }
                }
                conn = logblock.getConnection();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                state = conn.createStatement();
                rs = executeQuery(state, params.getQuery());
                sender.sendMessage(ChatColor.DARK_AQUA + params.getTitle() + ":");
                if (rs.next()) {
                    rs.beforeFirst();
                    final List<LookupCacheElement> blockchanges = new ArrayList<>();
                    final LookupCacheElementFactory factory = new LookupCacheElementFactory(params, sender instanceof Player ? 2 / 3f : 1);
                    while (rs.next()) {
                        blockchanges.add(factory.getLookupCacheElement(rs));
                    }
                    LookupCacheElement[] blockChangeArray = blockchanges.toArray(new LookupCacheElement[blockchanges.size()]);
                    if (!params.noCache) {
                        getSession(sender).lookupCache = blockChangeArray;
                    }
                    if (blockchanges.size() > linesPerPage) {
                        sender.sendMessage(ChatColor.DARK_AQUA.toString() + blockchanges.size() + " changes found." + (blockchanges.size() == linesLimit ? " Use 'limit -1' to see all changes." : ""));
                    }
                    if (params.sum != SummarizationMode.NONE) {
                        if (params.bct == BlockChangeType.KILLS && params.sum == SummarizationMode.PLAYERS) {
                            sender.sendMessage(ChatColor.GOLD + "Kills - Killed - Player");
                        } else {
                            sender.sendMessage(ChatColor.GOLD + "Created - Destroyed - " + (params.sum == SummarizationMode.TYPES ? (params.bct == BlockChangeType.ENTITIES ? "Entity" : "Block") : "Player"));
                        }
                    }
                    if (!params.noCache) {
                        showPage(sender, 1);
                    } else {
                        showPage(sender, 1, blockChangeArray, false);
                    }
                } else {
                    sender.sendMessage(ChatColor.DARK_AQUA + "No results found.");
                    if (!params.noCache) {
                        getSession(sender).lookupCache = null;
                    }
                }
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[Lookup] " + params.getQuery() + ": ", ex);
                }
            } finally {
                close();
            }
        }
    }

    public class CommandWriteLogFile extends AbstractCommand {
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
                    if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL) {
                        params.needChestAccess = true;
                    }
                }
                conn = logblock.getConnection();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                state = conn.createStatement();
                final File dumpFolder = new File(logblock.getDataFolder(), "log");
                if (!dumpFolder.exists()) {
                    dumpFolder.mkdirs();
                }
                file = new File(dumpFolder, params.getTitle().replace(":", ".").replace("/", "_").replace("\\", "_") + ".log");
                sender.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
                rs = executeQuery(state, params.getQuery());
                file.getParentFile().mkdirs();
                file.createNewFile();
                final FileWriter writer = new FileWriter(file);
                final String newline = System.getProperty("line.separator");
                file.getParentFile().mkdirs();
                int counter = 0;
                if (params.sum != SummarizationMode.NONE) {
                    writer.write("Created - Destroyed - " + (params.sum == SummarizationMode.TYPES ? (params.bct == BlockChangeType.ENTITIES ? "Entity" : "Block") : "Player") + newline);
                }
                final LookupCacheElementFactory factory = new LookupCacheElementFactory(params, sender instanceof Player ? 2 / 3f : 1);
                while (rs.next()) {
                    writer.write(BaseComponent.toPlainText(factory.getLookupCacheElement(rs).getLogMessage()) + newline);
                    counter++;
                }
                writer.close();
                sender.sendMessage(ChatColor.GREEN + "Wrote " + counter + " lines.");
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[WriteLogFile] " + params.getQuery() + " (file was " + file.getAbsolutePath() + "): ", ex);
                }
            } finally {
                close();
            }
        }
    }

    public class CommandSaveQueue extends AbstractCommand {
        public CommandSaveQueue(CommandSender sender, QueryParams params, boolean async) throws Exception {
            super(sender, params, async);
        }

        @Override
        public void run() {
            final Consumer consumer = logblock.getConsumer();
            sender.sendMessage(ChatColor.DARK_AQUA + "Current queue size: " + consumer.getQueueSize());
        }
    }

    public class CommandTeleport extends AbstractCommand {
        public CommandTeleport(CommandSender sender, QueryParams params, boolean async) throws Exception {
            super(sender, params, async);
        }

        @Override
        public void run() {
            try {
                params.needCoords = true;
                if (params.bct == BlockChangeType.CHESTACCESS || params.bct == BlockChangeType.ALL) {
                    params.needChestAccess = true;
                }
                params.limit = 1;
                params.sum = SummarizationMode.NONE;
                conn = logblock.getConnection();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                state = conn.createStatement();
                rs = executeQuery(state, params.getQuery());
                if (rs.next()) {
                    final Player player = (Player) sender;
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
                } else {
                    sender.sendMessage(ChatColor.RED + "No block change found to teleport to");
                }
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[Teleport] " + params.getQuery() + ": ", ex);
                }
            } finally {
                close();
            }
        }
    }

    public class CommandRollback extends AbstractCommand {
        public CommandRollback(CommandSender sender, QueryParams params, boolean async) throws Exception {
            super(sender, params, async);
        }

        @Override
        public void run() {
            try {
                if (params.bct == BlockChangeType.CHAT) {
                    sender.sendMessage(ChatColor.RED + "Chat cannot be rolled back");
                    return;
                }
                if (params.bct == BlockChangeType.KILLS) {
                    sender.sendMessage(ChatColor.RED + "Kills cannot be rolled back");
                    return;
                }
                if (params.sum != SummarizationMode.NONE) {
                    sender.sendMessage(ChatColor.RED + "Cannot rollback summarized changes");
                    return;
                }
                params.needDate = true;
                params.needCoords = true;
                params.needType = true;
                params.needData = true;
                params.needChestAccess = true;
                params.sum = SummarizationMode.NONE;
                conn = logblock.getConnection();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                state = conn.createStatement();
                if (!checkRestrictions(sender, params)) {
                    return;
                }
                if (!params.silent) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
                }
                rs = executeQuery(state, params.getQuery());
                final WorldEditor editor = new WorldEditor(logblock, params.world, params.forceReplace);
                WorldEditorEditFactory editFactory = new WorldEditorEditFactory(editor, params, true);
                while (rs.next()) {
                    editFactory.processRow(rs);
                }
                if (params.order == Order.DESC) {
                    editor.reverseRowOrder();
                }
                editor.sortRows(Order.DESC);
                final int changes = editor.getSize();
                if (changes > 10000) {
                    editor.setSender(sender);
                }
                if (!params.silent) {
                    sender.sendMessage(ChatColor.GREEN.toString() + changes + " " + (params.bct == BlockChangeType.ENTITIES ? "entities" : "blocks") + " found.");
                }
                if (changes == 0) {
                    if (!params.silent) {
                        sender.sendMessage(ChatColor.RED + "Rollback aborted");
                    }
                    return;
                }
                if (!params.silent && askRollbacks && sender instanceof Player && !logblock.getQuestioner().ask((Player) sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
                    sender.sendMessage(ChatColor.RED + "Rollback aborted");
                    return;
                }
                editor.start();
                if (!params.noCache) {
                    getSession(sender).lookupCache = editor.errors;
                }
                sender.sendMessage(ChatColor.GREEN + "Rollback finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
                if (!params.silent && askClearLogAfterRollback && logblock.hasPermission(sender, "logblock.clearlog") && sender instanceof Player) {
                    Thread.sleep(1000);
                    if (logblock.getQuestioner().ask((Player) sender, "Do you want to delete the rollbacked log?", "yes", "no").equals("yes")) {
                        params.silent = true;
                        new CommandClearLog(sender, params, false);
                    } else {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Clearlog cancelled");
                    }
                }
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[Rollback] " + params.getQuery() + ": ", ex);
                }
            } finally {
                close();
            }
        }
    }

    public class CommandRedo extends AbstractCommand {
        public CommandRedo(CommandSender sender, QueryParams params, boolean async) throws Exception {
            super(sender, params, async);
        }

        @Override
        public void run() {
            try {
                if (params.bct == BlockChangeType.CHAT) {
                    sender.sendMessage(ChatColor.RED + "Chat cannot be redone");
                    return;
                }
                if (params.bct == BlockChangeType.KILLS) {
                    sender.sendMessage(ChatColor.RED + "Kills cannot be redone");
                    return;
                }
                if (params.sum != SummarizationMode.NONE) {
                    sender.sendMessage(ChatColor.RED + "Cannot redo summarized changes");
                    return;
                }
                params.needDate = true;
                params.needCoords = true;
                params.needType = true;
                params.needData = true;
                params.needChestAccess = true;
                params.sum = SummarizationMode.NONE;
                conn = logblock.getConnection();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                state = conn.createStatement();
                if (!checkRestrictions(sender, params)) {
                    return;
                }
                rs = executeQuery(state, params.getQuery());
                if (!params.silent) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
                }
                final WorldEditor editor = new WorldEditor(logblock, params.world, params.forceReplace);
                WorldEditorEditFactory editFactory = new WorldEditorEditFactory(editor, params, false);
                while (rs.next()) {
                    editFactory.processRow(rs);
                }
                if (params.order == Order.ASC) {
                    editor.reverseRowOrder();
                }
                editor.sortRows(Order.ASC);
                final int changes = editor.getSize();
                if (!params.silent) {
                    sender.sendMessage(ChatColor.GREEN.toString() + changes + " " + (params.bct == BlockChangeType.ENTITIES ? "entities" : "blocks") + " found.");
                }
                if (changes == 0) {
                    if (!params.silent) {
                        sender.sendMessage(ChatColor.RED + "Redo aborted");
                    }
                    return;
                }
                if (!params.silent && askRedos && sender instanceof Player && !logblock.getQuestioner().ask((Player) sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
                    sender.sendMessage(ChatColor.RED + "Redo aborted");
                    return;
                }
                editor.start();
                sender.sendMessage(ChatColor.GREEN + "Redo finished successfully (" + editor.getElapsedTime() + " ms, " + editor.getSuccesses() + "/" + changes + " blocks" + (editor.getErrors() > 0 ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + (editor.getBlacklistCollisions() > 0 ? ", " + editor.getBlacklistCollisions() + " blacklist collisions" : "") + ")");
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[Redo] " + params.getQuery() + ": ", ex);
                }
            } finally {
                close();
            }
        }
    }

    public class CommandClearLog extends AbstractCommand {
        public CommandClearLog(CommandSender sender, QueryParams params, boolean async) throws Exception {
            super(sender, params, async);
        }

        @Override
        public void run() {
            try {
                conn = logblock.getConnection();
                conn.setAutoCommit(true);
                state = conn.createStatement();
                if (conn == null) {
                    sender.sendMessage(ChatColor.RED + "MySQL connection lost");
                    return;
                }
                if (!checkRestrictions(sender, params)) {
                    return;
                }
                if (params.sum != SummarizationMode.NONE) {
                    sender.sendMessage(ChatColor.RED + "Cannot summarize on ClearLog");
                    return;
                }
                String tableBase;
                String deleteFromTables;
                String tableName;
                params.needId = true;
                params.needDate = true;
                params.needPlayerId = true;
                if (params.bct == BlockChangeType.CHAT) {
                    params.needMessage = true;
                    tableBase = "lb-chat";
                    deleteFromTables = "`lb-chat` ";
                    tableName = "lb-chat";
                } else if (params.bct == BlockChangeType.KILLS) {
                    params.needWeapon = true;
                    params.needCoords = true;
                    tableBase = params.getTable();
                    deleteFromTables = "`" + tableBase + "-kills` ";
                    tableName = tableBase + "-kills";
                } else if (params.bct == BlockChangeType.ENTITIES || params.bct == BlockChangeType.ENTITIES_CREATED || params.bct == BlockChangeType.ENTITIES_KILLED) {
                    params.needType = true;
                    params.needCoords = true;
                    params.needData = true;
                    tableBase = params.getTable();
                    deleteFromTables = "`" + tableBase + "-entities` ";
                    tableName = tableBase + "-entities";
                } else {
                    params.needType = true;
                    params.needCoords = true;
                    params.needData = true;
                    params.needChestAccess = true;
                    tableBase = params.getTable();
                    deleteFromTables = "`" + tableBase + "-blocks`, `" + tableBase + "-state`, `" + tableBase + "-chestdata` ";
                    tableName = tableBase + "-blocks";
                }
                final File dumpFolder = new File(logblock.getDataFolder(), "dump");
                if (!dumpFolder.exists()) {
                    dumpFolder.mkdirs();
                }
                rs = state.executeQuery("SELECT count(*) " + params.getFrom() + params.getWhere());
                int deleted = rs.next() ? rs.getInt(1) : 0;
                rs.close();
                if (!params.silent && askClearLogs && sender instanceof Player) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Searching " + params.getTitle() + ":");
                    sender.sendMessage(ChatColor.GREEN.toString() + deleted + " entries found.");
                    if (deleted == 0 || !logblock.getQuestioner().ask((Player) sender, "Are you sure you want to continue?", "yes", "no").equals("yes")) {
                        sender.sendMessage(ChatColor.RED + "ClearLog aborted");
                        return;
                    }
                }
                if (deleted > 0 && dumpDeletedLog) {
                    final String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(System.currentTimeMillis());
                    try {
                        File outFile = new File(dumpFolder, (time + " " + tableName + " " + params.getTitle() + ".sql").replace(':', '.').replace('/', '_').replace('\\', '_'));
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outFile)), "UTF-8"));
                        try {
                            rs = state.executeQuery("SELECT " + params.getFields() + params.getFrom() + params.getWhere());
                            while (rs.next()) {
                                StringBuilder sb = new StringBuilder();
                                if (params.bct == BlockChangeType.CHAT) {
                                    sb.append("INSERT INTO `lb-chat` (`id`, `date`, `playerid`, `message`) VALUES (");
                                    sb.append(rs.getInt("id")).append(", FROM_UNIXTIME(");
                                    sb.append(rs.getTimestamp("date").getTime() / 1000).append("), ");
                                    sb.append(rs.getInt("playerid")).append(", '");
                                    sb.append(Utils.mysqlTextEscape(rs.getString("message")));
                                    sb.append("');\n");
                                } else if (params.bct == BlockChangeType.KILLS) {
                                    sb.append("INSERT INTO `").append(tableBase).append("-kills` (`id`, `date`, `killer`, `victim`, `weapon`, `x`, `y`, `z`) VALUES (");
                                    sb.append(rs.getInt("id")).append(", FROM_UNIXTIME(");
                                    sb.append(rs.getTimestamp("date").getTime() / 1000).append("), ");
                                    sb.append(rs.getInt("killerid")).append(", ");
                                    sb.append(rs.getInt("victimid")).append(", ");
                                    sb.append(rs.getInt("weapon")).append(", ");
                                    sb.append(rs.getInt("x")).append(", ");
                                    sb.append(rs.getInt("y")).append(", ");
                                    sb.append(rs.getInt("z"));
                                    sb.append(");\n");
                                } else if (params.bct == BlockChangeType.ENTITIES || params.bct == BlockChangeType.ENTITIES_CREATED || params.bct == BlockChangeType.ENTITIES_KILLED) {

                                } else {
                                    sb.append("INSERT INTO `").append(tableBase).append("-blocks` (`id`, `date`, `playerid`, `replaced`, `replacedData`, `type`, `typeData`, `x`, `y`, `z`) VALUES (");
                                    sb.append(rs.getInt("id")).append(", FROM_UNIXTIME(");
                                    sb.append(rs.getTimestamp("date").getTime() / 1000).append("), ");
                                    sb.append(rs.getInt("playerid")).append(", ");
                                    sb.append(rs.getInt("replaced")).append(", ");
                                    sb.append(rs.getInt("replacedData")).append(", ");
                                    sb.append(rs.getInt("type")).append(", ");
                                    sb.append(rs.getInt("typeData")).append(", ");
                                    sb.append(rs.getInt("x")).append(", ");
                                    sb.append(rs.getInt("y")).append(", ");
                                    sb.append(rs.getInt("z"));
                                    sb.append(");\n");
                                    byte[] replacedState = rs.getBytes("replacedState");
                                    byte[] typeState = rs.getBytes("typeState");
                                    if (replacedState != null || typeState != null) {
                                        sb.append("INSERT INTO `").append(tableBase).append("-state` (`id`, `replacedState`, `typeState`) VALUES (");
                                        sb.append(rs.getInt("id")).append(", ");
                                        sb.append(Utils.mysqlPrepareBytesForInsertAllowNull(replacedState)).append(", ");
                                        sb.append(Utils.mysqlPrepareBytesForInsertAllowNull(typeState));
                                        sb.append(");\n");
                                    }
                                    byte[] item = rs.getBytes("item");
                                    if (item != null) {
                                        sb.append("INSERT INTO `").append(tableBase).append("-chestdata` (`id`, `item`, `itemremove`, `itemtype`) VALUES (");
                                        sb.append(rs.getInt("id")).append(", ");
                                        sb.append(Utils.mysqlPrepareBytesForInsertAllowNull(item)).append(", ");
                                        sb.append(rs.getInt("itemremove")).append(", ");
                                        sb.append(rs.getInt("itemtype"));
                                        sb.append(");\n");
                                    }
                                }
                                writer.write(sb.toString());
                            }
                            rs.close();
                        } finally {
                            writer.close();
                        }
                    } catch (final SQLException ex) {
                        sender.sendMessage(ChatColor.RED + "Error while dumping log.");
                        logblock.getLogger().log(Level.SEVERE, "[ClearLog] Exception while dumping log: ", ex);
                        return;
                    }
                }
                if (deleted > 0) {
                    state.executeUpdate("DELETE " + deleteFromTables + params.getFrom() + params.getWhere());
                    if (params.bct == BlockChangeType.ENTITIES || params.bct == BlockChangeType.ENTITIES_CREATED || params.bct == BlockChangeType.ENTITIES_KILLED) {
                        state.executeUpdate("DELETE `" + tableBase + "-entityids` FROM `" + tableBase + "-entityids` LEFT JOIN `" + tableBase + "-entities` USING (entityid) WHERE `" + tableBase + "-entities`.entityid IS NULL");
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Cleared out table " + tableName + ". Deleted " + deleted + " entries.");
            } catch (final Exception ex) {
                if (logblock.isCompletelyEnabled() || !(ex instanceof SQLException)) {
                    sender.sendMessage(ChatColor.RED + "Exception, check error log");
                    logblock.getLogger().log(Level.SEVERE, "[ClearLog] Exception: ", ex);
                }
            } finally {
                close();
            }
        }
    }

    private ResultSet executeQuery(Statement state, String query) throws SQLException {
        if (Config.debug) {
            long startTime = System.currentTimeMillis();
            ResultSet rs = state.executeQuery(query);
            logblock.getLogger().log(Level.INFO, "[LogBlock Debug] Time Taken: " + (System.currentTimeMillis() - startTime) + " milliseconds. Query: " + query);
            return rs;
        } else {
            return state.executeQuery(query);
        }
    }

    private static List<String> argsToList(String[] arr, int offset) {
        final List<String> list = new ArrayList<>(Arrays.asList(arr));
        for (int i = 0; i < offset; i++) {
            list.remove(0);
        }
        return list;
    }
}
