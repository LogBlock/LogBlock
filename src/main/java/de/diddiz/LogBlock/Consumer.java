package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.hiddenBlocks;
import static de.diddiz.LogBlock.config.Config.hiddenPlayers;
import static de.diddiz.LogBlock.config.Config.isLogged;
import static de.diddiz.LogBlock.config.Config.logPlayerInfo;
import static de.diddiz.LogBlock.util.BukkitUtils.compressInventory;
import static de.diddiz.LogBlock.util.BukkitUtils.itemIDfromProjectileEntity;
import static de.diddiz.LogBlock.util.Utils.mysqlTextEscape;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import de.diddiz.LogBlock.EntityChange.EntityChangeType;
import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import de.diddiz.LogBlock.events.EntityChangePreLogEvent;
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.ItemStackAndAmount;
import de.diddiz.LogBlock.util.Utils;

public class Consumer extends Thread {
    private static final int MAX_SHUTDOWN_TIME_MILLIS = 20000;
    private static final int WAIT_FOR_CONNECTION_TIME_MILLIS = 10000;
    private static final int RETURN_IDLE_CONNECTION_TIME_MILLIS = 120000;
    private static final int RETRIES_ON_UNKNOWN_CONNECTION_ERROR = 2;

    private final Deque<Row> queue = new ArrayDeque<>();
    private final LogBlock logblock;
    private final Map<Actor, Integer> playerIds = new HashMap<>();
    private final Map<Actor, Integer> uncommitedPlayerIds = new HashMap<>();
    private final Map<World, Map<UUID, Long>> uncommitedEntityIds = new HashMap<>();

    private long addEntryCounter;
    private long nextWarnCounter;

    private boolean shutdown;
    private long shutdownInitialized;

    Consumer(LogBlock logblock) {
        this.logblock = logblock;
        PlayerLeaveRow.class.getName(); // preload this class
        setName("Logblock-Consumer");
    }

    public LogBlock getLogblock() {
        return logblock;
    }

    /**
     * Logs any block change. Don't try to combine broken and placed blocks. Queue two block changes or use the queueBLockReplace methods.
     *
     * @param actor
     *            Actor responsible for making the change
     * @param loc
     *            Location of the block change
     * @param typeBefore
     *            BlockData of the block before the change
     * @param typeAfter
     *            BlockData of the block after the change
     */
    public void queueBlock(Actor actor, Location loc, BlockData typeBefore, BlockData typeAfter) {
        queueBlock(actor, loc, typeBefore, typeAfter, null, null, null);
    }

    /**
     * Logs a block break. The type afterwards is assumed to be air.
     *
     * @param actor
     *            Actor responsible for breaking the block
     * @param before
     *            BlockState of the block before actually being destroyed.
     */
    public void queueBlockBreak(Actor actor, BlockState before) {
        queueBlock(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getBlockData(), null, BlockStateCodecs.serialize(before), null, null);
    }

    /**
     * Logs a block break. The block type afterwards is assumed to be air.
     *
     * @param actor
     *            Actor responsible for the block break
     * @param loc
     *            Location of the broken block
     * @param typeBefore
     *            BlockData of the block before the break
     */
    public void queueBlockBreak(Actor actor, Location loc, BlockData typeBefore) {
        queueBlock(actor, loc, typeBefore, null);
    }

