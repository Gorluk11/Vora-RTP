package dev.vora.rtp.teleport;

import dev.vora.rtp.VoraRTP;
import dev.vora.rtp.config.Settings;
import dev.vora.rtp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TeleportHandler {

    private final VoraRTP plugin;
    private final Settings settings;
    private final SafeLocationFinder finder;
    private final WorldResolver worldResolver;
    private final Set<UUID> busyPlayers = ConcurrentHashMap.newKeySet();

    public TeleportHandler(VoraRTP plugin, Settings settings, WorldResolver worldResolver) {
        this.plugin = plugin;
        this.settings = settings;
        this.worldResolver = worldResolver;
        this.finder = new SafeLocationFinder(settings.teleport());
    }

    public boolean isBusy(UUID uuid) {
        return busyPlayers.contains(uuid);
    }

    public CompletableFuture<Boolean> teleportAsync(Player player, World.Environment environment) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        UUID uuid = player.getUniqueId();

        if (!busyPlayers.add(uuid)) {
            future.complete(false);
            return future;
        }

        future.whenComplete((result, throwable) -> busyPlayers.remove(uuid));

        World targetWorld = worldResolver.resolve(environment);
        if (targetWorld == null) {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    MessageUtil.send(player, settings, settings.messages().noWorld());
                }
                future.complete(false);
            }, () -> future.complete(false));
            return future;
        }

        // Try the pre-generated location cache first
        Location cached = plugin.locationCache().poll(environment);
        if (cached != null && cached.getWorld() != null) {
            executeTeleport(player, cached, future);
            return future;
        }

        // Fall back to searching
        Bukkit.getAsyncScheduler().runNow(plugin, task -> search(player, targetWorld, 0, future));
        return future;
    }

    private void search(Player player, World world, int attempt, CompletableFuture<Boolean> future) {
        if (!player.isOnline()) {
            future.complete(false);
            return;
        }

        if (attempt >= settings.teleport().maxAttempts()) {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    MessageUtil.send(player, settings, settings.messages().failed());
                }
                future.complete(false);
            }, () -> future.complete(false));
            return;
        }

        Location base = finder.randomBase(world);
        int cx = base.getBlockX() >> 4;
        int cz = base.getBlockZ() >> 4;

        world.getChunkAtAsync(cx, cz)
                .orTimeout(settings.teleport().chunkTimeoutMs(), TimeUnit.MILLISECONDS)
                .whenComplete((chunk, err) -> {
                    if (err != null || chunk == null) {
                        retry(player, world, attempt + 1, future);
                        return;
                    }

                    Bukkit.getRegionScheduler().run(plugin, world, cx, cz, regionTask -> {
                        Location safe = finder.findSafe(base);
                        if (safe == null) {
                            retry(player, world, attempt + 1, future);
                            return;
                        }
                        executeTeleport(player, safe, future);
                    });
                });
    }

    private void retry(Player player, World world, int next, CompletableFuture<Boolean> future) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> search(player, world, next, future));
    }

    private void executeTeleport(Player player, Location location, CompletableFuture<Boolean> future) {
        if (!player.isOnline()) {
            future.complete(false);
            return;
        }

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                future.complete(false);
                return;
            }

            player.teleportAsync(location).whenComplete((success, throwable) -> {
                player.getScheduler().run(plugin, msgTask -> {
                    if (!player.isOnline()) {
                        future.complete(false);
                        return;
                    }

                    boolean ok = throwable == null && Boolean.TRUE.equals(success);

                    if (ok) {
                        MessageUtil.send(player, settings, settings.messages().success());
                        MessageUtil.sendTitle(player, settings.successTitle(), null);
                        MessageUtil.playSound(player, settings.sound("teleport"));

                        // Apply cooldown after successful teleport
                        plugin.cooldownManager().apply(player);
                    } else {
                        MessageUtil.send(player, settings, settings.messages().teleportFailed());
                    }

                    future.complete(ok);
                }, () -> future.complete(false));
            });
        }, () -> future.complete(false));
    }
}