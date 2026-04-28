package dev.vora.rtp.teleport;

import dev.vora.rtp.VoraRTP;
import dev.vora.rtp.config.Settings;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

// Pre-generates safe locations asynchronously so teleports feel instant
public final class LocationCache {

    private final VoraRTP plugin;
    private final Settings settings;
    private final WorldResolver worldResolver;
    private final SafeLocationFinder finder;

    private final Map<World.Environment, Queue<Location>> cache = new ConcurrentHashMap<>();
    private ScheduledTask refillTask;

    public LocationCache(VoraRTP plugin, Settings settings, WorldResolver worldResolver) {
        this.plugin = plugin;
        this.settings = settings;
        this.worldResolver = worldResolver;
        this.finder = new SafeLocationFinder(settings.teleport());

        cache.put(World.Environment.NORMAL, new ConcurrentLinkedQueue<>());
        cache.put(World.Environment.NETHER, new ConcurrentLinkedQueue<>());
        cache.put(World.Environment.THE_END, new ConcurrentLinkedQueue<>());
    }

    // Polls a cached location if available, null otherwise
    public Location poll(World.Environment environment) {
        Queue<Location> queue = cache.get(environment);
        return queue != null ? queue.poll() : null;
    }

    public void start() {
        if (!settings.teleport().cache().enabled()) {
            return;
        }

        long interval = settings.teleport().cache().refillIntervalTicks();

        // Refill runs on the async scheduler
        refillTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> refillAll(),
                interval * 50L,
                interval * 50L,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (refillTask != null) {
            refillTask.cancel();
            refillTask = null;
        }

        for (Queue<Location> queue : cache.values()) {
            queue.clear();
        }
    }

    private void refillAll() {
        refillEnvironment(World.Environment.NORMAL);
        refillEnvironment(World.Environment.NETHER);
        refillEnvironment(World.Environment.THE_END);
    }

    private void refillEnvironment(World.Environment environment) {
        Queue<Location> queue = cache.get(environment);
        if (queue == null) return;

        int maxSize = settings.teleport().cache().sizePerWorld();
        if (queue.size() >= maxSize) return;

        World world = worldResolver.resolve(environment);
        if (world == null) return;

        int needed = maxSize - queue.size();

        for (int i = 0; i < needed; i++) {
            tryGenerateOne(world, environment, queue);
        }
    }

    private void tryGenerateOne(World world, World.Environment environment, Queue<Location> queue) {
        int attempts = settings.teleport().maxAttempts();

        for (int attempt = 0; attempt < attempts; attempt++) {
            Location base = finder.randomBase(world);
            int cx = base.getBlockX() >> 4;
            int cz = base.getBlockZ() >> 4;

            try {
                // Block on the async thread to load the chunk and find a safe spot
                world.getChunkAtAsync(cx, cz)
                        .orTimeout(settings.teleport().chunkTimeoutMs(), TimeUnit.MILLISECONDS)
                        .thenCompose(chunk -> {
                            var future = new java.util.concurrent.CompletableFuture<Location>();

                            Bukkit.getRegionScheduler().run(plugin, world, cx, cz, regionTask -> {
                                Location safe = finder.findSafe(base);
                                future.complete(safe);
                            });

                            return future;
                        })
                        .thenAccept(safe -> {
                            if (safe != null) {
                                queue.offer(safe);
                            }
                        })
                        .get(settings.teleport().chunkTimeoutMs() + 5000L, TimeUnit.MILLISECONDS);

                // If we got one, stop trying
                if (!queue.isEmpty()) {
                    return;
                }
            } catch (Exception ignored) {
                // Timeout or interruption, try next attempt
            }
        }
    }
}