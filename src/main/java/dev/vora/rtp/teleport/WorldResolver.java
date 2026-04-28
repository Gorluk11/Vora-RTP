package dev.vora.rtp.teleport;

import dev.vora.rtp.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.Map;

// Caches world references so we don't iterate Bukkit.getWorlds() every teleport
public final class WorldResolver {

    private final Settings settings;
    private final Map<World.Environment, World> cache = new EnumMap<>(World.Environment.class);

    public WorldResolver(Settings settings) {
        this.settings = settings;
        refresh();
    }

    public void refresh() {
        cache.clear();

        cache.put(World.Environment.NORMAL, findWorld(settings.worldNames().normal(), World.Environment.NORMAL));
        cache.put(World.Environment.NETHER, findWorld(settings.worldNames().nether(), World.Environment.NETHER));
        cache.put(World.Environment.THE_END, findWorld(settings.worldNames().end(), World.Environment.THE_END));
    }

    public World resolve(World.Environment environment) {
        World world = cache.get(environment);

        // If cached world got unloaded, try to find it again
        if (world == null) {
            world = findWorld(nameFor(environment), environment);
            if (world != null) {
                cache.put(environment, world);
            }
        }

        return world;
    }

    private String nameFor(World.Environment env) {
        return switch (env) {
            case NORMAL -> settings.worldNames().normal();
            case NETHER -> settings.worldNames().nether();
            case THE_END -> settings.worldNames().end();
            default -> "";
        };
    }

    private static World findWorld(String name, World.Environment environment) {
        if (name != null && !name.isBlank()) {
            World byName = Bukkit.getWorld(name);
            if (byName != null && byName.getEnvironment() == environment) {
                return byName;
            }
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                return world;
            }
        }

        return null;
    }
}