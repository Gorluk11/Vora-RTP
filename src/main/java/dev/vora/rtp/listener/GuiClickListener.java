package dev.vora.rtp.listener;

import dev.vora.rtp.VoraRTP;
import dev.vora.rtp.config.Settings;
import dev.vora.rtp.gui.RTPMenuHolder;
import dev.vora.rtp.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuiClickListener implements Listener {

    private final VoraRTP plugin;

    public GuiClickListener(VoraRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof RTPMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Settings settings = plugin.settings();
        Settings.MenuOption option = settings.menu().bySlot(rawSlot);
        if (option == null) {
            return;
        }

        // World disabled in config
        if (!option.enabled()) {
            MessageUtil.send(player, settings, settings.messages().worldDisabled());
            MessageUtil.playSound(player, settings.sound("countdown-cancel"));
            return;
        }

        if (plugin.countdownManager().isActive(player.getUniqueId())
                || plugin.teleportHandler().isBusy(player.getUniqueId())) {
            MessageUtil.send(player, settings, settings.messages().busy());
            return;
        }

        // Cooldown check inside GUI too
        long remaining = plugin.cooldownManager().remaining(player);
        if (remaining > 0) {
            String seconds = String.valueOf((int) Math.ceil(remaining / 1000.0));
            String msg = settings.messages().cooldown().replace("{seconds}", seconds);
            MessageUtil.send(player, settings, msg);
            return;
        }

        player.closeInventory();
        MessageUtil.playSound(player, settings.sound("menu-click"));
        plugin.countdownManager().startCountdown(player, option.environment());
    }
}