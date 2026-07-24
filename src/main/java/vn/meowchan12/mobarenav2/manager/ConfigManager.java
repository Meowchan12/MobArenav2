package vn.meowchan12.mobarenav2.manager;

import com.google.common.base.Charsets;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {

    private final MobArenav2 plugin;

    private File arenasFolder;
    private FileConfiguration settingsConfig;
    private FileConfiguration classesConfig;
    private FileConfiguration upgradesConfig;

    private FileConfiguration bossesConfig;
    private FileConfiguration bossSkillConfig;
    private FileConfiguration supplyConfig;

    public ConfigManager(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
            try {
                plugin.saveResource("arenas/default.yml", false);
                plugin.getLogger().info("[MobArenav2] Successfully extracted default.yml template!");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MobArenav2] Could not find arenas/default.yml in the jar resources!");
            }
        }

        settingsConfig = loadAndUpdateConfig("settings.yml");
        classesConfig = loadAndUpdateConfig("classes.yml");
        upgradesConfig = loadAndUpdateConfig("upgrades.yml");

        bossesConfig = loadAndUpdateConfig("bosses.yml");
        bossSkillConfig = loadAndUpdateConfig("bossskill.yml");
        supplyConfig = loadAndUpdateConfig("supply.yml");

        plugin.getLogger().info("[MobArenav2] All configuration files loaded and auto-updated safely!");
    }

    private FileConfiguration loadAndUpdateConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream defConfigStream = plugin.getResource(fileName);

        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("[MobArenav2] Could not save auto-updated config to " + file.getName());
            }
        }

        return config;
    }

    // ========================================================
    // SAFE SAVE MECHANISM (FILE PROTECTION FIREWALL)
    // ========================================================
    public void saveArenaCoords(Arena arena) {
        File file = new File(arenasFolder, arena.getName() + ".yml");

        // [FIX] Lấy Config trực tiếp từ RAM của Arena để bảo toàn dữ liệu MVP (last-match)
        FileConfiguration config = arena.getConfig();
        if (config == null) {
            config = new YamlConfiguration();
        }

        // Bức tường lửa bảo vệ File khỏi lỗi cú pháp (Syntax Error)
        if (file.exists()) {
            try {
                YamlConfiguration testSyntax = new YamlConfiguration();
                testSyntax.load(file);
            } catch (Exception e) {
                plugin.getLogger().severe("[MobArenav2] ⛔ SAVE OPERATION REJECTED ⛔");
                plugin.getLogger().severe("[MobArenav2] YAML syntax error detected in file: " + file.getName());
                plugin.getLogger().severe("[MobArenav2] Save action blocked to prevent the file from being wiped!");
                return;
            }
        }

        // Cập nhật tọa độ vào Config trên RAM
        if (arena.getP1() != null) config.set("coords.p1", serializeLocation(arena.getP1()));
        if (arena.getP2() != null) config.set("coords.p2", serializeLocation(arena.getP2()));
        if (arena.getLobby() != null) config.set("coords.lobby", serializeLocation(arena.getLobby()));
        if (arena.getArenaSpawn() != null) config.set("coords.arena", serializeLocation(arena.getArenaSpawn()));
        if (arena.getSpectatorSpawn() != null) config.set("coords.spectator", serializeLocation(arena.getSpectatorSpawn()));

        config.set("coords.spawnpoints", null);
        int i = 1;
        for (Location loc : arena.getSpawnpoints()) {
            config.set("coords.spawnpoints." + i, serializeLocation(loc));
            i++;
        }

        // [MỚI] Lưu tọa độ Hòm thính (Supply Points)
        config.set("coords.supplypoints", null);
        int j = 1;
        for (Location loc : arena.getSupplypoints()) {
            config.set("coords.supplypoints." + j, serializeLocation(loc));
            j++;
        }

        try {
            config.save(file); // Ghi đè toàn bộ (MVP + Tọa độ + Setting) xuống ổ cứng
            arena.setConfig(config); // Gắn lại vào Arena để đồng bộ
            plugin.getLogger().info("[MobArenav2] Successfully saved FULL config & data for arena: " + arena.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("[MobArenav2] Could not save arena data for " + arena.getName());
            e.printStackTrace();
        }
    }

    public void createArenaTemplate(String arenaName) {
        File newFile = new File(arenasFolder, arenaName + ".yml");
        if (!newFile.exists()) {
            try {
                File defaultFile = new File(arenasFolder, "default.yml");
                if (defaultFile.exists()) {
                    com.google.common.io.Files.copy(defaultFile, newFile);
                } else {
                    plugin.saveResource("arenas/default.yml", true);
                    com.google.common.io.Files.copy(new File(arenasFolder, "default.yml"), newFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[MobArenav2] Could not create template for arena: " + arenaName);
            }
        }
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch() + "," + loc.getWorld().getName();
    }

    public File getArenasFolder() { return arenasFolder; }
    public FileConfiguration getSettings() { return settingsConfig; }
    public FileConfiguration getClasses() { return classesConfig; }
    public FileConfiguration getUpgrades() { return upgradesConfig; }
    public FileConfiguration getBosses() { return bossesConfig; }
    public FileConfiguration getBossSkill() { return bossSkillConfig; }
    public FileConfiguration getSupply() { return supplyConfig; }
}