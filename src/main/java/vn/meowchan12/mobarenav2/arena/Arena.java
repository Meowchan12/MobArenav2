package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.util.*;

public class Arena {

    private final String name;
    private FileConfiguration config;

    private Location p1, p2, lobby, arenaSpawn, spectatorSpawn;
    private final List<Location> spawnpoints = new ArrayList<>();
    private final List<Location> supplypoints = new ArrayList<>(); // [MỚI] Tọa độ hòm thính
    private final double spawnMinDistance = 5.0;

    private boolean isRunning = false;
    private int currentWave = 0;

    private boolean isVoting = false;
    private boolean isEndlessMode = false;
    private int voteTicks = 0;
    private final Queue<String> endlessWaveQueue = new LinkedList<>();
    private final Map<UUID, Boolean> activeVotes = new HashMap<>();

    private final Map<UUID, ArenaPlayerProfile> playerProfiles = new HashMap<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final List<LivingEntity> activeMobs = new ArrayList<>();

    private final List<Location> activeSupplies = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public FileConfiguration getConfig() { return config; }
    public void setConfig(FileConfiguration config) { this.config = config; }

    public Location getP1() { return p1; }
    public void setPoint1(Location p1) { this.p1 = p1; }
    public Location getP2() { return p2; }
    public void setPoint2(Location p2) { this.p2 = p2; }
    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; }
    public Location getArenaSpawn() { return arenaSpawn; }
    public void setArenaSpawn(Location arenaSpawn) { this.arenaSpawn = arenaSpawn; }
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }
    public List<Location> getSpawnpoints() { return spawnpoints; }
    public List<Location> getSupplypoints() { return supplypoints; }
    public double getSpawnMinDistance() { return spawnMinDistance; }

    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { this.isRunning = running; }
    public int getCurrentWave() { return currentWave; }
    public void setCurrentWave(int currentWave) { this.currentWave = currentWave; }

    public boolean isVoting() { return isVoting; }
    public void setVoting(boolean voting) { this.isVoting = voting; }
    public boolean isEndlessMode() { return isEndlessMode; }
    public void setEndlessMode(boolean endlessMode) { this.isEndlessMode = endlessMode; }
    public Queue<String> getEndlessWaveQueue() { return endlessWaveQueue; }
    public Map<UUID, Boolean> getActiveVotes() { return activeVotes; }
    public int getVoteTicks() { return voteTicks; }
    public void setVoteTicks(int voteTicks) { this.voteTicks = voteTicks; }

    public Map<UUID, ArenaPlayerProfile> getPlayerProfiles() { return playerProfiles; }
    public Set<UUID> getSpectators() { return spectators; }
    public List<LivingEntity> getActiveMobs() { return activeMobs; }
    public List<Location> getActiveSupplies() { return activeSupplies; }

    public Set<UUID> getActivePlayers() {
        Set<UUID> active = new HashSet<>(playerProfiles.keySet());
        active.removeAll(spectators);
        return active;
    }

    public List<Player> getPlayersAsObjects() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : getActivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }
        return players;
    }

    public boolean containsLocation(Location loc) {
        if (p1 == null || p2 == null || loc == null || loc.getWorld() != p1.getWorld()) return false;
        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX());
        double maxY = Math.max(p1.getY(), p2.getY());
        double maxZ = Math.max(p1.getZ(), p2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public void checkAndStart(MobArenav2 plugin) {
        if (isRunning) return;

        for (ArenaPlayerProfile profile : playerProfiles.values()) {
            if (!profile.isReady() && !spectators.contains(profile.getPlayerUUID())) return;
        }

        int minPlayers = config != null ? config.getInt("settings.player-limits.min-players", 1) : 1;
        if (getActivePlayers().size() < minPlayers) {
            broadcastMessage("§c[MobArenav2] Everyone is ready, but the arena requires at least " + minPlayers + " players to start!");
            for (ArenaPlayerProfile profile : playerProfiles.values()) {
                profile.setReady(false);
            }
            return;
        }

        clearLeftoverEntities(plugin);
        plugin.getWaveManager().startArena(this);
    }

    public void broadcastMessage(String message) {
        for (UUID uuid : playerProfiles.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public void clearLeftoverEntities(MobArenav2 plugin) {
        if (p1 == null || p2 == null) return;
        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX());
        double maxY = Math.max(p1.getY(), p2.getY());
        double maxZ = Math.max(p1.getZ(), p2.getZ());

        NamespacedKey bossKey = new NamespacedKey(plugin, "arena_boss_id");

        for (Entity entity : p1.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity) || entity instanceof Player) continue;

            Location loc = entity.getLocation();
            if (loc.getX() >= minX && loc.getX() <= maxX &&
                    loc.getY() >= minY && loc.getY() <= maxY &&
                    loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                if (entity instanceof org.bukkit.entity.Monster ||
                        entity instanceof org.bukkit.entity.Slime ||
                        entity.getPersistentDataContainer().has(bossKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    public ConfigurationSection getWaveConfig(int wave) {
        ConfigurationSection wavesSection = config.getConfigurationSection("waves");
        if (wavesSection == null) return null;

        List<ConfigurationSection> matchedWaves = new ArrayList<>();
        int highestPriority = Integer.MAX_VALUE;

        for (String key : wavesSection.getKeys(false)) {
            ConfigurationSection waveSec = wavesSection.getConfigurationSection(key);
            if (waveSec == null) continue;

            String waveCondition = waveSec.getString("waves");
            if (waveCondition == null) continue;

            if (matchesWaveCondition(waveCondition, wave)) {
                int priority = waveSec.getInt("priority", 5);

                if (priority < highestPriority) {
                    highestPriority = priority;
                    matchedWaves.clear();
                    matchedWaves.add(waveSec);
                } else if (priority == highestPriority) {
                    matchedWaves.add(waveSec);
                }
            }
        }

        if (matchedWaves.isEmpty()) return null;
        if (matchedWaves.size() == 1) return matchedWaves.get(0);
        return matchedWaves.get(new Random().nextInt(matchedWaves.size()));
    }

    private boolean matchesWaveCondition(String condition, int wave) {
        String[] parts = condition.split(",");
        for (String part : parts) {
            part = part.trim();
            try {
                if (part.contains("-")) {
                    String[] bounds = part.split("-");
                    int min = Integer.parseInt(bounds[0].trim());
                    int max = Integer.parseInt(bounds[1].trim());
                    if (wave >= min && wave <= max) return true;
                } else if (part.endsWith("+")) {
                    int min = Integer.parseInt(part.replace("+", "").trim());
                    if (wave >= min) return true;
                } else {
                    int exact = Integer.parseInt(part);
                    if (wave == exact) return true;
                }
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    public void endArena(MobArenav2 plugin) {
        if (this.config != null && !playerProfiles.isEmpty()) {
            config.set("last-match.wave", this.currentWave);
            config.set("last-match.is-endless", this.isEndlessMode);
            config.set("last-match.players", null);

            List<ArenaPlayerProfile> mvps = new ArrayList<>(playerProfiles.values());
            mvps.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));

            String[] topNames = {"---", "---", "---"};
            int[] topKills = {0, 0, 0};
            String[] topClasses = {"None", "None", "None"};

            for (int i = 0; i < mvps.size(); i++) {
                ArenaPlayerProfile prof = mvps.get(i);
                String pName = Bukkit.getOfflinePlayer(prof.getPlayerUUID()).getName();
                if (pName == null) pName = "Unknown";

                if (i < 3) {
                    topNames[i] = pName;
                    topKills[i] = prof.getKills();
                    topClasses[i] = prof.getSelectedClass() != null ? prof.getSelectedClass() : "None";
                }

                String path = "last-match.players." + prof.getPlayerUUID().toString();
                config.set(path + ".name", pName);
                config.set(path + ".class", prof.getSelectedClass() != null ? prof.getSelectedClass() : "None");
                config.set(path + ".kills", prof.getKills());

                plugin.getUserDataManager().addStats(prof.getPlayerUUID(), prof.getKills(), this.isEndlessMode, this.currentWave);
            }
            plugin.getConfigManager().saveArenaCoords(this);

            // ==========================================
            // [MỚI] ARENA LOCK SYSTEM (MỞ KHÓA MAP)
            // Nếu người chơi chiến thắng (sống sót tới cuối), ghi nhận mở khóa!
            // ==========================================
            boolean isVictory = !getActivePlayers().isEmpty();
            if (isVictory) {
                for (UUID uuid : playerProfiles.keySet()) {
                    plugin.getUserDataManager().unlockArena(uuid, this.name);
                }
            }

            boolean useAnnouncer = config.getBoolean("settings.announcer.announce-when-arena-ends", true);

            if (useAnnouncer) {
                String path = isVictory ? "settings.announcer.messages.victory" : "settings.announcer.messages.defeat";
                List<String> messages = config.getStringList(path);

                if (messages == null || messages.isEmpty()) {
                    messages = Arrays.asList(
                            "&e==========================================",
                            isVictory ? "&a&lVICTORY! &fArena &e%arena% &fhas been cleared!" : "&c&lMATCH OVER! &fArena &e%arena%",
                            "&7Reached Wave: &b%wave%&7/&b%max_wave%",
                            "",
                            "&e&lTOP MVPs:",
                            " &61. &f%top1_player% &7- &c%top1_kills% Kills &8(%top1_class%)",
                            " &62. &f%top2_player% &7- &c%top2_kills% Kills &8(%top2_class%)",
                            " &63. &f%top3_player% &7- &c%top3_kills% Kills &8(%top3_class%)",
                            "&e=========================================="
                    );
                }

                int maxWaves = config.getInt("settings.max-waves", 100);
                for (String line : messages) {
                    if (line.contains("%top2_player%") && mvps.size() < 2) continue;
                    if (line.contains("%top3_player%") && mvps.size() < 3) continue;

                    String parsedLine = line.replace("%arena%", this.name)
                            .replace("%wave%", String.valueOf(this.currentWave) + (this.isEndlessMode ? " (Endless)" : ""))
                            .replace("%max_wave%", String.valueOf(maxWaves))
                            .replace("%top1_player%", topNames[0]).replace("%top1_kills%", String.valueOf(topKills[0])).replace("%top1_class%", topClasses[0])
                            .replace("%top2_player%", topNames[1]).replace("%top2_kills%", String.valueOf(topKills[1])).replace("%top2_class%", topClasses[1])
                            .replace("%top3_player%", topNames[2]).replace("%top3_kills%", String.valueOf(topKills[2])).replace("%top3_class%", topClasses[2])
                            .replace("&", "§");

                    broadcastMessage(parsedLine);
                }
            }
        }

        this.isRunning = false;
        this.currentWave = 0;
        this.isEndlessMode = false;
        this.isVoting = false;
        this.endlessWaveQueue.clear();
        this.activeVotes.clear();

        for (LivingEntity mob : activeMobs) {
            if (mob != null && mob.isValid()) mob.remove();
        }
        activeMobs.clear();

        clearLeftoverEntities(plugin);

        for (Location loc : activeSupplies) {
            if (loc != null && loc.getBlock().getType() == Material.CHEST) {
                if (loc.getBlock().getState() instanceof Chest chest) {
                    chest.getInventory().clear();
                }
                loc.getBlock().setType(Material.AIR);
            }
        }
        activeSupplies.clear();

        for (ArenaPlayerProfile profile : playerProfiles.values()) {
            if (profile.getScoreboard() != null) {
                profile.getScoreboard().remove();
            }
        }

        List<UUID> toLeave = new ArrayList<>(playerProfiles.keySet());
        toLeave.addAll(spectators);

        for (UUID uuid : toLeave) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getArenaManager().leaveArena(p);
        }

        playerProfiles.clear();
        spectators.clear();

        new ArenaRestorer(plugin).restoreArena(this);
    }
}