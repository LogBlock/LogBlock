package de.diddiz.LogBlock.platform.spigot;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.componentwrapper.*;
import de.diddiz.LogBlock.platform.PlatformAdapter;
import de.diddiz.LogBlock.platform.ShelfSideChain;
import de.diddiz.LogBlock.platform.DripstoneThickness;
import de.diddiz.LogBlock.util.MessagingUtil;
import de.diddiz.LogBlock.util.TypeColor;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.inventory.ItemStack;

public final class SpigotPlatformAdapter implements PlatformAdapter {
    public SpigotPlatformAdapter(LogBlock plugin) {
    }
    private static final Map<ChatColor, net.md_5.bungee.api.ChatColor> colorMap;
    private static final Map<net.md_5.bungee.api.ChatColor, ChatColor> inverseColorMap;
    static {
        colorMap = new HashMap<>();
        colorMap.put(ChatColor.BLACK, net.md_5.bungee.api.ChatColor.BLACK);
        colorMap.put(ChatColor.DARK_BLUE, net.md_5.bungee.api.ChatColor.DARK_BLUE);
        colorMap.put(ChatColor.DARK_GREEN, net.md_5.bungee.api.ChatColor.DARK_GREEN);
        colorMap.put(ChatColor.DARK_AQUA, net.md_5.bungee.api.ChatColor.DARK_AQUA);
        colorMap.put(ChatColor.DARK_RED, net.md_5.bungee.api.ChatColor.DARK_RED);
        colorMap.put(ChatColor.DARK_PURPLE, net.md_5.bungee.api.ChatColor.DARK_PURPLE);
        colorMap.put(ChatColor.GOLD, net.md_5.bungee.api.ChatColor.GOLD);
        colorMap.put(ChatColor.GRAY, net.md_5.bungee.api.ChatColor.GRAY);
        colorMap.put(ChatColor.DARK_GRAY, net.md_5.bungee.api.ChatColor.DARK_GRAY);
        colorMap.put(ChatColor.BLUE, net.md_5.bungee.api.ChatColor.BLUE);
        colorMap.put(ChatColor.GREEN, net.md_5.bungee.api.ChatColor.GREEN);
        colorMap.put(ChatColor.AQUA, net.md_5.bungee.api.ChatColor.AQUA);
        colorMap.put(ChatColor.RED, net.md_5.bungee.api.ChatColor.RED);
        colorMap.put(ChatColor.LIGHT_PURPLE, net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
        colorMap.put(ChatColor.YELLOW, net.md_5.bungee.api.ChatColor.YELLOW);
        colorMap.put(ChatColor.WHITE, net.md_5.bungee.api.ChatColor.WHITE);

        inverseColorMap = new HashMap<>();
        for (Entry<ChatColor, net.md_5.bungee.api.ChatColor> e : colorMap.entrySet()) {
            inverseColorMap.put(e.getValue(), e.getKey());
        }
    }

    @Override
    public void sendComponent(CommandSender target, Component message) {
        target.spigot().sendMessage(toBungee(message));
    }

    private static BaseComponent toBungee(Component message) {
        BaseComponent result = new net.md_5.bungee.api.chat.TextComponent(((TextComponent) message).getText());
        result.setColor(toBungee(message.getColor()));
        if (!message.getChildren().isEmpty()) {
            for (Component child : message.getChildren()) {
                result.addExtra(toBungee(child));
            }
        }
        if (message.getHover() != null) {
            result.setHoverEvent(toBungee(message.getHover()));
        }
        if (message.getClick() != null) {
            result.setClickEvent(toBungee(message.getClick()));
        }
        // TODO formatings
        return result;
    }

    private static net.md_5.bungee.api.chat.ClickEvent toBungee(Click click) {
        if (click instanceof RunCommandClick run) {
            return new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, run.getCommand());
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static net.md_5.bungee.api.chat.HoverEvent toBungee(Hover hover) {
        if (hover instanceof TextHover text) {
            return new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Text(toBungee(text.getText())));
        } else if (hover instanceof ItemHover item) {
            ItemStack stack = item.getItem();
            try {
                String itemTag = stack.getItemMeta().getAsString();
                return new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM, new Item(stack.getType().getKey().toString(), 1, itemTag != null ? ItemTag.ofNbt(itemTag) : null));
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Failed to convert Itemstack to JSON", e);
                return new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[] { toBungee(MessagingUtil.createTextComponentWithColor("Error", TypeColor.ERROR.getColor())) }));
            }
        }
        return null;
    }

    private static net.md_5.bungee.api.ChatColor toBungee(ChatColor color) {
        return color == null ? null : colorMap.getOrDefault(color, net.md_5.bungee.api.ChatColor.of(new Color(color.getColor())));
    }

    public static Component toComponent(BaseComponent component) {
        Component result = Components.text(((net.md_5.bungee.api.chat.TextComponent) component).getText(), toComponent(component.getColorRaw()));
        if (component.getExtra() != null) {
            for (BaseComponent child : component.getExtra()) {
                result = result.append(toComponent(child));
            }
        }
        // TODO formatings
        // ignored: hover, click
        return result;
    }

    private static ChatColor toComponent(net.md_5.bungee.api.ChatColor color) {
        return color == null ? null : inverseColorMap.getOrDefault(color, ChatColor.from(color.getColor()));
    }
    @Override
    public Component fromLegacy(String text) {
        return toComponent(net.md_5.bungee.api.chat.TextComponent.fromLegacy(text));
    }

    @Override
    public String toPlainText(Component component) {
        return toBungee(component).toPlainText();
    }

    @Override
    public ShelfSideChain getShelfSideChain(Shelf shelf) {
        return ShelfSideChain.valueOf(shelf.getSideChain().name());
    }

    @Override
    public DripstoneThickness getDripstoneThickness(PointedDripstone dripstone) {
        return DripstoneThickness.valueOf(dripstone.getThickness().name());
    }

    @Override
    public void setDripstoneThickness(PointedDripstone dripstone, DripstoneThickness thickness) {
        dripstone.setThickness(PointedDripstone.Thickness.valueOf(thickness.name()));
    }
}
