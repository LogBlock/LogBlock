package de.diddiz.LogBlock.html.tags;

import de.diddiz.LogBlock.html.Element;
import net.minecraft.server.v1_7_R1.ChatClickable;
import net.minecraft.server.v1_7_R1.ChatModifier;
import net.minecraft.server.v1_7_R1.EnumClickAction;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class A extends Element {
	@XmlAttribute(required = true)
	private String href;

	@Override
	protected void modifyStyle(ChatModifier style) {
		style.a(new ChatClickable(EnumClickAction.OPEN_URL, href));
	}
}
