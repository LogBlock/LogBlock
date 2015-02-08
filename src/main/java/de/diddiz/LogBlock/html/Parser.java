package de.diddiz.LogBlock.html;

import net.minecraft.server.v1_7_R1.ChatBaseComponent;
import net.minecraft.server.v1_7_R1.Packet;
import net.minecraft.server.v1_7_R1.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class Parser {
	/*
	 * ChatComponentText = TextComponent
	 * ChatMessage = TranslatableComponent
	 * ChatModifier = Style
	 * ChatClickable = ClickEvent
	 * ChatHoverable = HoverEvent
	 */
	public static ChatBaseComponent parse(String xmlSource, Object... params) throws JAXBException {
		xmlSource = "<span>" + xmlSource + "</span>";

		final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		final Element element = (Element) unmarshaller.unmarshal(new StringReader(xmlSource));

		return element.getDefaultNmsComponent(params);
	}

	public static ChatBaseComponent format(String format, Object... params) throws JAXBException {
		return parse(format, params);
	}

	public static boolean sendToAll(String format, Object... params) {
		return sendToPlayers(Arrays.asList(Bukkit.getOnlinePlayers()), format, params);
	}

	public static boolean sendToPlayers(List<? extends CommandSender> targetPlayers, String format, Object... params) {
		try {
			final PacketPlayOutChat packet = createChatPacket(format, params);

			for (CommandSender commandSender : targetPlayers) {
				if (!(commandSender instanceof Player)) {
					commandSender.sendMessage(parsePlain(format, params));
					continue;
				}

				sendPacketToPlayer((Player) commandSender, packet);
			}

			return true;
		}
		catch (JAXBException e) {
			e.printStackTrace();
			Bukkit.broadcastMessage("Error parsing XML");

			return false;
		}
	}

	private static void sendPacketToPlayer(Player player, Packet packet) {
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
	}

	public static boolean sendToPlayer(Player player, String format, Object... params) {
		try {
			sendPacketToPlayer(player, createChatPacket(format, params));

			return true;
		} catch (JAXBException e) {
			e.printStackTrace();
			player.sendMessage("Error parsing XML");

			return false;
		}
	}

	public static boolean sendToPlayer(CommandSender commandSender, String format, Object... params) {
		if (commandSender instanceof Player)
			return sendToPlayer((Player) commandSender, format, params);

		commandSender.sendMessage(parsePlain(format, params));
		return true;
	}

	private static String parsePlain(String format, Object[] params) {
		return String.format(format, params); // TODO: strip XML tags
	}

	private static PacketPlayOutChat createChatPacket(String format, Object... params) throws JAXBException {
		return new PacketPlayOutChat(format(format, params));
	}

	public static String escape(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&apos;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");

		return s;
	}
}
