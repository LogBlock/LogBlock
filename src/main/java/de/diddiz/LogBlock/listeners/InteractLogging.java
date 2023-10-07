package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.Reflections;
import java.util.UUID;
import org.bukkit.DyeColor;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Cake;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.Comparator.Mode;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class InteractLogging extends LoggingListener {
    public InteractLogging(LogBlock lb) {
        super(lb);
    }

    private UUID lastInteractionPlayer;
    private BlockData lastInteractionBlockData;
    private Location lastInteractionLocation;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());
        if (wcfg != null) {
            final Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }
            final BlockData blockData = clicked.getBlockData();
            final Material type = blockData.getMaterial();
            final Player player = event.getPlayer();
            final Location loc = clicked.getLocation();
            lastInteractionPlayer = player.getUniqueId();
            lastInteractionBlockData = blockData;
            lastInteractionLocation = loc;

            if (BukkitUtils.isFenceGate(type) || BukkitUtils.isWoodenTrapdoor(type)) {
                if (wcfg.isLogging(Logging.DOORINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Openable newBlockData = (Openable) blockData.clone();
                    newBlockData.setOpen(!newBlockData.isOpen());
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (BukkitUtils.isPressurePlate(type)) {
                if (wcfg.isLogging(Logging.PRESUREPLATEINTERACT) && event.getAction() == Action.PHYSICAL) {
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, blockData);
                }
            } else if (BukkitUtils.isWoodenDoor(type)) {
                if (wcfg.isLogging(Logging.DOORINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Door newBlockData = (Door) blockData.clone();
                    newBlockData.setOpen(!newBlockData.isOpen());
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (BukkitUtils.isButton(type) || type == Material.LEVER) {
                if (wcfg.isLogging(Logging.SWITCHINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Switch newBlockData = (Switch) blockData.clone();
                    if (!newBlockData.isPowered() || type == Material.LEVER) {
                        newBlockData.setPowered(!newBlockData.isPowered());
                    }
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (BukkitUtils.isSign(type)) {
                if (wcfg.isLogging(Logging.SIGNTEXT) && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
                    Material itemType = event.getItem().getType();
                    if (BukkitUtils.isDye(itemType) || itemType == Material.GLOW_INK_SAC || itemType == Material.INK_SAC || itemType == Material.HONEYCOMB) {
                        final BlockState before = event.getClickedBlock().getState();
                        if (before instanceof Sign signBefore) {
                            boolean waxed = Reflections.isSignWaxed(signBefore);
                            if (!waxed) {
                                final Sign signAfter = (Sign) event.getClickedBlock().getState();
                                Side side = BukkitUtils.getFacingSignSide(player, clicked);
                                SignSide signSideBefore = signBefore.getSide(side);
                                SignSide signSideAfter = signAfter.getSide(side);
                                if (itemType == Material.GLOW_INK_SAC) {
                                    if (!signSideBefore.isGlowingText() && hasText(signSideBefore)) {
                                        signSideAfter.setGlowingText(true);
                                        consumer.queueBlockReplace(Actor.actorFromEntity(player), signBefore, signAfter);
                                    }
                                } else if (itemType == Material.INK_SAC) {
                                    if (signSideBefore.isGlowingText() && hasText(signSideBefore)) {
                                        signSideAfter.setGlowingText(false);
                                        consumer.queueBlockReplace(Actor.actorFromEntity(player), signBefore, signAfter);
                                    }
                                } else if (itemType == Material.HONEYCOMB) {
                                    signAfter.setEditable(false);
                                    consumer.queueBlockReplace(Actor.actorFromEntity(player), signBefore, signAfter);
                                } else if (BukkitUtils.isDye(itemType) && hasText(signSideBefore)) {
                                    DyeColor newColor = BukkitUtils.dyeToDyeColor(itemType);
                                    if (newColor != null && signSideBefore.getColor() != newColor) {
                                        signSideAfter.setColor(newColor);
                                        consumer.queueBlockReplace(Actor.actorFromEntity(player), signBefore, signAfter);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (type == Material.CAKE) {
                if (event.hasItem() && BukkitUtils.isCandle(event.getItem().getType()) && event.useItemInHand() != Result.DENY) {
                    BlockData newBlockData = Material.valueOf(event.getItem().getType().name() + "_CAKE").createBlockData();
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                } else if (wcfg.isLogging(Logging.CAKEEAT) && event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getFoodLevel() < 20) {
                    Cake newBlockData = (Cake) blockData.clone();
                    if (newBlockData.getBites() < 6) {
                        newBlockData.setBites(newBlockData.getBites() + 1);
                        consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                    } else {
                        consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, Material.AIR.createBlockData());
                    }
                }
            } else if (type == Material.NOTE_BLOCK) {
                if (wcfg.isLogging(Logging.NOTEBLOCKINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    NoteBlock newBlockData = (NoteBlock) blockData.clone();
                    if (newBlockData.getNote().getOctave() == 2) {
                        newBlockData.setNote(new Note(0, Tone.F, true));
                    } else {
                        newBlockData.setNote(newBlockData.getNote().sharped());
                    }
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (type == Material.REPEATER) {
                if (wcfg.isLogging(Logging.DIODEINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Repeater newBlockData = (Repeater) blockData.clone();
                    newBlockData.setDelay((newBlockData.getDelay() % 4) + 1);
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (type == Material.COMPARATOR) {
                if (wcfg.isLogging(Logging.COMPARATORINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Comparator newBlockData = (Comparator) blockData.clone();
                    newBlockData.setMode(newBlockData.getMode() == Mode.COMPARE ? Mode.SUBTRACT : Mode.COMPARE);
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (type == Material.DAYLIGHT_DETECTOR) {
                if (wcfg.isLogging(Logging.DAYLIGHTDETECTORINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    DaylightDetector newBlockData = (DaylightDetector) blockData.clone();
                    newBlockData.setInverted(!newBlockData.isInverted());
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                }
            } else if (type == Material.TRIPWIRE) {
                if (wcfg.isLogging(Logging.TRIPWIREINTERACT) && event.getAction() == Action.PHYSICAL) {
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, blockData);
                }
            } else if (type == Material.FARMLAND) {
                if (wcfg.isLogging(Logging.CROPTRAMPLE) && event.getAction() == Action.PHYSICAL) {
                    // 3 = Dirt ID
                    consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, Material.DIRT.createBlockData());
                    // Log the crop on top as being broken
                    Block trampledCrop = clicked.getRelative(BlockFace.UP);
                    if (BukkitUtils.isCropBlock(trampledCrop.getType())) {
                        consumer.queueBlockBreak(Actor.actorFromEntity(player), trampledCrop.getState());
                    }
                }
            } else if (type == Material.TURTLE_EGG) {
                if (wcfg.isLogging(Logging.BLOCKBREAK) && event.getAction() == Action.PHYSICAL) {
                    TurtleEgg turtleEggData = (TurtleEgg) blockData;
                    int eggs = turtleEggData.getEggs();
                    if (eggs > 1) {
                        TurtleEgg turtleEggData2 = (TurtleEgg) turtleEggData.clone();
                        turtleEggData2.setEggs(eggs - 1);
                        consumer.queueBlock(Actor.actorFromEntity(player), loc, turtleEggData, turtleEggData2);
                    } else {
                        consumer.queueBlock(Actor.actorFromEntity(player), loc, turtleEggData, Material.AIR.createBlockData());
                    }
                }
            } else if (type == Material.PUMPKIN) {
                if ((wcfg.isLogging(Logging.BLOCKBREAK) || wcfg.isLogging(Logging.BLOCKPLACE)) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    ItemStack inHand = event.getItem();
                    if (inHand != null && inHand.getType() == Material.SHEARS) {
                        BlockFace clickedFace = event.getBlockFace();
                        Directional newBlockData = (Directional) Material.CARVED_PUMPKIN.createBlockData();
                        if (clickedFace == BlockFace.NORTH || clickedFace == BlockFace.SOUTH || clickedFace == BlockFace.EAST || clickedFace == BlockFace.WEST) {
                            newBlockData.setFacing(clickedFace);
                        } else {
                            // use player distance to calculate the facing
                            Location playerLoc = player.getLocation();
                            playerLoc.subtract(0.5, 0, 0.5);
                            double dx = playerLoc.getX() - loc.getX();
                            double dz = playerLoc.getZ() - loc.getZ();
                            if (Math.abs(dx) > Math.abs(dz)) {
                                newBlockData.setFacing(dx > 0 ? BlockFace.EAST : BlockFace.WEST);
                            } else {
                                newBlockData.setFacing(dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
                            }
                        }
                        consumer.queueBlock(Actor.actorFromEntity(player), loc, blockData, newBlockData);
                    }
                }
            }
        }
    }

    private boolean hasText(SignSide signSide) {
        for (int i = 0; i < 4; i++) {
            if (!signSide.getLine(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent event) {
        if (lastInteractionPlayer != null && event.getEntity() != null && event.getEntity().getUniqueId().equals(lastInteractionPlayer) && lastInteractionLocation != null && event.getLocation().equals(lastInteractionLocation)) {
            if (lastInteractionBlockData instanceof Candle) {
                Candle previousCandle = (Candle) lastInteractionBlockData;
                if (previousCandle.isLit()) {
                    BlockData newData = lastInteractionLocation.getBlock().getBlockData();
                    if (newData instanceof Candle) {
                        Candle newCandle = (Candle) newData;
                        if (!newCandle.isLit() && !newCandle.isWaterlogged()) {
                            // log candle extinguish
                            consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), lastInteractionLocation, lastInteractionBlockData, newData);
                        }
                    }
                }
            } else if (lastInteractionBlockData instanceof Lightable && BukkitUtils.isCandleCake(lastInteractionBlockData.getMaterial())) {
                Lightable previousLightable = (Lightable) lastInteractionBlockData;
                BlockData newData = lastInteractionLocation.getBlock().getBlockData();
                if (event.getEvent().equals(GameEvent.EAT)) {
                    final WorldConfig wcfg = getWorldConfig(event.getLocation().getWorld());
                    if (wcfg.isLogging(Logging.CAKEEAT)) {
                        // nom nom (don't know why newData is incorrect here)
                        newData = Material.CAKE.createBlockData();
                        ((Cake) newData).setBites(1);
                        consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), lastInteractionLocation, lastInteractionBlockData, newData);
                    }
                } else if (previousLightable.isLit()) {
                    if (newData instanceof Lightable) {
                        Lightable newLightable = (Lightable) newData;
                        if (!newLightable.isLit()) {
                            // log cake extinguish
                            consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), lastInteractionLocation, lastInteractionBlockData, newData);
                        }
                    }
                }
            }
        }
        lastInteractionPlayer = null;
        lastInteractionBlockData = null;
        lastInteractionLocation = null;
    }
}
