package dev.vora.rtp.teleport;

import dev.vora.rtp.config.Settings;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {

    private final Settings.TeleportConfig config;

    public SafeLocationFinder(Settings.TeleportConfig config) {
        this.config = config;
    }

    public Location randomBase(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int minDistance;
        int maxDistance;

        switch (world.getEnvironment()) {
            case NETHER -> {
                minDistance = config.nether().minDistance();
                maxDistance = config.nether().maxDistance();
            }
            case THE_END -> {
                minDistance = config.end().minDistance();
                maxDistance = config.end().maxDistance();
            }
            default -> {
                minDistance = config.overworld().minDistance();
                maxDistance = config.overworld().maxDistance();
            }
        }

        int x = randomCoord(random, minDistance, maxDistance);
        int z = randomCoord(random, minDistance, maxDistance);
        return new Location(world, x + 0.5D, 0.0D, z + 0.5D);
    }

    public Location findSafe(Location base) {
        World world = base.getWorld();
        if (world == null) return null;

        int x = base.getBlockX();
        int z = base.getBlockZ();

        return switch (world.getEnvironment()) {
            case NETHER -> findSafeNether(world, x, z);
            case THE_END -> findSafeEnd(world, x, z);
            default -> findSafeNormal(world, x, z);
        };
    }

    private Location findSafeNormal(World world, int x, int z) {
        int topY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (topY <= world.getMinHeight()) return null;

        int minY = Math.max(world.getMinHeight() + 1, topY - config.overworld().searchDepth());

        for (int y = topY; y >= minY; y--) {
            Location safe = testLocation(world, x, y, z);
            if (safe != null) return safe;
        }
        return null;
    }

    private Location findSafeEnd(World world, int x, int z) {
        int topY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        if (topY <= world.getMinHeight()) return null;

        int minY = Math.max(world.getMinHeight() + 1, topY - config.end().searchDepth());

        for (int y = topY; y >= minY; y--) {
            Location safe = testLocation(world, x, y, z);
            if (safe != null && hasEndSupport(world, x, y, z)) return safe;
        }
        return null;
    }

    private Location findSafeNether(World world, int x, int z) {
        int minY = Math.max(world.getMinHeight() + 1, config.nether().minY());
        int maxY = Math.min(world.getMaxHeight() - 3, config.nether().maxY());
        if (minY > maxY) return null;

        int startY = ThreadLocalRandom.current().nextInt(minY, maxY + 1);

        for (int offset = 0; offset <= (maxY - minY); offset++) {
            int up = startY + offset;
            if (up <= maxY) {
                Location safe = testLocation(world, x, up, z);
                if (safe != null && isNetherExtraSafe(world, x, up, z)) return safe;
            }

            if (offset == 0) continue;

            int down = startY - offset;
            if (down >= minY) {
                Location safe = testLocation(world, x, down, z);
                if (safe != null && isNetherExtraSafe(world, x, down, z)) return safe;
            }
        }
        return null;
    }

    private Location testLocation(World world, int x, int groundY, int z) {
        Block ground = world.getBlockAt(x, groundY, z);
        Block feet = world.getBlockAt(x, groundY + 1, z);
        Block head = world.getBlockAt(x, groundY + 2, z);

        if (!isSafeGround(ground)) return null;
        if (!isSafeSpace(feet) || !isSafeSpace(head)) return null;
        if (!isNearbyAreaSafe(world, x, groundY + 1, z)) return null;

        return new Location(world, x + 0.5D, groundY + 1.0D, z + 0.5D);
    }

    private boolean isSafeGround(Block block) {
        Material type = block.getType();
        if (type.isAir() || !type.isSolid()) return false;
        return !config.unsafeGroundMaterials().contains(type);
    }

    private boolean isSafeSpace(Block block) {
        Material type = block.getType();
        if (type.isSolid()) return false;
        return !config.dangerousMaterials().contains(type);
    }

    private boolean isNearbyAreaSafe(World world, int x, int feetY, int z) {
        Set<Material> unsafeNear = config.unsafeNearMaterials();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (unsafeNear.contains(world.getBlockAt(x + dx, feetY, z + dz).getType())) return false;
                if (unsafeNear.contains(world.getBlockAt(x + dx, feetY - 1, z + dz).getType())) return false;
            }
        }
        return true;
    }

    private boolean hasEndSupport(World world, int x, int groundY, int z) {
        int supportedColumns = 0;
        int depth = config.end().supportCheckDepth();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int minY = Math.max(world.getMinHeight(), groundY - depth);
                for (int y = groundY; y >= minY; y--) {
                    if (world.getBlockAt(x + dx, y, z + dz).getType().isSolid()) {
                        supportedColumns++;
                        break;
                    }
                }
            }
        }
        return supportedColumns >= config.end().minSupportedColumns();
    }

    private boolean isNetherExtraSafe(World world, int x, int groundY, int z) {
        Material aboveHead = world.getBlockAt(x, groundY + 3, z).getType();
        return !config.dangerousMaterials().contains(aboveHead);
    }

    private int randomCoord(ThreadLocalRandom random, int min, int max) {
        int value = random.nextInt(min, max + 1);
        return random.nextBoolean() ? value : -value;
    }
}