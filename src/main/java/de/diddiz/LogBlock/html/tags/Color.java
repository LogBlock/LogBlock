package de.diddiz.LogBlock.html.tags;

import de.diddiz.LogBlock.html.Element;
import net.minecraft.server.v1_7_R1.ChatModifier;
import net.minecraft.server.v1_7_R1.EnumChatFormat;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Color extends Element {
	@XmlAttribute
	private String name;

	@XmlAttribute
	private String id;

	@Override
	protected void modifyStyle(ChatModifier style) {
		if (name != null) {
			style.setColor(EnumChatFormat.b(name.toUpperCase())); // v1_7_R1
			return;
		}

		if (id == null) {
			return;
		}

		if (id.isEmpty()) {
			return;
		}

		for (EnumChatFormat enumChatFormat : EnumChatFormat.values()) {
			if (enumChatFormat.isFormat()) {
				continue;
			}

			if (enumChatFormat.getChar() != id.charAt(0)) {
				continue;
			}

			style.setColor(enumChatFormat);
			return;
		}

		throw new RuntimeException("Invalid style character " + id.charAt(0) + "!");
	}
}
