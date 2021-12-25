package io.github.linoxgh.moretools;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;

public class Items {

    private static final FileConfiguration cfg = MoreTools.getInstance().getConfig();
    
    // TOOLS
    public static final SlimefunItemStack CRESCENT_HAMMER = new SlimefunItemStack("CRESCENT_HAMMER", Material.IRON_HOE, "&b新月锤", "&7&o其实这只是一个扳手.", "", "&e左键单击 &7> 快速拆卸机器.");
    
    static {
        ItemMeta meta = CRESCENT_HAMMER.getItemMeta();
        List<String> lore = meta.getLore();
        
        if (cfg.getBoolean("item-settings.crescent-hammer.features.enable-rotation")) {
            lore.add(3, ChatColor.YELLOW + "右键单击" + ChatColor.GRAY + " > 旋转可旋转的方块.");
        }
        
        if (cfg.getBoolean("item-settings.crescent-hammer.features.enable-channel-change")) {
            lore.add(4, ChatColor.YELLOW + "蹲下 + 左键" + ChatColor.GRAY + " > 增加货物节点的信道.");
            lore.add(5, ChatColor.YELLOW + "蹲下 + 右键" + ChatColor.GRAY + " > 减少货物节点的信道.");
        }
        
        meta.setLore(lore);
        CRESCENT_HAMMER.setItemMeta(meta);
    }
}