package dev.vora.rtp.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Settings {

    private final String prefix;
    private final Messages messages;
    private final CooldownConfig cooldown;
    private final CountdownConfig countdown;
    private final TitleConfig countdownTitle;
    private final TitleConfig successTitle;
    private final Map<String, SoundConfig> sounds;
    private final Menu menu;
    private final WorldNames worldNames;
    private final TeleportConfig teleport;

    private Settings(
            String prefix,
            Messages messages,
            CooldownConfig cooldown,
            CountdownConfig countdown,
            TitleConfig countdownTitle,
            TitleConfig successTitle,
            Map<String, SoundConfig> sounds,
            Menu menu,
            WorldNames worldNames,
            TeleportConfig teleport
    ) {
        this.prefix = prefix;
        this.messages = messages;
        this.cooldown = cooldown;
        this.countdown = countdown;
        this.countdownTitle = countdownTitle;
        this.successTitle = successTitle;
        this.sounds = sounds;
        this.menu = menu;
        this.worldNames = worldNames;
        this.teleport = teleport;
    }

    public static Settings load(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        String prefix = c.getString("prefix", "&8❖ &fᴠᴏʀᴀ &8» ");

        Messages messages = new Messages(
                c.getString("messages.player-only", "&fBu komut sadece oyuncular içindir."),
                c.getString("messages.no-permission", "&cBu komutu kullanma iznin yok."),
                c.getString("messages.busy", "&cZaten bir işlem devam ediyor."),
                c.getString("messages.cooldown", "&cRTP için {seconds} saniye bekle."),
                c.getString("messages.countdown-cancelled", "&cHareket ettiğin için RTP iptal edildi."),
                c.getString("messages.teleporting", "&fRTP başlatılıyor..."),
                c.getString("messages.no-world", "&cHedef dünya bulunamadı."),
                c.getString("messages.failed", "&cGüvenli bir konum bulunamadı."),
                c.getString("messages.teleport-failed", "&cRTP başarısız oldu."),
                c.getString("messages.success", "&aRTP tamamlandı."),
                c.getString("messages.reloaded", "&aConfig yeniden yüklendi."),
                c.getString("messages.world-disabled", "&cBu dünya kapalı.")
        );

        CooldownConfig cooldown = new CooldownConfig(
                Math.max(0, c.getInt("cooldown.seconds", 5)) * 1000L,
                c.getString("cooldown.bypass-permission", "vora.rtp.bypass.cooldown")
        );

        CountdownConfig countdown = new CountdownConfig(
                Math.max(0, c.getInt("countdown.seconds", 5)),
                c.getBoolean("countdown.cancel-on-move", true)
        );

        TitleConfig countdownTitle = readTitle(c, "titles.countdown");
        TitleConfig successTitle = readTitle(c, "titles.success");

        Map<String, SoundConfig> sounds = new HashMap<>();
        ConfigurationSection soundSection = c.getConfigurationSection("sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, readSound(c, "sounds." + key));
            }
        }

        int menuSize = normalizeMenuSize(c.getInt("menu.size", 27));
        Material filler = readMaterial(c.getString("menu.filler"), Material.BLACK_STAINED_GLASS_PANE);

        // disabled item config for worlds that are turned off
        Material disabledMat = readMaterial(c.getString("menu.worlds.disabled-item.material"), Material.BARRIER);
        String disabledName = c.getString("menu.worlds.disabled-item.name", "&c&lᴋᴀᴘᴀʟı");
        List<String> disabledLore = c.getStringList("menu.worlds.disabled-item.lore");
        DisabledItem disabledItem = new DisabledItem(disabledMat, disabledName, disabledLore);

        MenuOption normal = readMenuOption(c, "menu.worlds.normal", World.Environment.NORMAL, 10, Material.GRASS_BLOCK, menuSize);
        MenuOption nether = readMenuOption(c, "menu.worlds.nether", World.Environment.NETHER, 13, Material.NETHERRACK, menuSize);
        MenuOption end = readMenuOption(c, "menu.worlds.end", World.Environment.THE_END, 16, Material.END_STONE, menuSize);

        Menu menu = new Menu(c.getString("menu.title", "&8VORA │ RTP"), menuSize, filler, normal, nether, end, disabledItem);

        WorldNames worldNames = new WorldNames(
                c.getString("worlds.normal", "world"),
                c.getString("worlds.nether", "world_nether"),
                c.getString("worlds.end", "world_the_end")
        );

        LocationCacheConfig cacheConfig = new LocationCacheConfig(
                c.getBoolean("teleport.location-cache.enabled", true),
                Math.max(1, c.getInt("teleport.location-cache.size-per-world", 5)),
                Math.max(20L, c.getLong("teleport.location-cache.refill-interval-ticks", 600L))
        );

        TeleportConfig teleport = new TeleportConfig(
                Math.max(1, c.getInt("teleport.max-attempts", 30)),
                Math.max(1000L, c.getLong("teleport.chunk-timeout-ms", 10000L)),
                cacheConfig,
                new OverworldConfig(
                        Math.max(0, c.getInt("teleport.overworld.min-distance", 200)),
                        Math.max(1, c.getInt("teleport.overworld.max-distance", 5000)),
                        Math.max(1, c.getInt("teleport.overworld.search-depth", 8))
                ),
                new NetherConfig(
                        Math.max(0, c.getInt("teleport.nether.min-distance", 100)),
                        Math.max(1, c.getInt("teleport.nether.max-distance", 2000)),
                        c.getInt("teleport.nether.min-y", 24),
                        c.getInt("teleport.nether.max-y", 118)
                ),
                new EndConfig(
                        Math.max(0, c.getInt("teleport.end.min-distance", 200)),
                        Math.max(1, c.getInt("teleport.end.max-distance", 3000)),
                        Math.max(1, c.getInt("teleport.end.search-depth", 8)),
                        Math.max(1, c.getInt("teleport.end.support-check-depth", 8)),
                        Math.max(1, c.getInt("teleport.end.min-supported-columns", 2))
                ),
                readMaterialSet(c.getStringList("teleport.dangerous-materials")),
                readMaterialSet(c.getStringList("teleport.unsafe-ground-materials")),
                readMaterialSet(c.getStringList("teleport.unsafe-near-materials"))
        );

        return new Settings(prefix, messages, cooldown, countdown, countdownTitle, successTitle, sounds, menu, worldNames, teleport);
    }

    private static TitleConfig readTitle(FileConfiguration c, String path) {
        return new TitleConfig(
                c.getString(path + ".title", ""),
                c.getString(path + ".subtitle", ""),
                c.getInt(path + ".fade-in", 0),
                c.getInt(path + ".stay", 20),
                c.getInt(path + ".fade-out", 5)
        );
    }

    private static SoundConfig readSound(FileConfiguration c, String path) {
        boolean enabled = c.getBoolean(path + ".enabled", true);
        Sound sound;
        try {
            sound = Sound.valueOf(c.getString(path + ".sound", "UI_BUTTON_CLICK"));
        } catch (Exception ignored) {
            sound = Sound.UI_BUTTON_CLICK;
        }
        float volume = (float) c.getDouble(path + ".volume", 0.7D);
        float pitch = (float) c.getDouble(path + ".pitch", 1.0D);
        return new SoundConfig(enabled, sound, volume, pitch);
    }

    private static MenuOption readMenuOption(FileConfiguration c, String path, World.Environment env, int defSlot, Material defMat, int menuSize) {
        boolean enabled = c.getBoolean(path + ".enabled", true);
        int slot = c.getInt(path + ".slot", defSlot);
        if (slot < 0 || slot >= menuSize) slot = defSlot;

        return new MenuOption(
                env, enabled, slot,
                readMaterial(c.getString(path + ".material"), defMat),
                c.getString(path + ".name", "&fOption"),
                c.getStringList(path + ".lore")
        );
    }

    private static int normalizeMenuSize(int size) {
        int n = Math.max(9, Math.min(54, size));
        n -= n % 9;
        return Math.max(9, n);
    }

    private static Material readMaterial(String value, Material fallback) {
        if (value == null || value.isBlank()) return fallback;
        Material m = Material.matchMaterial(value);
        return m != null ? m : fallback;
    }

    private static Set<Material> readMaterialSet(List<String> values) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String v : values) {
            Material m = Material.matchMaterial(v);
            if (m != null) set.add(m);
        }
        return set;
    }

    public String prefix() { return prefix; }
    public Messages messages() { return messages; }
    public CooldownConfig cooldown() { return cooldown; }
    public CountdownConfig countdown() { return countdown; }
    public TitleConfig countdownTitle() { return countdownTitle; }
    public TitleConfig successTitle() { return successTitle; }
    public SoundConfig sound(String key) { return sounds.getOrDefault(key, SoundConfig.disabled()); }
    public Menu menu() { return menu; }
    public WorldNames worldNames() { return worldNames; }
    public TeleportConfig teleport() { return teleport; }

    public record Messages(
            String playerOnly, String noPermission, String busy, String cooldown,
            String countdownCancelled, String teleporting, String noWorld,
            String failed, String teleportFailed, String success,
            String reloaded, String worldDisabled
    ) {}

    public record CooldownConfig(long millis, String bypassPermission) {}
    public record CountdownConfig(int seconds, boolean cancelOnMove) {}
    public record TitleConfig(String title, String subtitle, int fadeIn, int stay, int fadeOut) {}

    public record SoundConfig(boolean enabled, Sound sound, float volume, float pitch) {
        public static SoundConfig disabled() {
            return new SoundConfig(false, Sound.UI_BUTTON_CLICK, 0F, 0F);
        }
    }

    public record DisabledItem(Material material, String name, List<String> lore) {}

    public record Menu(String title, int size, Material filler, MenuOption normal, MenuOption nether, MenuOption end, DisabledItem disabledItem) {
        public MenuOption bySlot(int slot) {
            if (normal.slot() == slot) return normal;
            if (nether.slot() == slot) return nether;
            if (end.slot() == slot) return end;
            return null;
        }
    }

    public record MenuOption(World.Environment environment, boolean enabled, int slot, Material material, String name, List<String> lore) {}
    public record WorldNames(String normal, String nether, String end) {}
    public record LocationCacheConfig(boolean enabled, int sizePerWorld, long refillIntervalTicks) {}

    public record TeleportConfig(
            int maxAttempts, long chunkTimeoutMs, LocationCacheConfig cache,
            OverworldConfig overworld, NetherConfig nether, EndConfig end,
            Set<Material> dangerousMaterials, Set<Material> unsafeGroundMaterials, Set<Material> unsafeNearMaterials
    ) {}

    public record OverworldConfig(int minDistance, int maxDistance, int searchDepth) {}
    public record NetherConfig(int minDistance, int maxDistance, int minY, int maxY) {}
    public record EndConfig(int minDistance, int maxDistance, int searchDepth, int supportCheckDepth, int minSupportedColumns) {}
}