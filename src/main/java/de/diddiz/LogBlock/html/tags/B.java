package de.diddiz.LogBlock.html.tags;

import de.diddiz.LogBlock.html.Element;
import net.minecraft.server.v1_7_R1.ChatModifier;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class B extends Element {
	@Override
	protected void modifyStyle(ChatModifier style) {
		style.setBold(true);
	}
}
