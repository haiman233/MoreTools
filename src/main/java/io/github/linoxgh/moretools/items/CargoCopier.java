package io.github.linoxgh.moretools.items;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.github.linoxgh.moretools.Messages;
import io.github.linoxgh.moretools.MoreTools;
import io.github.linoxgh.moretools.handlers.ItemInteractHandler;

import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.AdvancedCargoOutputNode;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoInputNode;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoOutputNode;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.cscorelib2.protection.ProtectableAction;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link CargoCopier} is a {@link SlimefunItem} which allows you to copy the settings of a cargo node
 * with a single left click and save these settings to a cargo node with a right click.
 *
 * @author Linox
 *
 * @see ItemInteractHandler
 *
 */
public class CargoCopier extends SimpleSlimefunItem<ItemInteractHandler> implements DamageableItem {

    private final boolean damageable;
    private final int cooldown;

    private final HashMap<UUID, Long> lastUses = new HashMap<>();
    private final NamespacedKey copy = new NamespacedKey(MoreTools.getInstance(), "cargo-settings");

    public CargoCopier(@NotNull Category category, @NotNull SlimefunItemStack item, @NotNull RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        FileConfiguration cfg = MoreTools.getInstance().getConfig();
        damageable = cfg.getBoolean("item-settings.cargo-copier.damageable");
        cooldown = cfg.getInt("item-settings.cargo-copier.cooldown");
    }

    @Override
    public @NotNull ItemInteractHandler getItemHandler() {
        return (e, sfItem) -> {
            ItemStack item = e.getItem();
            if (!sfItem.getID().equals(getID()) || item == null) {
                return;
            }
            e.setCancelled(true);

            Block b = e.getClickedBlock();
            if (b != null) {
                Player p = e.getPlayer();
                if (SlimefunPlugin.getProtectionManager().hasPermission(p, b.getLocation(), ProtectableAction.BREAK_BLOCK)) {

                    Long lastUse = lastUses.get(p.getUniqueId());
                    if (lastUse != null) {
                        if ((System.currentTimeMillis() - lastUse) < cooldown) {
                            p.sendMessage(
                                    Messages.CARGOCOPIER_COOLDOWN.getMessage().replaceAll(
                                            "\\{left-cooldown}",
                                            String.valueOf(cooldown - (System.currentTimeMillis() - lastUse)))
                            );
                            return;
                        }
                    }
                    lastUses.put(p.getUniqueId(), System.currentTimeMillis());

                    switch (e.getAction()) {
                        case RIGHT_CLICK_BLOCK:
                            saveCargoNode(b, p, item);
                            break;

                        case LEFT_CLICK_BLOCK:
                            copyCargoNode(b, p, item);
                            break;

                        default:
                            break;
                    }
                }
            }
        };
    }

    private void copyCargoNode(@NotNull Block b, @NotNull Player p, @NotNull ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            p.sendMessage(Messages.CARGOCOPIER_COPYFAIL.getMessage());
            return;
        }

        SlimefunItem node = BlockStorage.check(b);
        if (node == null) {
            p.sendMessage(Messages.CARGOCOPIER_WRONGBLOCK.getMessage());
            return;
        }

