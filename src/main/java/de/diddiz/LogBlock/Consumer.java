package de.diddiz.LogBlock;

import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.*;
import static de.diddiz.util.BukkitUtils.*;
import static org.bukkit.Bukkit.getLogger;

public class Consumer extends TimerTask
{
	private final Queue<Row> queue = new LinkedBlockingQueue<Row>();
	private final Set<String> failedPlayers = new HashSet<String>();
	private final LogBlock logblock;
	private final Map<String, Integer> playerIds = new HashMap<String, Integer>();
	private final Lock lock = new ReentrantLock();

	Consumer(LogBlock logblock) {
		this.logblock = logblock;
		try {
			Class.forName("PlayerLeaveRow");
		} catch (final ClassNotFoundException ex) {
		}
	}

	/**
	 * Logs any block change. Don't try to combine broken and placed blocks. Queue two block changes or use the queueBLockReplace methods.
	 */
	public void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data) {
		queueBlock(playerName, loc, typeBefore, typeAfter, data, null, null);
	}

	/**
	 * Logs a block break. The type afterwards is assumed to be o (air).
	 *
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockBreak(String playerName, BlockState before) {
		queueBlockBreak(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData());
	}

	/**
	 * Logs a block break. The block type afterwards is assumed to be o (air).
	 */
	public void queueBlockBreak(String playerName, Location loc, int typeBefore, byte dataBefore) {
		queueBlock(playerName, loc, typeBefore, 0, dataBefore);
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 *
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockPlace(String playerName, BlockState after) {
		queueBlockPlace(playerName, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), after.getBlock().getTypeId(), after.getBlock().getData());
	}

	/**
	 * Logs a block place. The block type before is assumed to be o (air).
	 */
	public void queueBlockPlace(String playerName, Location loc, int type, byte data) {
		queueBlock(playerName, loc, 0, type, data);
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, BlockState after) {
		queueBlockReplace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), after.getTypeId(), after.getRawData());
	}

	/**
	 * @param before
	 * Blockstate of the block before actually being destroyed.
	 */
	public void queueBlockReplace(String playerName, BlockState before, int typeAfter, byte dataAfter) {
		queueBlockReplace(playerName, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getTypeId(), before.getRawData(), typeAfter, dataAfter);
	}

	/**
	 * @param after
	 * Blockstate of the block after actually being placed.
	 */
	public void queueBlockReplace(String playerName, int typeBefore, byte dataBefore, BlockState after) {
		queueBlockReplace(playerName, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), typeBefore, dataBefore, after.getTypeId(), after.getRawData());
	}

	public void queueBlockReplace(String playerName, Location loc, int typeBefore, byte dataBefore, int typeAfter, byte dataAfter) {
		if (dataBefore == 0 && (typeBefore != typeAfter))
			queueBlock(playerName, loc, typeBefore, typeAfter, dataAfter);
		else {
			queueBlockBreak(playerName, loc, typeBefore, dataBefore);
			queueBlockPlace(playerName, loc, typeAfter, dataAfter);
		}
	}

	/**
	 * @param container
	 * The respective container. Must be an instance of Chest, Dispencer or Furnace.
	 */
	public void queueChestAccess(String playerName, BlockState container, short itemType, short itemAmount, byte itemData) {
		if (!(container instanceof InventoryHolder))
			return;
		queueChestAccess(playerName, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), itemType, itemAmount, itemData);
	}

	/**
	 * @param type
	 * Type id of the container. Must be 63 or 68.
	 */
	public void queueChestAccess(String playerName, Location loc, int type, short itemType, short itemAmount, byte itemData) {
		queueBlock(playerName, loc, type, type, (byte)0, null, new ChestAccess(itemType, itemAmount, itemData));
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 *
	 * @param container
	 * Must be instanceof InventoryHolder
	 */
	public void queueContainerBreak(String playerName, BlockState container) {
		if (!(container instanceof InventoryHolder))
			return;
		queueContainerBreak(playerName, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getTypeId(), container.getRawData(), ((InventoryHolder)container).getInventory());
	}

	/**
	 * Logs a container block break. The block type before is assumed to be o (air). All content is assumed to be taken.
	 */
	public void queueContainerBreak(String playerName, Location loc, int type, byte data, Inventory inv) {
		final ItemStack[] items = compressInventory(inv.getContents());
		for (final ItemStack item : items)
			queueChestAccess(playerName, loc, type, (short)item.getTypeId(), (short)(item.getAmount() * -1), rawData(item));
		queueBlockBreak(playerName, loc, type, data);
	}

	/**
	 * @param killer
	 * Can' be null
	 * @param victim
	 * Can' be null
	 */
	public void queueKill(Entity killer, Entity victim) {
		if (killer == null || victim == null)
			return;
		int weapon = 0;
		if (killer instanceof Player && ((Player)killer).getItemInHand() != null)
			weapon = ((Player)killer).getItemInHand().getTypeId();
		queueKill(victim.getLocation(), entityName(killer), entityName(victim), weapon);
	}

	/**
	 * @param world
	 * World the victim was inside.
	 * @param killerName
	 * Name of the killer. Can be null.
	 * @param victimName
	 * Name of the victim. Can't be null.
	 * @param weapon
	 * Item id of the weapon. 0 for no weapon.
	 * @deprecated Use {@link #queueKill(Location,String,String,int)} instead
	 */
	@Deprecated
	public void queueKill(World world, String killerName, String victimName, int weapon) {
		queueKill(new Location(world, 0, 0, 0), killerName, victimName, weapon);
	}

	/**
	 * @param location
	 * Location of the victim.
	 * @param killerName
	 * Name of the killer. Can be null.
	 * @param victimName
	 * Name of the victim. Can't be null.
	 * @param weapon
	 * Item id of the weapon. 0 for no weapon.
	 */
	public void queueKill(Location location, String killerName, String victimName, int weapon) {
		if (victimName == null || !isLogged(location.getWorld()))
			return;
		queue.add(new KillRow(location, killerName == null ? null : killerName.replaceAll("[^a-zA-Z0-9_]", ""), victimName.replaceAll("[^a-zA-Z0-9_]", ""), weapon));
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignBreak(String playerName, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(playerName, loc, type, 0, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignBreak(String playerName, Sign sign) {
		queueSignBreak(playerName, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	/**
	 * @param type
	 * Type of the sign. Must be 63 or 68.
	 * @param lines
	 * The four lines on the sign.
	 */
	public void queueSignPlace(String playerName, Location loc, int type, byte data, String[] lines) {
		if (type != 63 && type != 68 || lines == null || lines.length != 4)
			return;
		queueBlock(playerName, loc, 0, type, data, lines[0] + "\0" + lines[1] + "\0" + lines[2] + "\0" + lines[3], null);
	}

	public void queueSignPlace(String playerName, Sign sign) {
		queueSignPlace(playerName, new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()), sign.getTypeId(), sign.getRawData(), sign.getLines());
	}

	public void queueChat(String player, String message) {
		for (String ignored : Config.ignoredChat) {
			if (message.startsWith(ignored)) {
				return;
			}
		}
		queue.add(new ChatRow(player, message));
	}

	public void queueJoin(Player player) {
		queue.add(new PlayerJoinRow(player));
	}

	public void queueLeave(Player player) {
		queue.add(new PlayerLeaveRow(player));
	}

	@Override
	public void run() {
		if (queue.isEmpty() || !lock.tryLock())
			return;
		final Connection conn = logblock.getConnection();
		Statement state = null;
		if (Config.queueWarningSize > 0 && queue.size() >= Config.queueWarningSize) {
			getLogger().info("[Consumer] Queue overloaded. Size: " + getQueueSize());
		}

		try {
			if (conn == null)
				return;
			conn.setAutoCommit(false);
			state = conn.createStatement();
			final long start = System.currentTimeMillis();
			int count = 0;
			process:
			while (!queue.isEmpty() && (System.currentTimeMillis() - start < timePerRun || count < forceToProcessAtLeast)) {
				final Row r = queue.poll();
				if (r == null)
					continue;
				for (final String player : r.getPlayers()) {
					if (!playerIds.containsKey(player)) {
						if (!addPlayer(state, player)) {
							if (!failedPlayers.contains(player)) {
								failedPlayers.add(player);
								getLogger().warning("[Consumer] Failed to add player " + player);
							}
							continue process;
						}
					}
				}
				if (r instanceof PreparedStatementRow) {
					PreparedStatementRow PSRow = (PreparedStatementRow) r;
					PSRow.setConnection(conn);
					try {
						PSRow.executeStatements();
					} catch (final SQLException ex) {
						getLogger().log(Level.SEVERE, "[Consumer] SQL exception on insertion: ", ex);
						break;
					}
				} else {
					for (final String insert : r.getInserts())
						try {
							state.execute(insert);
						} catch (final SQLException ex) {
							getLogger().log(Level.SEVERE, "[Consumer] SQL exception on " + insert + ": ", ex);
							break process;
						}
				}

				count++;
			}
			conn.commit();
		} catch (final SQLException ex) {
			getLogger().log(Level.SEVERE, "[Consumer] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (final SQLException ex) {
				getLogger().log(Level.SEVERE, "[Consumer] SQL exception on close", ex);
			}
			lock.unlock();
		}
	}

	public void writeToFile() throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		final Set<String> insertedPlayers = new HashSet<String>();
		int counter = 0;
		new File("plugins/LogBlock/import/").mkdirs();
		PrintWriter writer = new PrintWriter(new File("plugins/LogBlock/import/queue-" + time + "-0.sql"));
		while (!queue.isEmpty()) {
			final Row r = queue.poll();
			if (r == null)
				continue;
			for (final String player : r.getPlayers())
				if (!playerIds.containsKey(player) && !insertedPlayers.contains(player)) {
					writer.println("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + player + "');");
					insertedPlayers.add(player);
				}
			for (final String insert : r.getInserts())
				writer.println(insert);
			counter++;
			if (counter % 1000 == 0) {
				writer.close();
				writer = new PrintWriter(new File("plugins/LogBlock/import/queue-" + time + "-" + counter / 1000 + ".sql"));
			}
		}
		writer.close();
	}

	int getQueueSize() {
		return queue.size();
	}

	static boolean hide(Player player) {
		final String playerName = player.getName().toLowerCase();
		if (hiddenPlayers.contains(playerName)) {
			hiddenPlayers.remove(playerName);
			return false;
		}
		hiddenPlayers.add(playerName);
		return true;
	}

	private boolean addPlayer(Statement state, String playerName) throws SQLException {
		state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + playerName + "')");
		final ResultSet rs = state.executeQuery("SELECT playerid FROM `lb-players` WHERE playername = '" + playerName + "'");
		if (rs.next())
			playerIds.put(playerName, rs.getInt(1));
		rs.close();
		return playerIds.containsKey(playerName);
	}

	private void queueBlock(String playerName, Location loc, int typeBefore, int typeAfter, byte data, String signtext, ChestAccess ca) {

		if (Config.fireCustomEvents) {
			// Create and call the event
			BlockChangePreLogEvent event = new BlockChangePreLogEvent(playerName, loc, typeBefore, typeAfter, data, signtext, ca);
			logblock.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) return;

			// Update variables
			playerName = event.getOwner();
			loc = event.getLocation();
			typeBefore = event.getTypeBefore();
			typeAfter = event.getTypeAfter();
			data = event.getData();
			signtext = event.getSignText();
			ca = event.getChestAccess();
		}
		// Do this last so LogBlock still has final say in what is being added
		if (playerName == null || loc == null || typeBefore < 0 || typeAfter < 0 || (Config.safetyIdCheck && (typeBefore > 255 || typeAfter > 255)) || hiddenPlayers.contains(playerName.toLowerCase()) || !isLogged(loc.getWorld()) || typeBefore != typeAfter && hiddenBlocks.contains(typeBefore) && hiddenBlocks.contains(typeAfter)) return;
		queue.add(new BlockRow(loc, playerName.replaceAll("[^a-zA-Z0-9_]", ""), typeBefore, typeAfter, data, signtext, ca));
	}

	private String playerID(String playerName) {
		if (playerName == null)
			return "NULL";
		final Integer id = playerIds.get(playerName);
		if (id != null)
			return id.toString();
		return "(SELECT playerid FROM `lb-players` WHERE playername = '" + playerName + "')";
	}

	private Integer playerIDAsInt(String playerName) {
		if (playerName == null) {
			return null;
		}
		return playerIds.get(playerName);
	}

	private static interface Row
	{
		String[] getInserts();

		String[] getPlayers();
	}

	private interface PreparedStatementRow extends Row
	{

		abstract void setConnection(Connection connection);
		abstract void executeStatements() throws SQLException;

	}

	private class BlockRow extends BlockChange implements PreparedStatementRow
	{
		private Connection connection;

		public BlockRow(Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
			super(System.currentTimeMillis() / 1000, loc, playerName, replaced, type, data, signtext, ca);
		}

		@Override
		public String[] getInserts() {
			final String table = getWorldConfig(loc.getWorld()).table;
			final String[] inserts = new String[ca != null || signtext != null ? 2 : 1];
			inserts[0] = "INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(playerName) + ", " + replaced + ", " + type + ", " + data + ", '" + loc.getBlockX() + "', " + loc.getBlockY() + ", '" + loc.getBlockZ() + "');";
			if (signtext != null) {
				inserts[1] = "INSERT INTO `" + table + "-sign` (id, signtext) values (LAST_INSERT_ID(), '" + signtext.replace("\\", "\\\\").replace("'", "\\'") + "');";
			}
			else if (ca != null)
				inserts[1] = "INSERT INTO `" + table + "-chest` (id, itemtype, itemamount, itemdata) values (LAST_INSERT_ID(), " + ca.itemType + ", " + ca.itemAmount + ", " + ca.itemData + ");";
			return inserts;
		}

		@Override
		public String[] getPlayers() {
			return new String[]{playerName};
		}

		@Override
		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void executeStatements() throws SQLException {
			final String table = getWorldConfig(loc.getWorld()).table;

			PreparedStatement ps1 = null;
			PreparedStatement ps = null;
			try {
				ps1 = connection.prepareStatement("INSERT INTO `" + table + "` (date, playerid, replaced, type, data, x, y, z) VALUES(FROM_UNIXTIME(?), " + playerID(playerName) + ", ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				ps1.setLong(1, date );
				ps1.setInt(2, replaced);
				ps1.setInt(3, type);
				ps1.setInt(4, data);
				ps1.setInt(5, loc.getBlockX());
				ps1.setInt(6, loc.getBlockY());
				ps1.setInt(7, loc.getBlockZ());
				ps1.executeUpdate();
	
				int id;
				ResultSet rs = ps1.getGeneratedKeys();
				rs.next();
				id = rs.getInt(1);
	
				if (signtext != null) {
					ps = connection.prepareStatement("INSERT INTO `" + table + "-sign` (signtext, id) VALUES(?, ?)");
					ps.setString(1, signtext);
					ps.setInt(2, id);
					ps.executeUpdate();
				} else if (ca != null) {
					ps = connection.prepareStatement("INSERT INTO `" + table + "-chest` (itemtype, itemamount, itemdata, id) values (?, ?, ?, ?)");
					ps.setInt(1, ca.itemType);
					ps.setInt(2, ca.itemAmount);
					ps.setInt(3, ca.itemData);
					ps.setInt(4, id);
					ps.executeUpdate();
				}
			}
			// we intentionally do not catch SQLException, it is thrown to the caller
			finally {
				// individual try/catch here, though ugly, prevents resource leaks
				if( ps1 != null ) {
					try {
						ps1.close();
					}
					catch(SQLException e) {
						// ideally should log to logger, none is available in this class
						// at the time of this writing, so I'll leave that to the plugin
						// maintainers to integrate if they wish
						e.printStackTrace();
					}
				}
				
				if( ps != null ) {
					try {
						ps.close();
					}
					catch(SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class KillRow implements Row
	{
		final long date;
		final String killer, victim;
		final int weapon;
		final Location loc;

		KillRow(Location loc, String attacker, String defender, int weapon) {
			date = System.currentTimeMillis() / 1000;
			this.loc = loc;
			killer = attacker;
			victim = defender;
			this.weapon = weapon;
		}

		@Override
		public String[] getInserts() {
			return new String[]{"INSERT INTO `" + getWorldConfig(loc.getWorld()).table + "-kills` (date, killer, victim, weapon, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(killer) + ", " + playerID(victim) + ", " + weapon + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ");"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{killer, victim};
		}
	}

	private class ChatRow extends ChatMessage implements PreparedStatementRow
	{
		private Connection connection;

		ChatRow(String player, String message) {
			super(player, message);
		}

		@Override
		public String[] getInserts() {
			return new String[]{"INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(playerName) + ", '" + message.replace("\\", "\\\\").replace("'", "\\'") + "');"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{playerName};
		}

		@Override
		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void executeStatements() throws SQLException {
			boolean noID = false;
			Integer id;

			String sql = "INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(?), ";
			if ((id = playerIDAsInt(playerName)) == null) {
				noID = true;
				sql += playerID(playerName) + ", ";
			} else {
				sql += "?, ";
			}
			sql += "?)";
			
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement(sql);
				ps.setLong(1, date);
				if (!noID) {
					ps.setInt(2, id);
					ps.setString(3, message);
				} else {
					ps.setString(2, message);
				}
				ps.execute();
			}
			// we intentionally do not catch SQLException, it is thrown to the caller
			finally {
				if( ps != null ) {
					try {
						ps.close();
					}
					catch(SQLException e) {
						// should print to a Logger instead if one is ever added to this class
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class PlayerJoinRow implements Row
	{
		private final String playerName;
		private final long lastLogin;
		private final String ip;

		PlayerJoinRow(Player player) {
			playerName = player.getName();
			lastLogin = System.currentTimeMillis() / 1000;
			ip = player.getAddress().toString().replace("'", "\\'");
		}

		@Override
		public String[] getInserts() {
			return new String[]{"UPDATE `lb-players` SET lastlogin = FROM_UNIXTIME(" + lastLogin + "), firstlogin = IF(firstlogin = 0, FROM_UNIXTIME(" + lastLogin + "), firstlogin), ip = '" + ip + "' WHERE " + (playerIds.containsKey(playerName) ? "playerid = " + playerIds.get(playerName) : "playerName = '" + playerName + "'") + ";"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{playerName};
		}
	}

	private class PlayerLeaveRow implements Row
	{
		private final String playerName;
		private final long leaveTime;

		PlayerLeaveRow(Player player) {
			playerName = player.getName();
			leaveTime = System.currentTimeMillis() / 1000;
		}

		@Override
		public String[] getInserts() {
			return new String[]{"UPDATE `lb-players` SET onlinetime = onlinetime + TIMESTAMPDIFF(SECOND, lastlogin, FROM_UNIXTIME('" + leaveTime + "')) WHERE lastlogin > 0 && " + (playerIds.containsKey(playerName) ? "playerid = " + playerIds.get(playerName) : "playerName = '" + playerName + "'") + ";"};
		}

		@Override
		public String[] getPlayers() {
			return new String[]{playerName};
		}
	}
}