    /**
     * Logs a block place. The block type before is assumed to be air.
     *
     * @param actor
     *            Actor responsible for placing the block
     * @param after
     *            BlockState of the block after actually being placed.
     */
    public void queueBlockPlace(Actor actor, BlockState after) {
        queueBlock(actor, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), null, after.getBlockData(), null, BlockStateCodecs.serialize(after), null);
    }

    /**
     * Logs a block place. The block type before is assumed to be air.
     *
     * @param actor
     *            Actor responsible for placing the block
     * @param loc
     *            Location of the placed block
     * @param type
     *            BlockData of the placed block
     */
    public void queueBlockPlace(Actor actor, Location loc, BlockData type) {
        queueBlock(actor, loc, null, type);
    }

    /**
     * Logs a block being replaced from the before and after {@link org.bukkit.block.BlockState}s
     *
     * @param actor
     *            Actor responsible for replacing the block
     * @param before
     *            BlockState of the block before actually being destroyed.
     * @param after
     *            BlockState of the block after actually being placed.
     */
    public void queueBlockReplace(Actor actor, BlockState before, BlockState after) {
        queueBlock(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getBlockData(), after.getBlockData(), BlockStateCodecs.serialize(before), BlockStateCodecs.serialize(after), null);
    }

    /**
     * Logs a block being replaced from the before {@link org.bukkit.block.BlockState} and the type and data after
     *
     * @param actor
     *            Actor responsible for replacing the block
     * @param before
     *            BlockState of the block before being replaced.
     * @param typeAfter
     *            BlockData of the block after being replaced
     */
    public void queueBlockReplace(Actor actor, BlockState before, BlockData typeAfter) {
        queueBlock(actor, new Location(before.getWorld(), before.getX(), before.getY(), before.getZ()), before.getBlockData(), typeAfter, BlockStateCodecs.serialize(before), null, null);
    }

    /**
     * Logs a block being replaced from the type and data before and the {@link org.bukkit.block.BlockState} after
     *
     * @param actor
     *            Actor responsible for replacing the block
     * @param typeBefore
     *            BlockData of the block before being replaced
     * @param after
     *            BlockState of the block after actually being placed.
     */
    public void queueBlockReplace(Actor actor, BlockData typeBefore, BlockState after) {
        queueBlock(actor, new Location(after.getWorld(), after.getX(), after.getY(), after.getZ()), typeBefore, after.getBlockData(), null, BlockStateCodecs.serialize(after), null);
    }

    public void queueBlockReplace(Actor actor, Location loc, BlockData typeBefore, BlockData typeAfter) {
        queueBlock(actor, loc, typeBefore, typeAfter, null, null, null);
    }

    /**
     * Logs an actor interacting with a container block's inventory
     *
     * @param actor
     *            The actor interacting with the container
     * @param container
     *            The respective container. Must be an instance of an InventoryHolder.
     * @param itemStack
     *            Item taken/stored, including amount
     * @param remove
     *            true if the item was removed
     */
    public void queueChestAccess(Actor actor, BlockState container, ItemStackAndAmount itemStack, boolean remove) {
        if (!(container instanceof InventoryHolder)) {
            throw new IllegalArgumentException("Container must be instanceof InventoryHolder");
        }
        queueChestAccess(actor, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getBlockData(), itemStack, remove);
    }

    /**
     * Logs an actor interacting with a container block's inventory
     *
     * @param actor
     *            The actor interacting with the container
     * @param loc
     *            The location of the container block
     * @param type
     *            BlockData of the container.
     * @param itemStack
     *            Item taken/stored, including amount
     * @param remove
     *            true if the item was removed
     */
    public void queueChestAccess(Actor actor, Location loc, BlockData type, ItemStackAndAmount itemStack, boolean remove) {
        queueBlock(actor, loc, type, type, null, null, new ChestAccess(itemStack, remove, MaterialConverter.getOrAddMaterialId(itemStack.stack().getType())));
    }

    /**
     * Logs a container block break. The block type before is assumed to be air. All content is assumed to be taken.
     *
     * @param actor
     *            The actor breaking the container
     * @param container
     *            Must be an instance of InventoryHolder
     */
    public void queueContainerBreak(Actor actor, BlockState container) {
        if (!(container instanceof InventoryHolder)) {
            return;
        }
        queueContainerBreak(actor, new Location(container.getWorld(), container.getX(), container.getY(), container.getZ()), container.getBlockData(), ((InventoryHolder) container).getInventory());
    }

    /**
     * Logs a container block break. The block type before is assumed to be air. All content is assumed to be taken.
     *
     * @param actor
     *            The actor responsible for breaking the container
     * @param loc
     *            The location of the inventory block
     * @param type
     *            BlockData of the container block
     * @param inv
     *            The inventory of the container block
     */
    public void queueContainerBreak(Actor actor, Location loc, BlockData type, Inventory inv) {
        final Collection<ItemStackAndAmount> items = compressInventory(inv.getContents());
        for (final ItemStackAndAmount item : items) {
            queueChestAccess(actor, loc, type, item, true);
        }
        queueBlockBreak(actor, loc, type);
    }

    /**
     * @param killer
     *            Can't be null
     * @param victim
     *            Can't be null
     */
    public void queueKill(Entity killer, Entity victim) {
        if (killer == null || victim == null) {
            return;
        }
        ItemStack weapon = null;
        Actor killerActor = Actor.actorFromEntity(killer);
        // If it's a projectile kill we want to manually assign the weapon, so check for player before converting a projectile to its source
        if (killer instanceof Player && ((Player) killer).getInventory().getItemInMainHand() != null) {
            weapon = ((Player) killer).getInventory().getItemInMainHand();
        }
        if (killer instanceof Projectile) {
            Material projectileMaterial = itemIDfromProjectileEntity(killer);
            weapon = projectileMaterial == null ? null : new ItemStack(projectileMaterial);
            ProjectileSource ps = ((Projectile) killer).getShooter();
            if (ps == null) {
                killerActor = Actor.actorFromEntity(killer);
            } else {
                killerActor = Actor.actorFromProjectileSource(ps);
            }
        }

        queueKill(victim.getLocation(), killerActor, Actor.actorFromEntity(victim), weapon);
    }

    /**
     * This form should only be used when the killer is not an entity e.g. for fall or suffocation damage
     *
     * @param killer
     *            Can't be null
     * @param victim
     *            Can't be null
     */
    public void queueKill(Actor killer, Entity victim) {
        if (killer == null || victim == null) {
            return;
        }
        queueKill(victim.getLocation(), killer, Actor.actorFromEntity(victim), null);
    }

    /**
     * @param location
     *            Location of the victim.
     * @param killer
     *            Killer Actor. Can be null.
     * @param victim
     *            Victim Actor. Can't be null.
     * @param weapon
     *            Item of the weapon. null for no weapon.
     */
    public void queueKill(Location location, Actor killer, Actor victim, ItemStack weapon) {
        if (victim == null || !isLogged(location.getWorld())) {
            return;
        }
        addQueueLast(new KillRow(location, killer == null ? null : killer, victim, weapon == null ? 0 : MaterialConverter.getOrAddMaterialId(weapon.getType())));
    }

    public void queueChat(Actor player, String message) {
        if (!Config.ignoredChat.isEmpty()) {
            String lowerCaseMessage = message.toLowerCase();
            for (String ignored : Config.ignoredChat) {
                if (lowerCaseMessage.startsWith(ignored)) {
                    return;
                }
            }
        }
        if (hiddenPlayers.contains(player.getName().toLowerCase())) {
            return;
        }
        while (message.length() > 256) {
            addQueueLast(new ChatRow(player, message.substring(0, 256)));
            message = message.substring(256);
        }
        addQueueLast(new ChatRow(player, message));
    }

    public void queueJoin(Player player) {
        addQueueLast(new PlayerJoinRow(player));
    }

    public void queueLeave(Player player, long onlineTime) {
        addQueueLast(new PlayerLeaveRow(player, onlineTime));
    }

    public void shutdown() {
        synchronized (queue) {
            shutdown = true;
            shutdownInitialized = System.currentTimeMillis();
            queue.notifyAll();
        }
        while (isAlive()) {
            try {
                join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    @Override
    public void run() {
        ArrayList<Row> currentRows = new ArrayList<>();
        Connection conn = null;
        BatchHelper batchHelper = new BatchHelper();
        int lastCommitsFailed = 0;
        while (true) {
            try {
                if (conn == null) {
                    batchHelper.reset();
                    conn = logblock.getConnection();
                    if (conn != null) {
                        // initialize connection
                        conn.setAutoCommit(false);
                    } else {
                        // we did not get a connection
                        boolean wantsShutdown;
                        synchronized (queue) {
                            wantsShutdown = shutdown;
                        }
                        if (wantsShutdown) {
                            // lets give up
                            break;
                        }
                        // wait for a connection
                        logblock.getLogger().severe("[Consumer] Could not connect to the database!");
                        try {
                            Thread.sleep(WAIT_FOR_CONNECTION_TIME_MILLIS);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        continue;
                    }
                }
                Row r;
                boolean processBatch = false;
                synchronized (queue) {
                    if (shutdown) {
                        // Give this thread some time to process the remaining entries
                        if (queue.isEmpty() || System.currentTimeMillis() - shutdownInitialized > MAX_SHUTDOWN_TIME_MILLIS) {
                            if (currentRows.isEmpty()) {
                                break;
                            } else {
                                processBatch = true;
                            }
                        }
                    }
                    r = queue.pollFirst();
                    if (r == null) {
                        try {
                            if (currentRows.isEmpty() && !shutdown) {
                                // nothing to do for us
                                // wait some time before closing the connection
                                queue.wait(RETURN_IDLE_CONNECTION_TIME_MILLIS);
                                // if there is still nothing to do, close the connection and go to sleep
                                if (queue.isEmpty() && !shutdown) {
                                    try {
                                        conn.close();
                                    } catch (Exception e) {
                                        // ignored
                                    }
                                    conn = null;
                                    queue.wait();
                                }
                            } else {
                                processBatch = true;
                            }
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
                if (r != null) {
                    boolean failOnActors = false;
                    for (final Actor actor : r.getActors()) {
                        if (playerIDAsIntIncludeUncommited(actor) == null) {
                            if (!addPlayer(conn, actor)) {
                                failOnActors = true; // skip this row
                            }
                        }
                    }
                    if (!failOnActors) {
                        currentRows.add(r);
                        r.process(conn, batchHelper);
                    }
                }
                if (currentRows.size() >= Math.max((processBatch ? 1 : (Config.forceToProcessAtLeast * 10)), 1)) {
                    batchHelper.processStatements(conn);
                    conn.commit();
                    currentRows.clear();
                    playerIds.putAll(uncommitedPlayerIds);
                    uncommitedPlayerIds.clear();
                    uncommitedEntityIds.clear();
                    lastCommitsFailed = 0;
                }
            } catch (Exception e) {
                boolean retry = lastCommitsFailed < RETRIES_ON_UNKNOWN_CONNECTION_ERROR;
                String state = "unknown";
                if (e instanceof SQLException) {
                    // Retry on network errors: SQLSTATE = 08S01 08001 08004 HY000 40001
                    state = ((SQLException) e).getSQLState();
                    retry = retry || (state != null && (state.equals("08S01") || state.equals("08001") || state.equals("08004") || state.equals("HY000") || state.equals("40001")));
                }
                lastCommitsFailed += 1;
                if (retry) {
                    logblock.getLogger().log(Level.WARNING, "[Consumer] Database connection lost, reconnecting! SQLState: " + state);
                    // readd rows to the queue
                    synchronized (queue) {
                        while (!currentRows.isEmpty()) {
                            queue.addFirst(currentRows.remove(currentRows.size() - 1));
                        }
                    }
                } else {
                    logblock.getLogger().log(Level.SEVERE, "[Consumer] Could not insert entries! SQLState: " + state, e);
                }
                currentRows.clear();
                batchHelper.reset();
                uncommitedPlayerIds.clear();
                uncommitedEntityIds.clear();
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e1) {
                        // ignore
                    }
                }
                conn = null;
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e1) {
                // ignore
            }
        }

        // readd to queue - this can be saved later
        synchronized (queue) {
            while (!currentRows.isEmpty()) {
                queue.addFirst(currentRows.remove(currentRows.size() - 1));
            }
        }
    }

    public void writeToFile() throws FileNotFoundException {
        final long time = System.currentTimeMillis();
        final Set<Actor> insertedPlayers = new HashSet<>();
        int counter = 0;
        final File importDir = new File(logblock.getDataFolder(), "import");
        importDir.mkdirs();
        PrintWriter writer = new PrintWriter(new File(importDir, "queue-" + time + "-0.sql"));
        while (!isQueueEmpty()) {
            final Row r = pollQueueFirst();
            if (r == null) {
                continue;
            }
            for (final Actor actor : r.getActors()) {
                if (!playerIds.containsKey(actor) && !insertedPlayers.contains(actor)) {
                    // Odd query contruction is to work around innodb auto increment behaviour - bug #492
                    writer.println("INSERT IGNORE INTO `lb-players` (playername,UUID) SELECT '" + mysqlTextEscape(actor.getName()) + "','" + mysqlTextEscape(actor.getUUID()) + "' FROM `lb-players` WHERE NOT EXISTS (SELECT NULL FROM `lb-players` WHERE UUID = '" + mysqlTextEscape(actor.getUUID()) + "') LIMIT 1;");
                    insertedPlayers.add(actor);
                }
            }
            for (final String insert : r.getInserts()) {
                writer.println(insert);
            }
            counter++;
            if (counter % 1000 == 0) {
                writer.close();
                writer = new PrintWriter(new File(importDir, "queue-" + time + "-" + counter / 1000 + ".sql"));
            }
        }
        writer.close();
    }

    int getQueueSize() {
        synchronized (queue) {
            return queue.size();
        }
    }

    private boolean isQueueEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

    private void addQueueLast(Row row) {
        synchronized (queue) {
            boolean wasEmpty = queue.isEmpty();
            queue.addLast(row);
            addEntryCounter++;
            if (Config.queueWarningSize > 0 && queue.size() >= Config.queueWarningSize && addEntryCounter >= nextWarnCounter) {
                logblock.getLogger().warning("[Consumer] Queue overloaded. Size: " + queue.size());
                nextWarnCounter = addEntryCounter + 1000;
            }
            if (wasEmpty) {
                queue.notifyAll();
            }
        }
    }

    private Row pollQueueFirst() {
        synchronized (queue) {
            return queue.pollFirst();
        }
    }

    static void hide(Player player) {
        hiddenPlayers.add(player.getName().toLowerCase());
    }

    static void unHide(Player player) {
        hiddenPlayers.remove(player.getName().toLowerCase());
    }

    static boolean toggleHide(Player player) {
        final String playerName = player.getName().toLowerCase();
        if (hiddenPlayers.contains(playerName)) {
            hiddenPlayers.remove(playerName);
            return false;
        }
        hiddenPlayers.add(playerName);
        return true;
    }

    private boolean addPlayer(Connection conn, Actor actor) throws SQLException {
        // Odd query contruction is to work around innodb auto increment behaviour - bug #492
        String name = actor.getName();
        String uuid = actor.getUUID();
        Statement state = conn.createStatement();
        String q1 = "INSERT IGNORE INTO `lb-players` (playername,UUID) SELECT '" + mysqlTextEscape(name) + "','" + mysqlTextEscape(uuid) + "' FROM `lb-players` WHERE NOT EXISTS (SELECT NULL FROM `lb-players` WHERE UUID = '" + mysqlTextEscape(uuid) + "') LIMIT 1";
        String q2 = "SELECT playerid FROM `lb-players` WHERE UUID = '" + mysqlTextEscape(uuid) + "'";
        int q1Result = state.executeUpdate(q1);
        ResultSet rs = state.executeQuery(q2);
        if (rs.next()) {
            uncommitedPlayerIds.put(actor, rs.getInt(1));
        }
        rs.close();
        if (!uncommitedPlayerIds.containsKey(actor)) {
            state.executeUpdate("INSERT IGNORE INTO `lb-players` (playername,UUID) VALUES ('" + mysqlTextEscape(name) + "','" + mysqlTextEscape(uuid) + "')");
            rs = state.executeQuery(q2);
            if (rs.next()) {
                uncommitedPlayerIds.put(actor, rs.getInt(1));
            } else {
                logblock.getLogger().warning("[Consumer] Failed to add player " + actor.getName());
                logblock.getLogger().warning("[Consumer-Debug] Query 1: " + q1);
                logblock.getLogger().warning("[Consumer-Debug] Query 1 - Result: " + q1Result);
                logblock.getLogger().warning("[Consumer-Debug] Query 2: " + q2);
            }
            rs.close();
        }
        state.close();
        return uncommitedPlayerIds.containsKey(actor);
    }

    private long getEntityUUID(Connection conn, World world, UUID uuid) throws SQLException {
        Map<UUID, Long> uncommitedEntityIdsHere = uncommitedEntityIds.get(world);
        if (uncommitedEntityIdsHere == null) {
            uncommitedEntityIdsHere = new HashMap<>();
            uncommitedEntityIds.put(world, uncommitedEntityIdsHere);
        }
        Long existing = uncommitedEntityIdsHere.get(uuid);
        if (existing != null) {
            return existing;
        }

        // Odd query contruction is to work around innodb auto increment behaviour - bug #492
        final String table = getWorldConfig(world).table;
        String uuidString = uuid.toString();
        Statement state = conn.createStatement();
        String q1 = "INSERT IGNORE INTO `" + table + "-entityids` (entityuuid) SELECT '" + mysqlTextEscape(uuidString) + "' FROM `" + table + "-entityids` WHERE NOT EXISTS (SELECT NULL FROM `" + table + "-entityids` WHERE entityuuid = '" + mysqlTextEscape(uuidString) + "') LIMIT 1";
        String q2 = "SELECT entityid FROM `" + table + "-entityids` WHERE entityuuid = '" + mysqlTextEscape(uuidString) + "'";
        int q1Result = state.executeUpdate(q1);
        ResultSet rs = state.executeQuery(q2);
        if (rs.next()) {
            uncommitedEntityIdsHere.put(uuid, rs.getLong(1));
        }
        rs.close();
        // if there was not any row in the table the query above does not work, so we need to try this one
        if (!uncommitedEntityIdsHere.containsKey(uuid)) {
            state.executeUpdate("INSERT IGNORE INTO `" + table + "-entityids` (entityuuid) VALUES ('" + mysqlTextEscape(uuidString) + "')");
            rs = state.executeQuery(q2);
            if (rs.next()) {
                uncommitedEntityIdsHere.put(uuid, rs.getLong(1));
            } else {
                logblock.getLogger().warning("[Consumer] Failed to add entity uuid " + uuidString.toString());
                logblock.getLogger().warning("[Consumer-Debug] World: " + world.getName());
                logblock.getLogger().warning("[Consumer-Debug] Query 1: " + q1);
                logblock.getLogger().warning("[Consumer-Debug] Query 1 - Result: " + q1Result);
                logblock.getLogger().warning("[Consumer-Debug] Query 2: " + q2);
            }
            rs.close();
        }
        state.close();
        return uncommitedEntityIdsHere.get(uuid);
    }

    private void queueBlock(Actor actor, Location loc, BlockData typeBefore, BlockData typeAfter, YamlConfiguration stateBefore, YamlConfiguration stateAfter, ChestAccess ca) {
        if (typeBefore == null || typeBefore.getMaterial() == Material.CAVE_AIR || typeBefore.getMaterial() == Material.VOID_AIR) {
            typeBefore = Bukkit.createBlockData(Material.AIR);
        }
        if (typeAfter == null && ((typeBefore instanceof Waterlogged && ((Waterlogged) typeBefore).isWaterlogged()) || BukkitUtils.isAlwaysWaterlogged(typeBefore.getMaterial()))) {
            typeAfter = Bukkit.createBlockData(Material.WATER);
        }
        if (typeAfter == null || typeAfter.getMaterial() == Material.CAVE_AIR || typeAfter.getMaterial() == Material.VOID_AIR) {
            typeAfter = Bukkit.createBlockData(Material.AIR);
        }
        if (BlockChangePreLogEvent.getHandlerList().getRegisteredListeners().length > 0) {
            // Create and call the event
            BlockChangePreLogEvent event = new BlockChangePreLogEvent(actor, loc, typeBefore, typeAfter, stateBefore, stateAfter, ca);
            logblock.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            // Update variables
            actor = event.getOwnerActor();
            loc = event.getLocation();
            typeBefore = event.getTypeBefore();
            typeAfter = event.getTypeAfter();
            stateBefore = event.getStateBefore();
            stateAfter = event.getStateAfter();
            ca = event.getChestAccess();
        }
        // Do this last so LogBlock still has final say in what is being added
        if (actor == null || loc == null || typeBefore == null || typeAfter == null || hiddenPlayers.contains(actor.getName().toLowerCase()) || !isLogged(loc.getWorld()) || typeBefore != typeAfter && hiddenBlocks.contains(typeBefore.getMaterial()) && hiddenBlocks.contains(typeAfter.getMaterial())) {
            return;
        }

        int replacedMaterialId = MaterialConverter.getOrAddMaterialId(typeBefore);
        int replacedStateId = MaterialConverter.getOrAddBlockStateId(typeBefore);
        int typeMaterialId = MaterialConverter.getOrAddMaterialId(typeAfter);
        int typeStateId = MaterialConverter.getOrAddBlockStateId(typeAfter);

        addQueueLast(new BlockRow(loc, actor, replacedMaterialId, replacedStateId, Utils.serializeYamlConfiguration(stateBefore), typeMaterialId, typeStateId, Utils.serializeYamlConfiguration(stateAfter), ca));
    }

    public void queueEntityModification(Actor actor, Entity entity, EntityChangeType changeType, YamlConfiguration data) {
        if (actor == null || changeType == null || entity == null || hiddenPlayers.contains(actor.getName().toLowerCase()) || !isLogged(entity.getWorld())) {
            return;
        }
        UUID entityId = entity.getUniqueId();
        EntityType entityType = entity.getType();
        Location loc = entity.getLocation();

        if (EntityChangePreLogEvent.getHandlerList().getRegisteredListeners().length > 0) {
            // Create and call the event
            EntityChangePreLogEvent event = new EntityChangePreLogEvent(actor, loc, entity, changeType, data);
            logblock.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            // Update variables
            actor = event.getOwnerActor();
            loc = event.getLocation();
        }
        // Do this last so LogBlock still has final say in what is being added
        if (actor == null || loc == null || hiddenPlayers.contains(actor.getName().toLowerCase()) || !isLogged(loc.getWorld())) {
            return;
        }

        addQueueLast(new EntityRow(loc, actor, entityType, entityId, changeType, Utils.serializeYamlConfiguration(data)));
    }

    /**
     * Change the UUID that is stored for an entity in the database. This is needed when an entity is respawned
     * and now has a different UUID.
     *
     * @param world the world that contains the entity
     * @param entityId the database id of the entity
     * @param entityUUID the new UUID of the entity
     */
    public void queueEntityUUIDChange(World world, int entityId, UUID entityUUID) {
        addQueueLast(new EntityUUIDChange(world, entityId, entityUUID));
    }

    private String playerID(Actor actor) {
        if (actor == null) {
            return "NULL";
        }
        final Integer id = playerIds.get(actor);
        if (id != null) {
            return id.toString();
        }
        return "(SELECT playerid FROM `lb-players` WHERE UUID = '" + mysqlTextEscape(actor.getUUID()) + "')";
    }

    private Integer playerIDAsIntIncludeUncommited(Actor actor) {
        if (actor == null) {
            return null;
        }
        Integer id = playerIds.get(actor);
        if (id != null) {
            return id;
        }
        return uncommitedPlayerIds.get(actor);
    }

    private static interface Row {
        String[] getInserts();

        void process(Connection conn, BatchHelper batchHelper) throws SQLException;

        Actor[] getActors();
    }

    private class BlockRow extends BlockChange implements Row {
        final String statementString;
        final String selectActorIdStatementString;

        public BlockRow(Location loc, Actor actor, int replaced, int replacedData, byte[] replacedState, int type, int typeData, byte[] typeState, ChestAccess ca) {
            super(System.currentTimeMillis() / 1000, loc, actor, replaced, replacedData, replacedState, type, typeData, typeState, ca);

            statementString = getWorldConfig(loc.getWorld()).insertBlockStatementString;
            selectActorIdStatementString = getWorldConfig(loc.getWorld()).selectBlockActorIdStatementString;
        }

        @Override
        public String[] getInserts() {
            final String table = getWorldConfig(loc.getWorld()).table;
            final String[] inserts = new String[ca != null || replacedState != null || typeState != null ? 2 : 1];

            inserts[0] = "INSERT INTO `" + table + "-blocks` (date, playerid, replaced, replaceddata, type, typedata, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(actor) + ", " + replacedMaterial + ", " + replacedData + ", " + typeMaterial + ", " + typeData + ", '" + loc.getBlockX()
                    + "', " + safeY(loc) + ", '" + loc.getBlockZ() + "');";
            if (replacedState != null || typeState != null) {
                inserts[1] = "INSERT INTO `" + table + "-state` (replacedState, typeState, id) VALUES(" + Utils.mysqlPrepareBytesForInsertAllowNull(replacedState) + ", " + Utils.mysqlPrepareBytesForInsertAllowNull(typeState) + ", LAST_INSERT_ID());";
            } else if (ca != null) {
                try {
                    inserts[1] = "INSERT INTO `" + table + "-chestdata` (id, item, itemremove, itemtype) values (LAST_INSERT_ID(), '" + Utils.mysqlEscapeBytes(Utils.saveItemStack(ca.itemStack)) + "', " + (ca.remove ? 1 : 0) + ", " + ca.itemType + ");";
                } catch (Exception e) {
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not serialize ItemStack " + e.getMessage(), e);
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Problematic row: " + toString());
                    return new String[0];
                }
            }
            return inserts;
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { actor };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            byte[] serializedItemStack = null;
            if (ca != null) {
                try {
                    serializedItemStack = Utils.saveItemStack(ca.itemStack);
                } catch (Exception e) {
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not serialize ItemStack " + e.getMessage(), e);
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Problematic row: " + toString());
                    return;
                }
            }
            final byte[] finalSerializedItemStack = serializedItemStack;
            int sourceActor = playerIDAsIntIncludeUncommited(actor);
            Location actorBlockLocation = actor.getBlockLocation();
            if (actorBlockLocation != null) {
                Integer tempSourceActor = batchHelper.getUncommitedBlockActor(actorBlockLocation);
                if (tempSourceActor != null) {
                    sourceActor = tempSourceActor;
                } else {
                    PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, selectActorIdStatementString, Statement.NO_GENERATED_KEYS);
                    smt.setInt(1, actorBlockLocation.getBlockX());
                    smt.setInt(2, safeY(actorBlockLocation));
                    smt.setInt(3, actorBlockLocation.getBlockZ());
                    ResultSet rs = smt.executeQuery();
                    if (rs.next()) {
                        sourceActor = rs.getInt(1);
                    }
                    rs.close();
                }
            }
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.RETURN_GENERATED_KEYS);
            smt.setLong(1, date);
            smt.setInt(2, sourceActor);
            smt.setInt(3, replacedMaterial);
            smt.setInt(4, replacedData);
            smt.setInt(5, typeMaterial);
            smt.setInt(6, typeData);
            smt.setInt(7, loc.getBlockX());
            smt.setInt(8, safeY(loc));
            smt.setInt(9, loc.getBlockZ());
            batchHelper.addUncommitedBlockActorId(loc, sourceActor);
            batchHelper.addBatch(smt, new LongCallback() {
                @Override
                public void call(long id) throws SQLException {
                    PreparedStatement ps;
                    if (typeState != null || replacedState != null) {
                        ps = batchHelper.getOrPrepareStatement(conn, getWorldConfig(loc.getWorld()).insertBlockStateStatementString, Statement.NO_GENERATED_KEYS);
                        ps.setBytes(1, replacedState);
                        ps.setBytes(2, typeState);
                        ps.setLong(3, id);
                        batchHelper.addBatch(ps, null);
                    }
                    if (ca != null) {
                        ps = batchHelper.getOrPrepareStatement(conn, getWorldConfig(loc.getWorld()).insertBlockChestDataStatementString, Statement.NO_GENERATED_KEYS);
                        ps.setBytes(1, finalSerializedItemStack);
                        ps.setInt(2, ca.remove ? 1 : 0);
                        ps.setLong(3, id);
                        ps.setInt(4, ca.itemType);
                        batchHelper.addBatch(ps, null);
                    }
                }
            });
        }
    }

    private class KillRow implements Row {
        final long date;
        final Actor killer, victim;
        final int weapon;
        final Location loc;
        final String statementString;

        KillRow(Location loc, Actor attacker, Actor defender, int weapon) {
            date = System.currentTimeMillis() / 1000;
            this.loc = loc;
            killer = attacker;
            victim = defender;
            this.weapon = weapon;

            statementString = "INSERT INTO `" + getWorldConfig(loc.getWorld()).table + "-kills` (date, killer, victim, weapon, x, y, z) VALUES (FROM_UNIXTIME(?), ?, ?, ?, ?, ?, ?)";
        }

        @Override
        public String[] getInserts() {
            return new String[] { "INSERT INTO `" + getWorldConfig(loc.getWorld()).table + "-kills` (date, killer, victim, weapon, x, y, z) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(killer) + ", " + playerID(victim) + ", " + weapon + ", " + loc.getBlockX() + ", " + safeY(loc) + ", "
                    + loc.getBlockZ() + ");" };
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { killer, victim };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.NO_GENERATED_KEYS);
            smt.setLong(1, date);
            smt.setInt(2, playerIDAsIntIncludeUncommited(killer));
            smt.setInt(3, playerIDAsIntIncludeUncommited(victim));
            smt.setInt(4, weapon);
            smt.setInt(5, loc.getBlockX());
            smt.setInt(6, safeY(loc));
            smt.setInt(7, loc.getBlockZ());
            batchHelper.addBatch(smt, null);
        }
    }

    private class ChatRow extends ChatMessage implements Row {
        private String statementString;

        ChatRow(Actor player, String message) {
            super(player, message);

            statementString = "INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(?), ?, ?)";
        }

        @Override
        public String[] getInserts() {
            return new String[] { "INSERT INTO `lb-chat` (date, playerid, message) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(player) + ", '" + mysqlTextEscape(message) + "');" };
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { player };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.NO_GENERATED_KEYS);
            smt.setLong(1, date);
            smt.setInt(2, playerIDAsIntIncludeUncommited(player));
            smt.setString(3, message);
            batchHelper.addBatch(smt, null);
        }
    }

    private class PlayerJoinRow implements Row {
        private final Actor player;
        private final long lastLogin;
        private final String ip;
        private String statementString;

        PlayerJoinRow(Player player) {
            this.player = Actor.actorFromEntity(player);
            lastLogin = System.currentTimeMillis() / 1000;
            ip = player.getAddress().toString().replace("'", "\\'");

            if (logPlayerInfo) {
                statementString = "UPDATE `lb-players` SET lastlogin = FROM_UNIXTIME(?), firstlogin = IF(firstlogin = 0, FROM_UNIXTIME(?), firstlogin), ip = ?, playername = ? WHERE UUID = ?";
            } else {
                statementString = "UPDATE `lb-players` SET playername = ? WHERE UUID = ?";
            }
        }

        @Override
        public String[] getInserts() {
            if (logPlayerInfo) {
                return new String[] {
                        "UPDATE `lb-players` SET lastlogin = FROM_UNIXTIME(" + lastLogin + "), firstlogin = IF(firstlogin = 0, FROM_UNIXTIME(" + lastLogin + "), firstlogin), ip = '" + ip + "', playername = '" + mysqlTextEscape(player.getName()) + "' WHERE UUID = '" + player.getUUID() + "';" };
            }
            return new String[] { "UPDATE `lb-players` SET playername = '" + mysqlTextEscape(player.getName()) + "' WHERE UUID = '" + mysqlTextEscape(player.getUUID()) + "';" };
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { player };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.NO_GENERATED_KEYS);
            if (logPlayerInfo) {
                smt.setLong(1, lastLogin);
                smt.setLong(2, lastLogin);
                smt.setString(3, ip);
                smt.setString(4, player.getName());
                smt.setString(5, player.getUUID());
            } else {
                smt.setString(1, player.getName());
                smt.setString(2, player.getUUID());
            }
            batchHelper.addBatch(smt, null);
        }
    }

    private class PlayerLeaveRow implements Row {
        private final long onlineTime;
        private final Actor actor;
        private String statementString;

        PlayerLeaveRow(Player player, long onlineTime) {
            this.onlineTime = onlineTime;
            actor = Actor.actorFromEntity(player);
            statementString = "UPDATE `lb-players` SET onlinetime = onlinetime + ? WHERE lastlogin > 0 && UUID = ?";
        }

        @Override
        public String[] getInserts() {
            if (logPlayerInfo) {
                return new String[] { "UPDATE `lb-players` SET onlinetime = onlinetime + " + onlineTime + " WHERE lastlogin > 0 && UUID = '" + mysqlTextEscape(actor.getUUID()) + "';" };
            }
            return new String[0];
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { actor };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.NO_GENERATED_KEYS);
            smt.setLong(1, onlineTime);
            smt.setString(2, actor.getUUID());
            batchHelper.addBatch(smt, null);
        }
    }

    private class EntityRow extends EntityChange implements Row {
        final String statementString;
        final String selectActorIdStatementString;

        public EntityRow(Location loc, Actor actor, EntityType type, UUID entityid, EntityChangeType changeType, byte[] data) {
            super(System.currentTimeMillis() / 1000, loc, actor, type, entityid, changeType, data);
            statementString = getWorldConfig(loc.getWorld()).insertEntityStatementString;
            selectActorIdStatementString = getWorldConfig(loc.getWorld()).selectBlockActorIdStatementString;
        }

        @Override
        public String[] getInserts() {
            final String table = getWorldConfig(loc.getWorld()).table;
            final String[] inserts = new String[2];

            inserts[0] = "INSERT IGNORE INTO `" + table + "-entityids` (entityuuid) SELECT '" + mysqlTextEscape(entityUUID.toString()) + "' FROM `" + table + "-entityids` WHERE NOT EXISTS (SELECT NULL FROM `" + table + "-entityids` WHERE entityuuid = '" + mysqlTextEscape(entityUUID.toString()) + "') LIMIT 1";
            int entityTypeId = EntityTypeConverter.getOrAddEntityTypeId(type);
            inserts[1] = "INSERT INTO `" + table + "-entities` (date, playerid, entityid, entitytypeid, x, y, z, action, data) VALUES (FROM_UNIXTIME(" + date + "), " + playerID(actor) + ", " + "(SELECT entityid FROM `" + table + "-entityids` WHERE entityuuid = '" + mysqlTextEscape(entityUUID.toString()) + "')"
                    + ", " + entityTypeId + ", '" + loc.getBlockX() + "', " + safeY(loc) + ", '" + loc.getBlockZ() + "', " + changeType.ordinal() + ", " + Utils.mysqlPrepareBytesForInsertAllowNull(data) + ");";
            return inserts;
        }

        @Override
        public Actor[] getActors() {
            return new Actor[] { actor };
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            int sourceActor = playerIDAsIntIncludeUncommited(actor);
            Location actorBlockLocation = actor.getBlockLocation();
            if (actorBlockLocation != null) {
                Integer tempSourceActor = batchHelper.getUncommitedBlockActor(actorBlockLocation);
                if (tempSourceActor != null) {
                    sourceActor = tempSourceActor;
                } else {
                    PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, selectActorIdStatementString, Statement.NO_GENERATED_KEYS);
                    smt.setInt(1, actorBlockLocation.getBlockX());
                    smt.setInt(2, safeY(actorBlockLocation));
                    smt.setInt(3, actorBlockLocation.getBlockZ());
                    ResultSet rs = smt.executeQuery();
                    if (rs.next()) {
                        sourceActor = rs.getInt(1);
                    }
                    rs.close();
                }
            }
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, statementString, Statement.NO_GENERATED_KEYS);
            smt.setLong(1, date);
            smt.setInt(2, sourceActor);
            smt.setLong(3, getEntityUUID(conn, loc.getWorld(), entityUUID));
            smt.setInt(4, EntityTypeConverter.getOrAddEntityTypeId(type));
            smt.setInt(5, loc.getBlockX());
            smt.setInt(6, safeY(loc));
            smt.setInt(7, loc.getBlockZ());
            smt.setInt(8, changeType.ordinal());
            smt.setBytes(9, data);
            batchHelper.addBatch(smt, null);
        }
    }

    private class EntityUUIDChange implements Row {
        private final World world;
        private final long entityId;
        private final UUID entityUUID;
        final String updateEntityUUIDString;

        public EntityUUIDChange(World world, long entityId, UUID entityUUID) {
            this.world = world;
            this.entityId = entityId;
            this.entityUUID = entityUUID;
            updateEntityUUIDString = getWorldConfig(world).updateEntityUUIDString;
        }

        @Override
        public String[] getInserts() {
            final String table = getWorldConfig(world).table;
            final String[] inserts = new String[1];

            inserts[0] = "UPDATE `" + table + "-entityids` SET entityuuid = '" + mysqlTextEscape(entityUUID.toString()) + "' WHERE entityid = " + entityId;
            return inserts;
        }

        @Override
        public Actor[] getActors() {
            return new Actor[0];
        }

        @Override
        public void process(Connection conn, BatchHelper batchHelper) throws SQLException {
            PreparedStatement smt = batchHelper.getOrPrepareStatement(conn, updateEntityUUIDString, Statement.NO_GENERATED_KEYS);
            smt.setString(1, entityUUID.toString());
            smt.setLong(2, entityId);
            smt.executeUpdate();
        }
    }

    private int safeY(Location loc) {
        int safeY = loc.getBlockY();
        if (safeY < Short.MIN_VALUE) {
            safeY = Short.MIN_VALUE;
        }
        if (safeY > Short.MAX_VALUE) {
            safeY = Short.MAX_VALUE;
        }
        return safeY;
    }

    private class BatchHelper {
        private HashMap<String, PreparedStatement> preparedStatements = new HashMap<>();
        private HashSet<PreparedStatement> preparedStatementsWithGeneratedKeys = new HashSet<>();
        private LinkedHashMap<PreparedStatement, ArrayList<LongCallback>> generatedKeyHandler = new LinkedHashMap<>();
        private HashMap<Location, Integer> uncommitedBlockActors = new HashMap<>();

        public void reset() {
            preparedStatements.clear();
            preparedStatementsWithGeneratedKeys.clear();
            generatedKeyHandler.clear();
            uncommitedBlockActors.clear();
        }

        public void addUncommitedBlockActorId(Location loc, int actorId) {
            uncommitedBlockActors.put(loc, actorId);
        }

        public Integer getUncommitedBlockActor(Location loc) {
            return uncommitedBlockActors.get(loc);
        }

        public void processStatements(Connection conn) throws SQLException {
            while (!generatedKeyHandler.isEmpty()) {
                Entry<PreparedStatement, ArrayList<LongCallback>> entry = generatedKeyHandler.entrySet().iterator().next();
                PreparedStatement smt = entry.getKey();
                ArrayList<LongCallback> callbackList = entry.getValue();
                generatedKeyHandler.remove(smt);
                smt.executeBatch();
                if (preparedStatementsWithGeneratedKeys.contains(smt)) {
                    ResultSet keys = smt.getGeneratedKeys();
                    long[] results = new long[callbackList.size()];
                    int pos = 0;
                    while (keys.next() && pos < results.length) {
                        results[pos++] = keys.getLong(1);
                    }
                    keys.close();
                    for (int i = 0; i < results.length; i++) {
                        LongCallback callback = callbackList.get(i);
                        if (callback != null) {
                            callback.call(results[i]);
                        }
                    }
                }
            }
            uncommitedBlockActors.clear();
        }

        public PreparedStatement getOrPrepareStatement(Connection conn, String sql, int autoGeneratedKeys) throws SQLException {
            PreparedStatement smt = preparedStatements.get(sql);
            if (smt == null) {
                smt = conn.prepareStatement(sql, autoGeneratedKeys);
                preparedStatements.put(sql, smt);
                if (autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS) {
                    preparedStatementsWithGeneratedKeys.add(smt);
                }
            }
            return smt;
        }

        public void addBatch(PreparedStatement smt, LongCallback generatedKeysCallback) throws SQLException {
            smt.addBatch();
            ArrayList<LongCallback> callbackList = generatedKeyHandler.get(smt);
            if (callbackList == null) {
                callbackList = new ArrayList<>();
                generatedKeyHandler.put(smt, callbackList);
            }
            callbackList.add(generatedKeysCallback);
        }
    }

    protected interface LongCallback {
        public void call(long value) throws SQLException;
    }
}
