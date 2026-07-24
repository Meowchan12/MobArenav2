package vn.meowchan12.mobarenav2.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class UserDataManager {

    private final MobArenav2 plugin;
    private final File userFolder;

    public UserDataManager(MobArenav2 plugin) {
        this.plugin = plugin;
        this.userFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userFolder.exists()) {
            userFolder.mkdirs();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(userFolder, uuid.toString() + ".yml");
    }

    private YamlConfiguration getConfig(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveConfig(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[MobArenav2] Could not save user data to " + file.getName());
        }
    }

    // ==========================================
    // [MỚI] ARENA LOCK SYSTEM (CAMPAIGN PROGRESS)
    // ==========================================
    public void unlockArena(UUID uuid, String arenaName) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = getConfig(file);
        List<String> unlocked = config.getStringList("unlocked_arenas");

        if (!unlocked.contains(arenaName.toLowerCase())) {
            unlocked.add(arenaName.toLowerCase());
            config.set("unlocked_arenas", unlocked);
            saveConfig(config, file);
        }
    }

    public boolean hasUnlockedArena(UUID uuid, String arenaName) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return false;

        YamlConfiguration config = getConfig(file);
        List<String> unlocked = config.getStringList("unlocked_arenas");
        return unlocked.contains(arenaName.toLowerCase());
    }

    // ==========================================
    // LIFETIME STATS SYSTEM (BẢNG XẾP HẠNG)
    // ==========================================
    public void addStats(UUID uuid, int kills, boolean isWin, int maxWave) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = getConfig(file);

        int currentKills = config.getInt("stats.lifetime_kills", 0);
        int gamesPlayed = config.getInt("stats.games_played", 0);
        int gamesWon = config.getInt("stats.games_won", 0);
        int highestWave = config.getInt("stats.highest_wave", 0);

        config.set("stats.lifetime_kills", currentKills + kills);
        config.set("stats.games_played", gamesPlayed + 1);
        if (isWin) config.set("stats.games_won", gamesWon + 1);
        if (maxWave > highestWave) config.set("stats.highest_wave", maxWave);

        saveConfig(config, file);
    }

    public int getStat(UUID uuid, String statType) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return 0;
        YamlConfiguration config = getConfig(file);
        return config.getInt("stats." + statType, 0);
    }

    // Hàm rỗng (Dummy) để tương thích ngược
    public void removeData(Player player) {
        // Safe to ignore. Backup deletion is now handled by InventoryBackupManager.
    }
}