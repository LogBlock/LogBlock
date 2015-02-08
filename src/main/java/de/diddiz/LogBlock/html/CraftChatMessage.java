package de.diddiz.LogBlock.html;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.server.v1_7_R1.ChatClickable;
import net.minecraft.server.v1_7_R1.ChatComponentText;
import net.minecraft.server.v1_7_R1.ChatModifier;
import net.minecraft.server.v1_7_R1.EnumChatFormat;
import net.minecraft.server.v1_7_R1.EnumClickAction;
import net.minecraft.server.v1_7_R1.IChatBaseComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CraftChatMessage {
	private static class FromString {
		private static final Map<Character, EnumChatFormat> formatMap;

		static {
			Builder<Character, EnumChatFormat> builder = ImmutableMap.builder();
			for (EnumChatFormat format : EnumChatFormat.values()) {
				builder.put(format.getChar(), format);
			}
			formatMap = builder.build();
		}

		private final List<IChatBaseComponent> list = new ArrayList<IChatBaseComponent>();
		private IChatBaseComponent currentChatComponent = new ChatComponentText("");
		private ChatModifier defaultModifier;
		private ChatModifier modifier = new ChatModifier();
		private StringBuilder builder = new StringBuilder();
		private final IChatBaseComponent[] output;
		private static final Pattern url = Pattern.compile("^(\u00A7.)*?((?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*?)?)(\u00A7.)*?$");
		private int lastWord = 0;

		private FromString(String message) {
			this(message, new ChatModifier());
		}

		private FromString(String message, ChatModifier style) {
			modifier = (defaultModifier = style).clone();
			if (message == null) {
				output = new IChatBaseComponent[] { currentChatComponent };
				return;
			}
			list.add(currentChatComponent);

			EnumChatFormat format = null;
			Matcher matcher = url.matcher(message);
			lastWord = 0;

			for (int i = 0; i < message.length(); i++) {
				char currentChar = message.charAt(i);
				if (currentChar == '\u00A7' && (i < (message.length() - 1)) && (format = formatMap.get(message.charAt(i + 1))) != null) {
					checkUrl(matcher, message, i, false);
					if (builder.length() > 0) {
						appendNewComponent();
					}

					if (format == EnumChatFormat.RESET) {
						modifier = defaultModifier.clone();
					} else if (format.isFormat()) {
						switch (format) {
						case BOLD:
							modifier.setBold(Boolean.TRUE);
							break;
						case ITALIC:
							modifier.setItalic(Boolean.TRUE);
							break;
						case STRIKETHROUGH:
							modifier.setStrikethrough(Boolean.TRUE);
							break;
						case UNDERLINE:
							modifier.setUnderline(Boolean.TRUE);
							break;
						case RANDOM:
							modifier.setRandom(Boolean.TRUE);
							break;
						default:
							throw new AssertionError("Unexpected message format");
						}
					} else { // Color resets formatting
						modifier = defaultModifier.clone().setColor(format);
					}
					i++;
				} else if (currentChar == '\n') {
					if (builder.length() > 0) {
						appendNewComponent();
					}
					currentChatComponent = null;
				} else {
					if (currentChar == ' ' || i == message.length() - 1) {
						if (checkUrl(matcher, message, i, true)) {
							break;
						}
					}
					builder.append(currentChar);
				}
			}

			if (builder.length() > 0) {
				appendNewComponent();
			}

			output = list.toArray(new IChatBaseComponent[0]);
		}

		private boolean checkUrl(Matcher matcher, String message, int i, boolean newWord) {
			Matcher urlMatcher = matcher.region(lastWord, i == message.length() - 1 ? message.length() : i);
			if (newWord) {
				lastWord = i + 1;
			}
			if (urlMatcher.find()) {
				String fullUrl = urlMatcher.group(2);
				String protocol = urlMatcher.group(3);
				String url = urlMatcher.group(4);
				String path = urlMatcher.group(5);
				builder.delete(builder.length() - fullUrl.length() + (i == message.length() - 1 ? 1 : 0), builder.length());
				if (builder.length() > 0) {
					appendNewComponent();
				}
				builder.append(fullUrl);
				ChatClickable link = new ChatClickable(EnumClickAction.OPEN_URL,
						(protocol!=null?protocol:"http") + "://" + url + (path!=null?path:""));
				modifier.a(link);
				appendNewComponent();
				modifier.a((ChatClickable) null);
				if (!newWord) { //Force new word to prevent double checking
					lastWord = i + 1;
				}
				if (i == message.length() - 1) {
					return true;
				}
			}
			return false;
		}

		private void appendNewComponent() {
			IChatBaseComponent addition = new ChatComponentText(builder.toString()).setChatModifier(modifier);
			builder = new StringBuilder();
			modifier = modifier.clone();
			if (currentChatComponent == null) {
				currentChatComponent = new ChatComponentText("");
				list.add(currentChatComponent);
			}
			currentChatComponent.a(addition);
		}

		private IChatBaseComponent[] getOutput() {
			return output;
		}
	}

	public static IChatBaseComponent[] fromString(String message) {
		return new FromString(message).getOutput();
	}

	private CraftChatMessage() {
	}

	public static IChatBaseComponent[] fromString(String message, ChatModifier style) {
		return new FromString(message, style).getOutput();
	}
}
