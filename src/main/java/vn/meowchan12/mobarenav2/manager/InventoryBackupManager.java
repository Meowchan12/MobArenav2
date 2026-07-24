package vn.meowchan12.mobarenav2.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InventoryBackupManager {

    private final MobArenav2 plugin;
    private final File backupFolder;
    private final Set<UUID> pendingJoins = new HashSet<>();

    public InventoryBackupManager(MobArenav2 plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    public boolean isPendingJoin(UUID uuid) {
        return pendingJoins.contains(uuid);
    }

    public void addPending(UUID uuid) {
        pendingJoins.add(uuid);
    }

    public void removePending(UUID uuid) {
        pendingJoins.remove(uuid);
    }

    public boolean hasBackup(UUID uuid) {
        return new File(backupFolder, uuid.toString() + ".yml").exists();
    }

    // --- ASYNC BACKUP SYSTEM ---
    public void backupAndClear(Player player, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        File file = new File(backupFolder, uuid.toString() + ".yml");

        // 1. Clone data safely on the Main Thread
        ItemStack[] invContents = player.getInventory().getContents().clone();
        ItemStack[] armorContents = player.getInventory().getArmorContents().clone();
        float xp = player.getExp();
        int level = player.getLevel();
        double health = player.getHealth();

        // 2. Clear player's inventory immediately
        player.getInventory().clear();
        player.updateInventory();

        // 3. Save data to YAML on an Async Thread to prevent server lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("inventory", invContents);
            config.set("armor", armorContents);
            config.set("xp", xp);
            config.set("level", level);
            config.set("health", health);

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("[MobArenav2] CRITICAL ERROR: Could not save backup for " + player.getName());
                e.printStackTrace();
            }

            // 4. Return to Main Thread to execute the callback (Teleporting to arena)
            Bukkit.getScheduler().runTask(plugin, onComplete);
        });
    }

    // --- RESTORE SYSTEM ---
    @SuppressWarnings("unchecked")
    public void restoreBackup(Player player) {
        File file = new File(backupFolder, player.getUniqueId().toString() + ".yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Wash away Arena garbage before restoring
        player.getInventory().clear();

        if (config.contains("inventory")) {
            Object obj = config.get("inventory");
            if (obj instanceof List) {
                List<ItemStack> list = (List<ItemStack>) obj;
                player.getInventory().setContents(list.toArray(new ItemStack[0]));
            }
        }

        if (config.contains("armor")) {
            Object obj = config.get("armor");
            if (obj instanceof List) {
                List<ItemStack> list = (List<ItemStack>) obj;
                player.getInventory().setArmorContents(list.toArray(new ItemStack[0]));
            }
        }

        if (config.contains("xp")) player.setExp((float) config.getDouble("xp"));
        if (config.contains("level")) player.setLevel(config.getInt("level"));
        if (config.contains("health")) player.setHealth(Math.min(config.getDouble("health"), 20.0));

        player.updateInventory();

        // Delete backup file to prevent item duplication
        file.delete();
    }

    // --- ADMIN GUI VIEWER ---
    @SuppressWarnings("unchecked")
    public void openBackupGUI(Player admin, UUID targetUUID, String targetName) {
        File file = new File(backupFolder, targetUUID.toString() + ".yml");
        if (!file.exists()) {
            admin.sendMessage("§c[MobArenav2] No backup found for " + targetName + ".");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Inventory gui = Bukkit.createInventory(null, 54, "§8Backup: " + targetName);

        if (config.contains("inventory")) {
            Object obj = config.get("inventory");
            if (obj instanceof List) {
                List<ItemStack> list = (List<ItemStack>) obj;
                ItemStack[] contents = list.toArray(new ItemStack[0]);
                for (int i = 0; i < contents.length && i < 54; i++) {
                    if (contents[i] != null) gui.setItem(i, contents[i]);
                }
            }
        }
        admin.openInventory(gui);
    }
}