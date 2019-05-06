package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.TextFormat;
import io.manebot.chat.TextStyle;
import io.manebot.platform.PlatformUser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class TeamspeakTextFormat implements TextFormat {
    public static final TeamspeakTextFormat INSTANCE = new TeamspeakTextFormat();

    @Override
    public boolean shouldMention(PlatformUser user) {
        return user.isConnected();
    }

    @Override
    public String mention(Chat target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String mention(PlatformUser user) {
        return TextFormat.super.mention(user);
    }

    @Override
    public String format(String string, EnumSet<TextStyle> styles) {
        if (string.trim().length() <= 0) return string;

        List<TextStyle> list = new ArrayList<>(styles);

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i ++) {
            TextStyle style = list.get(i);
            String key = styleToKey(style);
            if (key != null) builder.append("[").append(key).append("]");
        }

        builder.append(escape(string));

        for (int i = list.size()-1; i >= 0; i --) {
            TextStyle style = list.get(i);
            String key = styleToKey(style);
            if (key != null) builder.append("[/").append(key).append("]");
        }

        return builder.toString();
    }

    private static String styleToKey(TextStyle style) {
        switch (style) {
            case BOLD:
                return "b";
            case ITALICS:
                return "i";
            case STRIKE_THROUGH:
                return "s";
            case UNDERLINE:
                return "u";
            default:
                return null;
        }
    }

    @Override
    public String escape(String string) {
        StringBuilder builder = new StringBuilder();

        for (char character : string.toCharArray()) {
            if (character == '[' || character == ']') builder.append('\\');
            builder.append(character);
        }

        return builder.toString();
    }

}
