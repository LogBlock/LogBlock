package de.diddiz.LogBlock.html.tags;

import de.diddiz.LogBlock.html.Element;
import net.minecraft.server.v1_7_R1.ChatBaseComponent;
import net.minecraft.server.v1_7_R1.ChatMessage;
import net.minecraft.server.v1_7_R1.ChatModifier;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class Tr extends Element {
	@XmlAttribute(required = true)
	private String key;

	@Override
	protected void modifyStyle(ChatModifier style) {
		// TODO: get rid of this
	}

	@Override
	public List<ChatBaseComponent> getNmsComponents(ChatModifier style, boolean condenseElements, Object[] params) throws JAXBException {
		final List<ChatBaseComponent> components = super.getNmsComponents(style, true, params);

		final ChatBaseComponent translateComponent = new ChatMessage(key, components.toArray());
		translateComponent.setChatModifier(style);
		return Arrays.asList(translateComponent);
	}
}
