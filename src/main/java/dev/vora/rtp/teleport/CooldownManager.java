package dev.vora.rtp.teleport;

import dev.vora.rtp.config.Settings;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final Settings settings;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(Settings settings) {
        this.settings = settings;
    }

    // Returns remaining cooldown in milliseconds, 0 if no cooldown
    public long remaining(Player player) {
        if (settings.cooldown().millis() <= 0) {
            return 0;
        }

        if (player.hasPermission(settings.cooldown().bypassPermission())) {
            return 0;
        }

        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) {
            return 0;
        }

        long left = expiry - System.currentTimeMillis();
        return Math.max(0, left);
    }

    // Sets the cooldown for the player starting from now
    public void apply(Player player) {
        if (settings.cooldown().millis() <= 0) {
            return;
        }

        if (player.hasPermission(settings.cooldown().bypassPermission())) {
            return;
        }

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + settings.cooldown().millis());
    }

    public void clear(UUID uuid) {
        cooldowns.remove(uuid);
    }
}