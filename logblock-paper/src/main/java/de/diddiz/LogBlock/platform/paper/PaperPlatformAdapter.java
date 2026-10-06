package de.diddiz.LogBlock.platform.paper;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.componentwrapper.ChatColor;
import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.componentwrapper.ItemHover;
import de.diddiz.LogBlock.componentwrapper.RunCommandClick;
import de.diddiz.LogBlock.componentwrapper.TextComponent;
import de.diddiz.LogBlock.componentwrapper.TextHover;
import de.diddiz.LogBlock.listeners.AdvancedEntityLogging;
import de.diddiz.LogBlock.platform.PlatformAdapter;
import de.diddiz.LogBlock.platform.ShelfSideChain;
import de.diddiz.LogBlock.platform.DripstoneThickness;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Speleothem;
import org.bukkit.command.CommandSender;

public final class PaperPlatformAdapter implements PlatformAdapter {
    public PaperPlatformAdapter(LogBlock plugin) {
    }

    @Override
    public void registerEntityLoggingListeners(LogBlock plugin, AdvancedEntityLogging logging) {
        plugin.getServer().getPluginManager().registerEvents(new PaperEntityBreakLogging(logging), plugin);
    }

    @Override
    public void sendComponent(CommandSender target, Component message) {
        target.sendMessage(toAdventure(message));
    }

    static net.kyori.adventure.text.Component toAdventure(Component message) {
        ChatColor ownColor = message.getColor();
        TextColor color = ownColor == null ? null : ownColor.hasName()
                ? NamedTextColor.NAMES.value(ownColor.getName()) : TextColor.color(ownColor.getColor());
        net.kyori.adventure.text.Component result = net.kyori.adventure.text.Component
                .text(((TextComponent) message).getText(), color)
                .children(message.getChildren().stream().map(PaperPlatformAdapter::toAdventure).toList());
        if (message.getHover() instanceof TextHover text) {
            result = result.hoverEvent(HoverEvent.showText(toAdventure(text.getText())));
        } else if (message.getHover() instanceof ItemHover item) {
            result = result.hoverEvent(item.getItem().asHoverEvent());
        }
        if (message.getClick() instanceof RunCommandClick click) {
            result = result.clickEvent(ClickEvent.runCommand(click.getCommand()));
        }
        return result;
    }

    @Override
    public Component fromLegacy(String text) {
        return fromAdventure(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    private static Component fromAdventure(net.kyori.adventure.text.Component message) {
        String text = message instanceof net.kyori.adventure.text.TextComponent component ? component.content() : "";
        TextColor color = message.color();
        ChatColor ownColor = null;
        if (color instanceof NamedTextColor named) {
            ownColor = switch (NamedTextColor.NAMES.key(named)) {
                case "black" -> ChatColor.BLACK;
                case "dark_blue" -> ChatColor.DARK_BLUE;
                case "dark_green" -> ChatColor.DARK_GREEN;
                case "dark_aqua" -> ChatColor.DARK_AQUA;
                case "dark_red" -> ChatColor.DARK_RED;
                case "dark_purple" -> ChatColor.DARK_PURPLE;
                case "gold" -> ChatColor.GOLD;
                case "gray" -> ChatColor.GRAY;
                case "dark_gray" -> ChatColor.DARK_GRAY;
                case "blue" -> ChatColor.BLUE;
                case "green" -> ChatColor.GREEN;
                case "aqua" -> ChatColor.AQUA;
                case "red" -> ChatColor.RED;
                case "light_purple" -> ChatColor.LIGHT_PURPLE;
                case "yellow" -> ChatColor.YELLOW;
                case "white" -> ChatColor.WHITE;
                default -> ChatColor.from(named.value());
            };
        } else if (color != null) {
            ownColor = ChatColor.from(color.value());
        }
        Component result = Components.text(text, ownColor);
        for (net.kyori.adventure.text.Component child : message.children()) {
            result = result.append(fromAdventure(child));
        }
        // The existing wrapper represents colors and text, but no legacy decorations.
        return result;
    }

    @Override
    public String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(toAdventure(component));
    }

    @Override
    public ShelfSideChain getShelfSideChain(Shelf shelf) {
        return ShelfSideChain.valueOf(shelf.getSideChain().name());
    }

    @Override
    public DripstoneThickness getDripstoneThickness(BlockData dripstone) {
        return DripstoneThickness.valueOf(((Speleothem) dripstone).getThickness().name());
    }

    @Override
    public void setDripstoneThickness(BlockData dripstone, DripstoneThickness thickness) {
        ((Speleothem) dripstone).setThickness(Speleothem.Thickness.valueOf(thickness.name()));
    }

    @Override
    public BlockFace getDripstoneVerticalDirection(BlockData dripstone) {
        return ((Speleothem) dripstone).getVerticalDirection();
    }
}
