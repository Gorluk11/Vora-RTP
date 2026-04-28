package dev.vora.rtp.teleport;

import dev.vora.rtp.VoraRTP;
import dev.vora.rtp.config.Settings;
import dev.vora.rtp.util.MessageUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CountdownManager {

    private final VoraRTP plugin;
    private final Settings settings;
    private final TeleportHandler teleportHandler;

    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> remainingSeconds = new ConcurrentHashMap<>();
    private final Map<UUID, World.Environment> targetEnvironments = new ConcurrentHashMap<>();

    public CountdownManager(VoraRTP plugin, Settings settings, TeleportHandler teleportHandler) {
        this.plugin = plugin;
        this.settings = settings;
        this.teleportHandler = teleportHandler;
    }

    public boolean isActive(UUID uuid) {
        return tasks.containsKey(uuid);
    }

    public void startCountdown(Player player, World.Environment environment) {
        UUID uuid = player.getUniqueId();

        if (isActive(uuid) || teleportHandler.isBusy(uuid)) {
            return;
        }

        int seconds = settings.countdown().seconds();
        if (seconds <= 0) {
            beginTeleport(player, environment);
            return;
        }

        remainingSeconds.put(uuid, seconds);
        targetEnvironments.put(uuid, environment);
        showTick(player, seconds);

        ScheduledTask task = player.getScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> tick(player),
                () -> cleanup(uuid),
                20L,
                20L
        );

        if (task != null) {
            tasks.put(uuid, task);
        }
    }

    private void tick(Player player) {
        UUID uuid = player.getUniqueId();

        if (!player.isOnline()) {
            cancel(player);
            return;
        }

        Integer current = remainingSeconds.get(uuid);
        if (current == null) {
            cancel(player);
            return;
        }

        int next = current - 1;
        if (next <= 0) {
            World.Environment environment = targetEnvironments.get(uuid);
            cleanup(uuid);
            if (environment == null) environment = World.Environment.NORMAL;
            beginTeleport(player, environment);
            return;
        }

        remainingSeconds.put(uuid, next);
        showTick(player, next);
    }

    private void showTick(Player player, int seconds) {
        MessageUtil.sendTitle(player, settings.countdownTitle(), String.valueOf(seconds));
        MessageUtil.playSound(player, settings.sound("countdown-tick"));
    }

    private void beginTeleport(Player player, World.Environment environment) {
        MessageUtil.send(player, settings, settings.messages().teleporting());

        teleportHandler.teleportAsync(player, environment).whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("RTP error (" + player.getName() + "): " + throwable.getMessage());
            }
        });
    }

    public void cancel(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isActive(uuid)) return;

        cleanup(uuid);

        if (player.isOnline()) {
            MessageUtil.send(player, settings, settings.messages().countdownCancelled());
            MessageUtil.playSound(player, settings.sound("countdown-cancel"));
            player.resetTitle();
        }
    }

    private void cleanup(UUID uuid) {
        remainingSeconds.remove(uuid);
        targetEnvironments.remove(uuid);
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void cancelAll() {
        for (ScheduledTask task : tasks.values()) task.cancel();
        tasks.clear();
        remainingSeconds.clear();
        targetEnvironments.clear();
    }
}