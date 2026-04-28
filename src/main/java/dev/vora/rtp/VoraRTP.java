package dev.vora.rtp;

import dev.vora.rtp.command.RTPCommand;
import dev.vora.rtp.config.Settings;
import dev.vora.rtp.gui.RTPGui;
import dev.vora.rtp.listener.CountdownListener;
import dev.vora.rtp.listener.GuiClickListener;
import dev.vora.rtp.teleport.CooldownManager;
import dev.vora.rtp.teleport.CountdownManager;
import dev.vora.rtp.teleport.LocationCache;
import dev.vora.rtp.teleport.TeleportHandler;
import dev.vora.rtp.teleport.WorldResolver;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoraRTP extends JavaPlugin {

    private Settings settings;
    private RTPGui gui;
    private WorldResolver worldResolver;
    private TeleportHandler teleportHandler;
    private CountdownManager countdownManager;
    private CooldownManager cooldownManager;
    private LocationCache locationCache;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadAll();
    }

    @Override
    public void onDisable() {
        shutdownAll();
    }

    public void loadAll() {
        shutdownAll();

        reloadConfig();
        this.settings = Settings.load(this);
        this.worldResolver = new WorldResolver(settings);
        this.gui = new RTPGui(settings);
        this.cooldownManager = new CooldownManager(settings);
        this.teleportHandler = new TeleportHandler(this, settings, worldResolver);
        this.countdownManager = new CountdownManager(this, settings, teleportHandler);
        this.locationCache = new LocationCache(this, settings, worldResolver);

        registerCommand();
        registerListeners();

        if (settings.teleport().cache().enabled()) {
            locationCache.start();
        }
    }

    private void shutdownAll() {
        if (countdownManager != null) countdownManager.cancelAll();
        if (locationCache != null) locationCache.stop();
    }

    private void registerCommand() {
        var command = getCommand("rtp");
        if (command == null) {
            throw new IllegalStateException("rtp command is missing in plugin.yml");
        }
        RTPCommand executor = new RTPCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiClickListener(this), this);
        pm.registerEvents(new CountdownListener(settings, countdownManager), this);
    }

    public Settings settings() { return settings; }
    public RTPGui gui() { return gui; }
    public TeleportHandler teleportHandler() { return teleportHandler; }
    public CountdownManager countdownManager() { return countdownManager; }
    public CooldownManager cooldownManager() { return cooldownManager; }
    public LocationCache locationCache() { return locationCache; }
}