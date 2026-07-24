package vn.meowchan12.mobarenav2.manager;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;
import vn.meowchan12.mobarenav2.utils.ItemParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassManager {

    private final MobArenav2 plugin;
    private final Map<String, ItemStack> classDisplayIcons = new HashMap<>();

    public ClassManager(MobArenav2 plugin) {
        this.plugin = plugin;
        loadClasses();
    }

    public void loadClasses() {
        classDisplayIcons.clear();
        ConfigurationSection section = plugin.getConfigManager().getClasses().getConfigurationSection("classes");
        if (section == null) return;

        for (String className : section.getKeys(false)) {
            String itemsStr = section.getString(className + ".items", "stone_sword");
            String firstItemStr = itemsStr.split(",")[0].trim();

            ItemStack icon = ItemParser.parseItem(firstItemStr);
            if (icon == null) icon = new ItemStack(Material.STONE_SWORD);

            classDisplayIcons.put(className, icon);
        }
    }

    public Set<String> getAvailableClasses() {
        return classDisplayIcons.keySet();
    }

    public ItemStack getClassIcon(String className) {
        return classDisplayIcons.get(className);
    }

    // --- CLASS RESTRICTION LOGIC ---
    public boolean isClassAllowedInArena(String className, Arena arena) {
        if (arena == null) return true;
        ConfigurationSection section = plugin.getConfigManager().getClasses().getConfigurationSection("classes." + className);

        if (section != null && section.contains("allowed-arenas")) {
            List<String> allowedArenas = section.getStringList("allowed-arenas");
            // Nếu danh sách không rỗng, kiểm tra xem tên arena hiện tại có nằm trong list không
            if (allowedArenas != null && !allowedArenas.isEmpty()) {
                for (String allowed : allowedArenas) {
                    if (allowed.equalsIgnoreCase(arena.getName())) {
                        return true;
                    }
                }
                return false; // Bị cấm ở map này
            }
        }
        return true; // Không setup list -> Cho phép ở mọi map
    }

    // --- CLASS LIMIT LOGIC ---
    public int getClassLimit(String className) {
        ConfigurationSection section = plugin.getConfigManager().getClasses().getConfigurationSection("classes." + className);
        if (section != null && section.contains("limit")) {
            return section.getInt("limit", -1);
        }
        return -1;
    }

    public int getPlayersInClass(Arena arena, String className) {
        if (arena == null) return 0;
        int count = 0;
        for (ArenaPlayerProfile profile : arena.getPlayerProfiles().values()) {
            if (className.equalsIgnoreCase(profile.getSelectedClass())) {
                count++;
            }
        }
        return count;
    }

    public boolean equipClass(Player player, String className) {
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return false;

        // 1. Kiểm tra giới hạn MAP (Arena Restriction) trước tiên
        if (!isClassAllowedInArena(className, arena)) {
            player.sendMessage("§c[MobArenav2] The " + className + " class is restricted and cannot be used in this arena!");
            return false;
        }

        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());

        // 2. Kiểm tra giới hạn số lượng người chơi (Limit)
        int limit = getClassLimit(className);
        if (limit != -1) {
            int current = getPlayersInClass(arena, className);

            if (profile != null && className.equalsIgnoreCase(profile.getSelectedClass())) {
                player.sendMessage("§e[MobArenav2] You have already equipped the " + className + " class!");
                return true;
            }

            if (current >= limit) {
                player.sendMessage("§c[MobArenav2] The " + className + " class is currently FULL (" + current + "/" + limit + ")!");
                return false;
            }
        }

        ConfigurationSection section = plugin.getConfigManager().getClasses().getConfigurationSection("classes." + className);
        if (section == null) return false;

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        String itemsStr = section.getString("items");
        if (itemsStr != null) {
            String[] items = itemsStr.split(",");
            for (String itemStr : items) {
                ItemStack item = ItemParser.parseItem(itemStr.trim());
                if (item != null) player.getInventory().addItem(item);
            }
        }

        String armorStr = section.getString("armor");
        if (armorStr != null) {
            String[] armors = armorStr.split(",");
            for (String armStr : armors) {
                ItemStack armor = ItemParser.parseItem(armStr.trim());
                if (armor != null) {
                    String typeName = armor.getType().name();
                    if (typeName.endsWith("_HELMET") || typeName.equals("CARVED_PUMPKIN")) player.getInventory().setHelmet(armor);
                    else if (typeName.endsWith("_CHESTPLATE") || typeName.equals("ELYTRA")) player.getInventory().setChestplate(armor);
                    else if (typeName.endsWith("_LEGGINGS")) player.getInventory().setLeggings(armor);
                    else if (typeName.endsWith("_BOOTS")) player.getInventory().setBoots(armor);
                }
            }
        }

        String effectsStr = section.getString("effects");
        if (effectsStr != null) {
            String[] effects = effectsStr.split(",");
            for (String effStr : effects) {
                String[] effData = effStr.trim().split(":");
                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effData[0].toLowerCase()));
                if (type != null) {
                    int amplifier = effData.length > 1 ? Integer.parseInt(effData[1]) : 0;
                    player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false, true));
                }
            }
        }

        if (profile != null) {
            profile.setSelectedClass(className);
        }

        player.sendMessage("§a[MobArenav2] You have selected the §e" + className + " §aclass!");
        return true;
    }
}