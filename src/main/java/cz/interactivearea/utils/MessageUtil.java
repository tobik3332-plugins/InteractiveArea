package cz.interactivearea.utils;

import cz.interactivearea.InteractiveAreaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static InteractiveAreaPlugin plugin;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static void init(InteractiveAreaPlugin instance) {
        plugin = instance;
    }

    /**
     * Podporuje kombinaci MiniMessage, &#RRGGBB a klasickych & kodu.
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        // Prevod &#RRGGBB na MiniMessage <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(raw);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(buffer);
        String processed = buffer.toString();

        // Pokud obsahuje MiniMessage tagy, pouzijeme hybridni prevod
        if (processed.contains("<") && processed.contains(">")) {
            // Prevedeme legacy & kody na MiniMessage format, aby fungovalo vse najednou
            Component legacyComp = LEGACY_AMPERSAND.deserialize(processed);
            String miniSerialized = MINI_MESSAGE.serialize(legacyComp)
                    .replace("\\<", "<")
                    .replace("\\>", ">");
            return MINI_MESSAGE.deserialize(miniSerialized);
        }

        return LEGACY_AMPERSAND.deserialize(processed);
    }

    public static void sendConfigMessage(CommandSender sender, String path, String... placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages." + path, "&cChybi zprava: " + path);

        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }

        sender.sendMessage(parse(prefix + msg));
    }
}
