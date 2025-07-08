package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.util.Fraction;
import de.diddiz.LogBlock.util.ItemStackAndAmount;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DecoratedPot;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.ChiseledBookshelf;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.util.BukkitUtils.*;

public class ChestAccessLogging extends LoggingListener {
    private static final InventoryAction PICKUP_ALL_INTO_BUNDLE;
    private static final InventoryAction PICKUP_FROM_BUNDLE;
    private static final InventoryAction PICKUP_SOME_INTO_BUNDLE;
    private static final InventoryAction PLACE_ALL_INTO_BUNDLE;
    private static final InventoryAction PLACE_FROM_BUNDLE;
    private static final InventoryAction PLACE_SOME_INTO_BUNDLE;
    static {
        InventoryAction pickupAllIntoBundle = null;
        InventoryAction pickupFromBundle = null;
        InventoryAction pickupSomeIntoBundle = null;
        InventoryAction placeAllIntoBundle = null;
        InventoryAction placeFromBundle = null;
        InventoryAction placeSomeIntoBundle = null;
        try {
            pickupAllIntoBundle = InventoryAction.valueOf("PICKUP_ALL_INTO_BUNDLE");
            pickupFromBundle = InventoryAction.valueOf("PICKUP_FROM_BUNDLE");
            pickupSomeIntoBundle = InventoryAction.valueOf("PICKUP_SOME_INTO_BUNDLE");
            placeAllIntoBundle = InventoryAction.valueOf("PLACE_ALL_INTO_BUNDLE");
            placeFromBundle = InventoryAction.valueOf("PLACE_FROM_BUNDLE");
            placeSomeIntoBundle = InventoryAction.valueOf("PLACE_SOME_INTO_BUNDLE");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        PICKUP_ALL_INTO_BUNDLE = pickupAllIntoBundle;
        PICKUP_FROM_BUNDLE = pickupFromBundle;
        PICKUP_SOME_INTO_BUNDLE = pickupSomeIntoBundle;
        PLACE_ALL_INTO_BUNDLE = placeAllIntoBundle;
        PLACE_FROM_BUNDLE = placeFromBundle;
        PLACE_SOME_INTO_BUNDLE = placeSomeIntoBundle;
    }

    private class PlayerActiveInventoryModifications {
        private final HumanEntity actor;
        private final Location location;
        private final LinkedHashMap<ItemStack, Integer> modifications;

        public PlayerActiveInventoryModifications(HumanEntity actor, Location location) {
            this.actor = actor;
            this.location = location;
            this.modifications = new LinkedHashMap<>();
        }

        public void addModification(ItemStack stack, int amount) {
            if (amount == 0) {
                return;
            }
            // if we have other viewers, we have to flush their changes
            ArrayList<PlayerActiveInventoryModifications> allViewers = containersByLocation.get(location);
            if (allViewers.size() > 1) {
                for (PlayerActiveInventoryModifications other : allViewers) {
                    if (other != this) {
                        other.flush();
                    }
                }
            }

            // consumer.getLogblock().getLogger().info("Modify container: " + stack + " change: " + amount);
            stack = new ItemStack(stack);
            stack.setAmount(1);
            Integer existing = modifications.get(stack);
            int newTotal = amount + (existing == null ? 0 : existing);
            if (newTotal == 0) {
                modifications.remove(stack);
            } else {
                modifications.put(stack, newTotal);
            }
        }

        public void flush() {
            if (!modifications.isEmpty()) {
                for (Entry<ItemStack, Integer> e : modifications.entrySet()) {
                    ItemStack stack = e.getKey();
                    int amount = e.getValue();
                    if (amount != 0) {
                        // consumer.getLogblock().getLogger().info("Store container: " + stack + " take: " + (amount < 0));
                        consumer.queueChestAccess(Actor.actorFromEntity(actor), location, location.getWorld().getBlockAt(location).getBlockData(), new ItemStackAndAmount(stack, Math.abs(amount)), amount < 0);
                    }
                }
                modifications.clear();
            }
        }

        public HumanEntity getActor() {
            return actor;
        }

        public Location getLocation() {
            return location;
        }
    }

    private final Map<HumanEntity, PlayerActiveInventoryModifications> containersByOwner = new HashMap<>();
    private final Map<Location, ArrayList<PlayerActiveInventoryModifications>> containersByLocation = new HashMap<>();

    public ChestAccessLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containersByOwner.remove(player);
            if (modifications != null) {
                final Location loc = modifications.getLocation();
                ArrayList<PlayerActiveInventoryModifications> atLocation = containersByLocation.get(loc);
                atLocation.remove(modifications);
                if (atLocation.isEmpty()) {
                    containersByLocation.remove(loc);
                }
                modifications.flush();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        if (event.getInventory() != null) {
            InventoryHolder holder = event.getInventory().getHolder();
            if (holder instanceof BlockState || holder instanceof DoubleChest) {
                if (getInventoryHolderType(holder) != Material.CRAFTING_TABLE) {
                    PlayerActiveInventoryModifications modifications = new PlayerActiveInventoryModifications(event.getPlayer(), getInventoryHolderLocation(holder));
                    containersByOwner.put(modifications.getActor(), modifications);
                    containersByLocation.compute(modifications.getLocation(), (k, v) -> {
                        if (v == null) {
                            v = new ArrayList<>();
                        }
                        v.add(modifications);
                        return v;
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        final HumanEntity player = event.getWhoClicked();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containersByOwner.get(player);
            if (modifications != null) {
                InventoryAction action = event.getAction();
                switch (action) {
                    case PICKUP_ONE:
                    case DROP_ONE_SLOT:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -1);
                        }
                        break;
                    case PICKUP_HALF:
                        // server behaviour: round up
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -(event.getCurrentItem().getAmount() + 1) / 2);
                        }
                        break;
                    case PICKUP_SOME: // oversized stack - can not take all when clicking
                        // server behaviour: leave a full stack in the slot, take everything else
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            int taken = event.getCurrentItem().getAmount() - event.getCurrentItem().getMaxStackSize();
                            modifications.addModification(event.getCursor(), -taken);
                        }
                        break;
                    case PICKUP_ALL:
                    case DROP_ALL_SLOT:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                        }
                        break;
                    case PLACE_ONE:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), 1);
                        }
                        break;
                    case PLACE_SOME: // not enough free place in target slot
                        // server behaviour: place as much as possible
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            int placeable = event.getCurrentItem().getMaxStackSize() - event.getCurrentItem().getAmount();
                            modifications.addModification(event.getCursor(), placeable);
                        }
                        break;
                    case PLACE_ALL:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), event.getCursor().getAmount());
                        }
                        break;
                    case SWAP_WITH_CURSOR:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), event.getCursor().getAmount());
                            modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                        }
                        break;
                    case MOVE_TO_OTHER_INVENTORY: // shift + click
                        boolean removed = event.getRawSlot() < event.getView().getTopInventory().getSize();
                        int maxMove = getFreeSpace(event.getCurrentItem(), removed ? event.getView().getBottomInventory() : event.getView().getTopInventory());
                        if (maxMove > 0) {
                            modifications.addModification(event.getCurrentItem(), Math.min(event.getCurrentItem().getAmount(), maxMove) * (removed ? -1 : 1));
                        }
                        break;
                    case COLLECT_TO_CURSOR: // double click
                        // server behaviour: first collect all with an amount != maxstacksize, then others, starting from slot 0 (container)
                        ItemStack cursor = event.getCursor();
                        if (cursor == null) {
                            return;
                        }
                        int toPickUp = cursor.getMaxStackSize() - cursor.getAmount();
                        int takenFromContainer = 0;
                        boolean takeFromFullStacks = false;
                        Inventory top = event.getView().getTopInventory();
                        Inventory bottom = event.getView().getBottomInventory();
                        while (toPickUp > 0) {
                            for (ItemStack stack : top.getStorageContents()) {
                                if (cursor.isSimilar(stack)) {
                                    if (takeFromFullStacks == (stack.getAmount() == stack.getMaxStackSize())) {
                                        int take = Math.min(toPickUp, stack.getAmount());
                                        toPickUp -= take;
                                        takenFromContainer += take;
                                        if (toPickUp <= 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (toPickUp <= 0) {
                                break;
                            }
                            for (ItemStack stack : bottom.getStorageContents()) {
                                if (cursor.isSimilar(stack)) {
                                    if (takeFromFullStacks == (stack.getAmount() == stack.getMaxStackSize())) {
                                        int take = Math.min(toPickUp, stack.getAmount());
                                        toPickUp -= take;
                                        if (toPickUp <= 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (takeFromFullStacks) {
                                break;
                            } else {
                                takeFromFullStacks = true;
                            }
                        }
                        if (takenFromContainer > 0) {
                            modifications.addModification(event.getCursor(), -takenFromContainer);
                        }
                        break;
                    case HOTBAR_SWAP: // number key or offhand key
                    case HOTBAR_MOVE_AND_READD: // something was in the other slot
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            ItemStack otherSlot = (event.getClick() == ClickType.SWAP_OFFHAND) ? event.getWhoClicked().getInventory().getItemInOffHand() : event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                                modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                            }
                            if (otherSlot != null && otherSlot.getType() != Material.AIR) {
                                modifications.addModification(otherSlot, otherSlot.getAmount());
                            }
                        }
                        break;
                    case DROP_ALL_CURSOR:
                    case DROP_ONE_CURSOR:
                    case CLONE_STACK:
                    case NOTHING:
                        // only the cursor or nothing (but not the inventory) was modified
                        break;
                    case UNKNOWN:
                    default:
                        if (action == PICKUP_ALL_INTO_BUNDLE || action == PICKUP_SOME_INTO_BUNDLE) {
                            // slot -> free space on cursor bundle
                            if (event.getRawSlot() < event.getView().getTopInventory().getSize() && event.getCursor() != null && event.getCurrentItem() != null) {
                                int amount = getMaxAmountToAdd(event.getCurrentItem(), event.getCursor());
                                if (amount > 0) {
                                    modifications.addModification(event.getCurrentItem(), -amount);
                                }
                            }
                        } else if (action == PICKUP_FROM_BUNDLE) {
                            // last stack from bundle in slot -> cursor (remove old bundle, add new bundle with lesser items)
                            if (event.getRawSlot() < event.getView().getTopInventory().getSize() && event.getCurrentItem() != null) {
                                ItemMeta meta = event.getCurrentItem().getItemMeta();
                                if (meta != null && meta instanceof BundleMeta bundleMeta) {
                                    ItemStack newBundleStack = event.getCurrentItem().clone();
                                    ArrayList<ItemStack> bundleContent = new ArrayList<>(bundleMeta.getItems());
                                    if (!bundleContent.isEmpty()) {
                                        bundleContent.removeFirst();
                                        bundleMeta = (BundleMeta) bundleMeta.clone();
                                        bundleMeta.setItems(bundleContent);
                                        newBundleStack.setItemMeta(bundleMeta);

                                        modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                                        modifications.addModification(newBundleStack, newBundleStack.getAmount());
                                    }
                                }
                            }
                        } else if (action == PLACE_ALL_INTO_BUNDLE || action == PLACE_SOME_INTO_BUNDLE) {
                            // cursor -> bundle in slot (remove old bundle, add new bundle with added items)
                            if (event.getRawSlot() < event.getView().getTopInventory().getSize() && event.getCursor() != null && event.getCurrentItem() != null) {
                                ItemMeta meta = event.getCurrentItem().getItemMeta();
                                if (meta != null && meta instanceof BundleMeta bundleMeta) {
                                    int addable = getMaxAmountToAdd(event.getCursor(), event.getCurrentItem());
                                    if (addable > 0) {
                                        ItemStack addToBundle = event.getCursor().clone();
                                        addToBundle.setAmount(addable);
                                        ItemStack newBundleStack = event.getCurrentItem().clone();
                                        ArrayList<ItemStack> bundleContent = new ArrayList<>(bundleMeta.getItems());
                                        // if the bundle contains similar stack, remove that stack and add larger stack
                                        for (int i = 0; i < bundleContent.size(); i++) {
                                            if (bundleContent.get(i).isSimilar(addToBundle)) {
                                                addToBundle.setAmount(addToBundle.getAmount() + bundleContent.get(i).getAmount());
                                                bundleContent.remove(i);
                                                break;
                                            }
                                        }
                                        bundleContent.addFirst(addToBundle);
                                        bundleMeta = (BundleMeta) bundleMeta.clone();
                                        bundleMeta.setItems(bundleContent);
                                        newBundleStack.setItemMeta(bundleMeta);

                                        modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                                        modifications.addModification(newBundleStack, newBundleStack.getAmount());
                                    }
                                }
                            }
                        } else if (action == PLACE_FROM_BUNDLE) {
                            // last stack from bundle on cursor -> slot
                            if (event.getRawSlot() < event.getView().getTopInventory().getSize() && event.getCursor() != null) {
                                ItemMeta meta = event.getCursor().getItemMeta();
                                if (meta != null && meta instanceof BundleMeta bundleMeta) {
                                    List<ItemStack> items = bundleMeta.getItems();
                                    if (!items.isEmpty()) {
                                        ItemStack item = items.getFirst();
                                        modifications.addModification(item, item.getAmount());
                                    }
                                }
                            }
                        } else {
                            // unable to log something we don't know
                            consumer.getLogblock().getLogger().warning("Unknown inventory action by " + event.getWhoClicked().getName() + ": " + event.getAction() + " Slot: " + event.getSlot() + " Slot type: " + event.getSlotType());
                        }
                        break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        final HumanEntity player = event.getWhoClicked();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containersByOwner.get(player);
            if (modifications != null) {
                Inventory container = event.getView().getTopInventory();
                int containerSize = container.getSize();
                for (Entry<Integer, ItemStack> e : event.getNewItems().entrySet()) {
                    int slot = e.getKey();
                    if (slot < containerSize) {
                        ItemStack old = container.getItem(slot);
                        int oldAmount = (old == null || old.getType() == Material.AIR) ? 0 : old.getAmount();
                        modifications.addModification(e.getValue(), e.getValue().getAmount() - oldAmount);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Block clicked = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || !event.hasBlock() || clicked == null) {
            return;
        }
        final Player player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        final Material type = clicked.getType();
        if (type == Material.DECORATED_POT) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != Material.AIR && clicked.getState() instanceof DecoratedPot pot) {
                ItemStack currentInPot = pot.getSnapshotInventory().getItem();
                if (currentInPot == null || currentInPot.getType() == Material.AIR || currentInPot.isSimilar(mainHand) && currentInPot.getAmount() < currentInPot.getMaxStackSize()) {
                    ItemStack stack = mainHand.clone();
                    stack.setAmount(1);
                    consumer.queueChestAccess(Actor.actorFromEntity(player), clicked.getLocation(), clicked.getBlockData(), ItemStackAndAmount.fromStack(stack), false);
                }
            }
        } else if (type == Material.CHISELED_BOOKSHELF) {
            if (clicked.getBlockData() instanceof ChiseledBookshelf blockData && blockData.getFacing() == event.getBlockFace() && clicked.getState() instanceof org.bukkit.block.ChiseledBookshelf bookshelf) {
                // calculate the slot the same way as minecraft does it
                Vector pos = event.getClickedPosition();
                if (pos == null) {
                    return; // some plugins create this event without a clicked pos
                }
                double clickx = switch (blockData.getFacing()) {
                    case NORTH -> 1 - pos.getX();
                    case SOUTH -> pos.getX();
                    case EAST -> 1 - pos.getZ();
                    case WEST -> pos.getZ();
                    default -> throw new IllegalArgumentException("Unexpected facing for chiseled bookshelf: " + blockData.getFacing());
                };
                int col = clickx < 0.375 ? 0 : (clickx < 0.6875 ? 1 : 2); // 6/16 ; 11/16
                int row = pos.getY() >= 0.5 ? 0 : 1;
                int slot = col + row * 3;

                ItemStack currentInSlot = bookshelf.getSnapshotInventory().getItem(slot);
                if (blockData.isSlotOccupied(slot)) {
                    // not empty: always take
                    if (currentInSlot != null && currentInSlot.getType() != Material.AIR) {
                        consumer.queueChestAccess(Actor.actorFromEntity(player), clicked.getLocation(), clicked.getBlockData(), ItemStackAndAmount.fromStack(currentInSlot), true);
                    }
                } else {
                    // empty: put if has tag BOOKSHELF_BOOKS
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && mainHand.getType() != Material.AIR && Tag.ITEMS_BOOKSHELF_BOOKS.isTagged(mainHand.getType())) {
                        ItemStack stack = mainHand.clone();
                        stack.setAmount(1);
                        consumer.queueChestAccess(Actor.actorFromEntity(player), clicked.getLocation(), clicked.getBlockData(), ItemStackAndAmount.fromStack(stack), false);
                    }
                }
            }
        }
    }

    private static int getFreeSpace(ItemStack item, Inventory inventory) {
        int freeSpace = 0;
        int maxStack = Math.max(item.getMaxStackSize(), 1);

        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];

            if (item.isSimilar(content)) {
                freeSpace += Math.max(maxStack - content.getAmount(), 0);
            } else if (content == null || content.getType() == Material.AIR) {
                freeSpace += maxStack;
            }
            if (freeSpace >= item.getAmount()) {
                return item.getAmount();
            }
        }

        return freeSpace;
    }

    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);

    private static Fraction computeContentWeight(List<ItemStack> content) {
        Fraction sum = Fraction.ZERO;
        for (ItemStack itemStack : content) {
            sum = sum.add(getWeightOfOne(itemStack).multiplyBy(Fraction.getFraction(itemStack.getAmount(), 1)));
        }
        return sum;
    }

    private static Fraction getWeightOfOne(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta instanceof BundleMeta bundleMeta) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(computeContentWeight(bundleMeta.getItems()));
        } else if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof Beehive hive) {
                int entityCount = hive.getEntityCount();
                if (entityCount > 0) {
                    return Fraction.ONE;
                }
            }
        }
        return Fraction.getFraction(1, stack.getMaxStackSize());
    }

    private static int getMaxAmountToAdd(ItemStack stackToAdd, ItemStack bundle) {
        ItemMeta meta = bundle.getItemMeta();
        if (meta == null || !(meta instanceof BundleMeta bundleMeta)) {
            return 0;
        }
        Fraction weight = computeContentWeight(bundleMeta.getItems());

        Fraction free = Fraction.ONE.subtract(weight);
        return Math.min(stackToAdd.getAmount(), Math.max(free.divideBy(getWeightOfOne(stackToAdd)).intValue(), 0));
    }
}
