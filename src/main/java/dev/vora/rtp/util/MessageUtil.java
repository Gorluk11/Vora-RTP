package dev.vora.rtp.util;

import dev.vora.rtp.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static void send(Player player, Settings settings, String message) {
        player.sendMessage(prefixed(settings, message));
    }

    public static Component prefixed(Settings settings, String message) {
        return component(settings.prefix() + message);
    }

    public static String legacyPrefixed(Settings settings, String message) {
        return legacy(settings.prefix() + message);
    }

    public static Component component(String text) {
        return LEGACY.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<Component> componentList(List<String> lines) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(component(line));
        }
        return components;
    }

    public static void sendTitle(Player player, Settings.TitleConfig config, String seconds) {
        String title = config.title();
        String subtitle = config.subtitle();

        if (seconds != null) {
            title = title.replace("{seconds}", seconds);
            subtitle = subtitle.replace("{seconds}", seconds);
        }

        Title.Times times = Title.Times.times(
                Duration.ofMillis(config.fadeIn() * 50L),
                Duration.ofMillis(config.stay() * 50L),
                Duration.ofMillis(config.fadeOut() * 50L)
        );

        player.showTitle(Title.title(component(title), component(subtitle), times));
    }

    public static void playSound(Player player, Settings.SoundConfig sound) {
        if (sound == null || !sound.enabled()) {
            return;
        }

        player.playSound(player.getLocation(), sound.sound(), sound.volume(), sound.pitch());
    }
}