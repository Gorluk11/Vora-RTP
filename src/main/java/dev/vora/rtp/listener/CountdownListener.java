package dev.vora.rtp.listener;

import dev.vora.rtp.config.Settings;
import dev.vora.rtp.teleport.CountdownManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CountdownListener implements Listener {

    private final Settings settings;
    private final CountdownManager countdownManager;

    public CountdownListener(Settings settings, CountdownManager countdownManager) {
        this.settings = settings;
        this.countdownManager = countdownManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!settings.countdown().cancelOnMove()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Only real position change cancels countdown
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (countdownManager.isActive(player.getUniqueId())) {
            countdownManager.cancel(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        countdownManager.cancel(event.getPlayer());
    }
}