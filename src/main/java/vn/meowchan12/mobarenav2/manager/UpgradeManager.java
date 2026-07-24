package vn.meowchan12.mobarenav2.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeManager {

    private final MobArenav2 plugin;
    private FileConfiguration config;

    private int triggerEvery = 5;
    private int defaultMaxSlots = 3;
    private final Map<String, Integer> classSlots = new HashMap<>();

    private final List<UpgradeData> availableUpgrades = new ArrayList<>();

    public UpgradeManager(MobArenav2 plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        availableUpgrades.clear();
        classSlots.clear();

        if (config.getBoolean("settings.enabled", true)) {
            triggerEvery = config.getInt("settings.trigger-every", 5);
            defaultMaxSlots = config.getInt("settings.default-max-slots", 3);

            ConfigurationSection classSec = config.getConfigurationSection("settings.class-slots");
            if (classSec != null) {
                for (String className : classSec.getKeys(false)) {
                    classSlots.put(className.toLowerCase(), classSec.getInt(className));
                }
            }

            ConfigurationSection upgradesSec = config.getConfigurationSection("upgrades");
            if (upgradesSec != null) {
                for (String key : upgradesSec.getKeys(false)) {
                    ConfigurationSection sec = upgradesSec.getConfigurationSection(key);
                    if (sec == null) continue;

                    UpgradeData data = new UpgradeData();
                    data.id = key;
                    data.displayName = sec.getString("display-name", "&a" + key).replace("&", "§");
                    data.material = Material.matchMaterial(sec.getString("material", "BOOK"));
                    if (data.material == null) data.material = Material.BOOK;

                    List<String> rawLore = sec.getStringList("lore");
                    data.lore = new ArrayList<>();
                    for (String l : rawLore) data.lore.add(l.replace("&", "§"));

                    data.type = sec.getString("type", "HEAL").toUpperCase();

                    data.maxLevel = sec.getInt("max-level", 1);
                    data.baseValue = sec.getDouble("base-value", sec.getDouble("value", 0.0));
                    data.increment = sec.getDouble("increment", 0.0);
                    data.eventsSection = sec.getConfigurationSection("events");

                    data.effectType = sec.getString("effect", "SPEED");
                    data.amplifier = sec.getInt("amplifier", 0);
                    data.duration = sec.getInt("duration", 200);
                    data.command = sec.getString("command", "");
                    data.weight = sec.getInt("weight", 10);
                    data.slot = sec.getInt("slot", -1);

                    availableUpgrades.add(data);
                }
            }
        } else {
            triggerEvery = -1;
        }
    }

    public int getTriggerEvery() {
        return triggerEvery;
    }

    public List<UpgradeData> getAvailableUpgrades() {
        return availableUpgrades;
    }

    public UpgradeData getUpgradeData(String id) {
        for (UpgradeData data : availableUpgrades) {
            if (data.id.equals(id)) return data;
        }
        return null;
    }

    // ==========================================
    // RPG SLOTS LOGIC
    // ==========================================
    public int getMaxSlots(Player player, String className) {
        for (int i = 10; i >= 1; i--) {
            if (player.hasPermission("mobarenav2.upgradeslot." + i)) {
                return i;
            }
        }
        if (className != null && classSlots.containsKey(className.toLowerCase())) {
            return classSlots.get(className.toLowerCase());
        }
        return defaultMaxSlots;
    }

    public int getUsedSlots(ArenaPlayerProfile profile) {
        int count = 0;
        for (UpgradeData data : availableUpgrades) {
            if (profile.getUpgradeLevel(data.id) > 0) {
                count++;
            }
        }
        return count;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public static class UpgradeData {
        public String id;
        public String displayName;
        public Material material;
        public List<String> lore;
        public String type;

        public int maxLevel;
        public double baseValue;
        public double increment;
        public ConfigurationSection eventsSection;

        public String effectType;
        public int amplifier;
        public int duration;
        public String command;
        public int weight;

        public int slot;
    }
}