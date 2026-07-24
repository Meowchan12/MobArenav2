package vn.meowchan12.mobarenav2.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SupplyManager {

    private final MobArenav2 plugin;
    private File file;
    private YamlConfiguration config;

    public SupplyManager(MobArenav2 plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        file = new File(plugin.getDataFolder(), "supply.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create supply.yml!");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save supply.yml!");
        }
    }

    public boolean createSupply(String id) {
        if (config.contains("supplies." + id)) return false;
        config.set("supplies." + id + ".items", new ArrayList<ItemStack>());
        saveConfig();
        return true;
    }

    public boolean deleteSupply(String id) {
        if (!config.contains("supplies." + id)) return false;
        config.set("supplies." + id, null);
        saveConfig();
        return true;
    }

    public boolean resetSupply(String id) {
        if (!config.contains("supplies." + id)) return false;
        config.set("supplies." + id + ".items", new ArrayList<ItemStack>());
        saveConfig();
        return true;
    }

    public void openEditGUI(Player player, String id) {
        if (!config.contains("supplies." + id)) {
            player.sendMessage("§c[MobArenav2] Supply ID '" + id + "' does not exist!");
            return;
        }

        // Tạo GUI 54 ô để Admin nhét đồ
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Editing Supply: " + id));

        List<?> list = config.getList("supplies." + id + ".items");
        if (list != null) {
            ItemStack[] items = list.toArray(new ItemStack[0]);
            for (int i = 0; i < items.length && i < 54; i++) {
                if (items[i] != null) gui.setItem(i, items[i]);
            }
        }

        player.openInventory(gui);
    }

    public void saveSupplyFromGUI(String id, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) items.add(item);
        }
        config.set("supplies." + id + ".items", items);
        saveConfig();
    }

    public List<ItemStack> getSupplyItems(String id) {
        if (!config.contains("supplies." + id)) return new ArrayList<>();
        List<?> list = config.getList("supplies." + id + ".items");
        if (list != null) {
            List<ItemStack> items = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof ItemStack is) items.add(is);
            }
            return items;
        }
        return new ArrayList<>();
    }
}