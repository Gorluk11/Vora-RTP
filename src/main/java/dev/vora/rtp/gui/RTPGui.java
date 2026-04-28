package dev.vora.rtp.gui;

import dev.vora.rtp.config.Settings;
import dev.vora.rtp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RTPGui {

    private final Settings settings;

    public RTPGui(Settings settings) {
        this.settings = settings;
    }

    public void open(Player player) {
        RTPMenuHolder holder = new RTPMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, settings.menu().size(), MessageUtil.component(settings.menu().title()));
        holder.bind(inventory);

        fill(inventory);

        placeWorld(inventory, settings.menu().normal());
        placeWorld(inventory, settings.menu().nether());
        placeWorld(inventory, settings.menu().end());

        player.openInventory(inventory);
        MessageUtil.playSound(player, settings.sound("menu-open"));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = new ItemStack(settings.menu().filler());
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(MessageUtil.component(" "));
        filler.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private void placeWorld(Inventory inventory, Settings.MenuOption option) {
        if (option.slot() < 0 || option.slot() >= inventory.getSize()) {
            return;
        }

        ItemStack item;

        // If this world is disabled, show the disabled item instead
        if (!option.enabled()) {
            Settings.DisabledItem di = settings.menu().disabledItem();
            item = new ItemStack(di.material());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(MessageUtil.component(di.name()));
            meta.lore(MessageUtil.componentList(di.lore()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(option.material());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(MessageUtil.component(option.name()));
            meta.lore(MessageUtil.componentList(option.lore()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        inventory.setItem(option.slot(), item);
    }
}