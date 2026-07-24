package vn.meowchan12.mobarenav2.arena;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.manager.ClassManager;

import java.util.ArrayList;
import java.util.List;

public class ClassGUI {

    public static void open(Player player, MobArenav2 plugin) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("§8Select your Class"));
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        ClassManager classManager = plugin.getClassManager();

        int slot = 10;

        for (String className : classManager.getAvailableClasses()) {
            if (slot > 16) break;

            ItemStack icon = classManager.getClassIcon(className).clone();
            ItemMeta meta = icon.getItemMeta();

            // 1. Kiểm tra xem class này có được phép chơi ở map này không
            boolean isRestricted = !classManager.isClassAllowedInArena(className, arena);

            // 2. Kiểm tra slot người chơi (Chỉ xét Full nếu class không bị cấm)
            int limit = classManager.getClassLimit(className);
            int current = classManager.getPlayersInClass(arena, className);
            boolean isFull = (!isRestricted && limit != -1 && current >= limit);

            // Ghi đè icon thành Barrier nếu bị cấm HOẶC đã đầy
            if (isRestricted || isFull) {
                icon.setType(Material.BARRIER);
            }

            if (meta != null) {
                if (isRestricted) {
                    meta.displayName(Component.text("§c§m" + className + "§r §4[RESTRICTED]"));
                } else if (isFull) {
                    meta.displayName(Component.text("§c§m" + className + "§r §4[FULL]"));
                } else {
                    meta.displayName(Component.text("§6§l" + className));
                }

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7--------------------"));

                if (isRestricted) {
                    String arenaName = (arena != null) ? arena.getName() : "this map";
                    lore.add(Component.text("§cUnavailable in " + arenaName));
                } else if (limit != -1) {
                    lore.add(Component.text("§fSlots: " + (isFull ? "§c" : "§a") + current + "/" + limit));
                } else {
                    lore.add(Component.text("§fSlots: §aUnlimited"));
                }

                lore.add(Component.text("§7--------------------"));

                if (isRestricted) {
                    lore.add(Component.text("§cMap restrictions apply!"));
                } else if (!isFull) {
                    lore.add(Component.text("§eClick to equip this class."));
                } else {
                    lore.add(Component.text("§cClass limit reached!"));
                }

                meta.lore(lore);

                NamespacedKey key = new NamespacedKey(plugin, "class_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, className);

                icon.setItemMeta(meta);
            }

            gui.setItem(slot, icon);
            slot++;
        }

        player.openInventory(gui);
    }
}