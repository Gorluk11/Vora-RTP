package dev.vora.rtp.command;

import dev.vora.rtp.VoraRTP;
import dev.vora.rtp.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class RTPCommand implements CommandExecutor, TabCompleter {

    private final VoraRTP plugin;

    public RTPCommand(VoraRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // /rtp reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vora.rtp.reload")) {
                if (sender instanceof Player p) {
                    MessageUtil.send(p, plugin.settings(), plugin.settings().messages().noPermission());
                }
                return true;
            }

            plugin.loadAll();

            if (sender instanceof Player p) {
                MessageUtil.send(p, plugin.settings(), plugin.settings().messages().reloaded());
            } else {
                sender.sendMessage(MessageUtil.legacy(plugin.settings().prefix() + plugin.settings().messages().reloaded()));
            }
            return true;
        }

        // /rtp
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.legacy(plugin.settings().prefix() + plugin.settings().messages().playerOnly()));
            return true;
        }

        if (!player.hasPermission("vora.rtp.use")) {
            MessageUtil.send(player, plugin.settings(), plugin.settings().messages().noPermission());
            return true;
        }

        // cooldown check
        long remaining = plugin.cooldownManager().remaining(player);
        if (remaining > 0) {
            String seconds = String.valueOf((int) Math.ceil(remaining / 1000.0));
            String msg = plugin.settings().messages().cooldown().replace("{seconds}", seconds);
            MessageUtil.send(player, plugin.settings(), msg);
            return true;
        }

        plugin.gui().open(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("vora.rtp.reload")) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }
}