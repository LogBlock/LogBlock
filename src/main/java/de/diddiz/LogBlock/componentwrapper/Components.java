package de.diddiz.LogBlock.componentwrapper;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.util.MessagingUtil;
import de.diddiz.LogBlock.util.TypeColor;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Components {
    private static final boolean HAS_ADVENTURE;
    private static final Component EMPTY = text("");
    private static final Component SPACE = text(" ");

    static {
        boolean hasAdventure = false;
        try {
            Class<?> audienceInterface = Class.forName("net.kyori.adventure.audience.Audience");
            hasAdventure = audienceInterface.isAssignableFrom(Player.class);
            LogBlock.getInstance().getLogger().info("Using adventure components");
        } catch (Throwable t) {
            LogBlock.getInstance().getLogger().info("Using bungee components");
        }
        HAS_ADVENTURE = hasAdventure;
    }

    public static Component empty() {
        return EMPTY;
    }

    public static Component space() {
        return SPACE;
    }

    public static TextComponent text(String text) {
        return text(text, null);
    }

    public static TextComponent text(String text, ChatColor color, Component... children) {
        return text(text, color, null, null, children);
    }

    public static TextComponent text(String text, ChatColor color, Hover hover, Click click, Component... children) {
        return new TextComponent(text, color, hover, click, children);
    }

    public static void sendTo(CommandSender target, Component message) {
        if (HAS_ADVENTURE) {
            AdventureSender.sendTo(target, message);
        } else {
            BungeeSender.sendTo(target, message);
        }
    }

    private static class AdventureSender {
        private static final Map<ChatColor, TextColor> colorMap;
        static {
            colorMap = new HashMap<>();
            colorMap.put(ChatColor.BLACK, NamedTextColor.BLACK);
            colorMap.put(ChatColor.DARK_BLUE, NamedTextColor.DARK_BLUE);
            colorMap.put(ChatColor.DARK_GREEN, NamedTextColor.DARK_GREEN);
            colorMap.put(ChatColor.DARK_AQUA, NamedTextColor.DARK_AQUA);
            colorMap.put(ChatColor.DARK_RED, NamedTextColor.DARK_RED);
            colorMap.put(ChatColor.DARK_PURPLE, NamedTextColor.DARK_PURPLE);
            colorMap.put(ChatColor.GOLD, NamedTextColor.GOLD);
            colorMap.put(ChatColor.GRAY, NamedTextColor.GRAY);
            colorMap.put(ChatColor.DARK_GRAY, NamedTextColor.DARK_GRAY);
            colorMap.put(ChatColor.BLUE, NamedTextColor.BLUE);
            colorMap.put(ChatColor.GREEN, NamedTextColor.GREEN);
            colorMap.put(ChatColor.AQUA, NamedTextColor.AQUA);
            colorMap.put(ChatColor.RED, NamedTextColor.RED);
            colorMap.put(ChatColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE);
            colorMap.put(ChatColor.YELLOW, NamedTextColor.YELLOW);
            colorMap.put(ChatColor.WHITE, NamedTextColor.WHITE);
        }

        public static void sendTo(CommandSender target, Component message) {
            Audience targetAudience = (Audience) target;
            targetAudience.sendMessage(toAdventure(message));
        }

        private static net.kyori.adventure.text.Component toAdventure(Component message) {
            net.kyori.adventure.text.Component result = net.kyori.adventure.text.Component.text(((TextComponent) message).getText(), toAdventure(message.getColor()));
            if (!message.getChildren().isEmpty()) {
                result = result.children(message.getChildren().parallelStream().map(AdventureSender::toAdventure).toList());
            }
            if (message.getHover() != null) {
                result = result.hoverEvent(toAdventure(message.getHover()));
            }
            if (message.getClick() != null) {
                result = result.clickEvent(toAdventure(message.getClick()));
            }
            // TODO formatings
            return result;
        }

        private static @Nullable ClickEvent toAdventure(Click click) {
            if (click instanceof RunCommandClick run) {
                return ClickEvent.runCommand(run.getCommand());
            }
            return null;
        }

        private static HoverEventSource<?> toAdventure(Hover hover) {
            if (hover instanceof TextHover text) {
                return HoverEvent.showText(toAdventure(text.getText()));
            } else if (hover instanceof ItemHover item) {
                return (HoverEventSource<?>) item.getItem();
            }
            return null;
        }

        private static TextColor toAdventure(ChatColor color) {
            return color == null ? null : colorMap.getOrDefault(color, TextColor.color(color.getColor()));
        }
    }

    private static class BungeeSender {
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

        public static void sendTo(CommandSender target, Component message) {
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
    }

    public static Component fromLegacy(String text) {
        // use bungee component converter for now
        BaseComponent component = net.md_5.bungee.api.chat.TextComponent.fromLegacy(text);
        return BungeeSender.toComponent(component);
    }

    public static String toPlainText(Component component) {
        // use bungee component converter for now
        return BungeeSender.toBungee(component).toPlainText();
    }
}