        StringBuilder builder = new StringBuilder("{");
        if (node.getID().equals("CARGO_NODE_INPUT")) {

            builder.append("round-robin:").append(BlockStorage.getLocationInfo(b.getLocation(), "round-robin")).append("}/{");
            builder.append("frequency:").append(BlockStorage.getLocationInfo(b.getLocation(), "frequency")).append("}/{");
            builder.append("filter-type:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-type")).append("}/{");
            builder.append("filter-lore:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-lore")).append("}/{");
            builder.append("filter-durability:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-durability")).append("}/{");
            builder.append("index:").append(BlockStorage.getLocationInfo(b.getLocation(), "index")).append("}");

        } else if (node.getID().equals("CARGO_NODE_OUTPUT_ADVANCED")) {

            builder.append("frequency:").append(BlockStorage.getLocationInfo(b.getLocation(), "frequency")).append("}/{");
            builder.append("filter-type:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-type")).append("}/{");
            builder.append("filter-lore:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-lore")).append("}/{");
            builder.append("filter-durability:").append(BlockStorage.getLocationInfo(b.getLocation(), "filter-durability")).append("}/{");
            builder.append("index:").append(BlockStorage.getLocationInfo(b.getLocation(), "index")).append("}");

        } else if (node.getID().equals("CARGO_NODE_OUTPUT")) {

            builder.append("frequency:").append(BlockStorage.getLocationInfo(b.getLocation(), "frequency")).append("}");

        } else {
            p.sendMessage(Messages.CARGOCOPIER_WRONGBLOCK.getMessage());
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(copy, PersistentDataType.STRING, builder.toString());

        item.setItemMeta(meta);
        p.sendMessage(Messages.CARGOCOPIER_COPYSUCCESS.getMessage());
    }

    private void saveCargoNode(@NotNull Block b, @NotNull Player p, @NotNull ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            p.sendMessage(Messages.CARGOCOPIER_SAVEFAIL.getMessage());
            return;
        }

        SlimefunItem node = BlockStorage.check(b);
        if (node == null) {
            p.sendMessage(Messages.CARGOCOPIER_WRONGBLOCK.getMessage());
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String settings = pdc.get(copy, PersistentDataType.STRING);
        if (settings == null) {
            p.sendMessage(Messages.CARGOCOPIER_SAVEFAIL.getMessage());
            return;
        }
        String[] settingsArr = settings.split("/");

        HashMap<String, String> map = new HashMap<>();
        for (String setting : settingsArr) {
            String[] subSetting = setting.substring(1, setting.length() - 1).split(":");
            for (String s : subSetting) {
                System.out.println(s);
            }
            map.put(subSetting[0], subSetting[1]);
        }

        if (node.getID().equals("CARGO_NODE_INPUT")) {

            for (Map.Entry<String, String> entry : map.entrySet()) {
                BlockStorage.addBlockInfo(b, entry.getKey(), entry.getValue());
            }

            try {
                CargoInputNode inputNode = (CargoInputNode) node;
                Method method = inputNode.getClass().getDeclaredMethod("updateBlockMenu", BlockMenu.class, Block.class);
                method.setAccessible(true);
                method.invoke(inputNode, BlockStorage.getInventory(b), b);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Could not find 'updateBlockMenu' method.");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Please report this to:");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, MoreTools.getInstance().getBugTrackerURL());
                e.printStackTrace();
            }

        } else if (node.getID().equals("CARGO_NODE_OUTPUT")) {

            for (Map.Entry<String, String> entry : map.entrySet()) {
                BlockStorage.addBlockInfo(b, entry.getKey(), entry.getValue());
            }

            try {
                CargoOutputNode inputNode = (CargoOutputNode) node;
                Method method = inputNode.getClass().getDeclaredMethod("updateBlockMenu", BlockMenu.class, Block.class);
                method.setAccessible(true);
                method.invoke(inputNode, BlockStorage.getInventory(b), b);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Could not find 'updateBlockMenu' method.");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Please report this to:");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, MoreTools.getInstance().getBugTrackerURL());
                e.printStackTrace();
            }

        } else if (node.getID().equals("CARGO_NODE_OUTPUT_ADVANCED")) {

            for (Map.Entry<String, String> entry : map.entrySet()) {
                BlockStorage.addBlockInfo(b, entry.getKey(), entry.getValue());
            }

            try {
                AdvancedCargoOutputNode inputNode = (AdvancedCargoOutputNode) node;
                Method method = inputNode.getClass().getDeclaredMethod("updateBlockMenu", BlockMenu.class, Block.class);
                method.setAccessible(true);
                method.invoke(inputNode, BlockStorage.getInventory(b), b);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Could not find 'updateBlockMenu' method.");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, "Please report this to:");
                MoreTools.getInstance().getLogger().log(Level.SEVERE, MoreTools.getInstance().getBugTrackerURL());
                e.printStackTrace();
            }

        } else {
            p.sendMessage(Messages.CARGOCOPIER_WRONGBLOCK.getMessage());
            return;
        }

        p.sendMessage(Messages.CARGOCOPIER_SAVESUCCESS.getMessage());
    }

    private @NotNull BlockBreakHandler getBlockBreakHandler() {
        return new BlockBreakHandler() {

            @Override
            public boolean onBlockBreak(BlockBreakEvent e, ItemStack item, int fortune, List<ItemStack> drops) {
                if (isItem(item)) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(Messages.CARGOCOPIER_BLOCKBREAKING.getMessage());
                    return true;
                }
                return false;
            }

            @Override
            public boolean isPrivate() {
                return false;
            }
        };
    }

    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler(getBlockBreakHandler());
    }

    @Override
    public boolean isDamageable() {
        return damageable;
    }

}
