package vn.meowchan12.mobarenav2.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;
import vn.meowchan12.mobarenav2.arena.ArenaScoreboard;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ArenaManager {

    private final MobArenav2 plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    public void addArena(Arena arena) { arenas.put(arena.getName().toLowerCase(), arena); }
    public Arena getArena(String name) { return arenas.get(name.toLowerCase()); }
    public Collection<Arena> getAllArenas() { return arenas.values(); }

    public Arena getArenaByPlayer(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.getPlayerProfiles().containsKey(player.getUniqueId())) return arena;
        }
        return null;
    }

    public boolean isPlaying(Player player) { return getArenaByPlayer(player) != null; }

    public boolean isSpectating(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.getSpectators().contains(player.getUniqueId())) return true;
        }
        return false;
    }

    // --- SAFE LOAD ALGORITHM ---
    public void loadArenas() {
        File folder = plugin.getConfigManager().getArenasFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            YamlConfiguration config = new YamlConfiguration();

            try {
                config.load(file);
            } catch (Exception e) {
                plugin.getLogger().severe("[MobArenav2] ❌ YAML SYNTAX ERROR IN FILE: " + file.getName());
                plugin.getLogger().severe("[MobArenav2] Details: " + e.getMessage());
                continue;
            }

            Arena arena = new Arena(name);
            arena.setConfig(config);

            if (config.contains("coords.p1")) arena.setPoint1(parseLocation(config.getString("coords.p1")));
            if (config.contains("coords.p2")) arena.setPoint2(parseLocation(config.getString("coords.p2")));
            if (config.contains("coords.lobby")) arena.setLobby(parseLocation(config.getString("coords.lobby")));
            if (config.contains("coords.arena")) arena.setArenaSpawn(parseLocation(config.getString("coords.arena")));
            if (config.contains("coords.spectator")) arena.setSpectatorSpawn(parseLocation(config.getString("coords.spectator")));

            if (config.contains("coords.spawnpoints")) {
                for (String key : config.getConfigurationSection("coords.spawnpoints").getKeys(false)) {
                    Location sp = parseLocation(config.getString("coords.spawnpoints." + key));
                    if (sp != null) arena.getSpawnpoints().add(sp);
                }
            }
            addArena(arena);
            plugin.getLogger().info("Loaded arena: " + name);
        }
    }

    // --- HOT-RELOAD (SAFE MODE) ---
    public void reloadArenas() {
        File folder = plugin.getConfigManager().getArenasFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            YamlConfiguration config = new YamlConfiguration();

            try {
                config.load(file);
            } catch (Exception e) {
                plugin.getLogger().severe("[MobArenav2] ❌ YAML SYNTAX ERROR IN FILE: " + file.getName());
                plugin.getLogger().severe("[MobArenav2] Reload operation ABORTED for this file to protect data!");
                continue;
            }

            Arena arena = getArena(name);
            if (arena == null) {
                arena = new Arena(name);
                addArena(arena);
            }

            arena.setConfig(config);

            if (config.contains("coords.p1")) arena.setPoint1(parseLocation(config.getString("coords.p1")));
            if (config.contains("coords.p2")) arena.setPoint2(parseLocation(config.getString("coords.p2")));
            if (config.contains("coords.lobby")) arena.setLobby(parseLocation(config.getString("coords.lobby")));
            if (config.contains("coords.arena")) arena.setArenaSpawn(parseLocation(config.getString("coords.arena")));
            if (config.contains("coords.spectator")) arena.setSpectatorSpawn(parseLocation(config.getString("coords.spectator")));

            if (config.contains("coords.spawnpoints")) {
                arena.getSpawnpoints().clear();
                for (String key : config.getConfigurationSection("coords.spawnpoints").getKeys(false)) {
                    Location sp = parseLocation(config.getString("coords.spawnpoints." + key));
                    if (sp != null) arena.getSpawnpoints().add(sp);
                }
            }
        }
        plugin.getLogger().info("[MobArenav2] Hot-reloaded all arena configurations safely.");
    }

    // --- SAFE LOCATION PARSER ---
    private Location parseLocation(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            String[] parts = str.split(",");
            if (parts.length < 6) return null;
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = Float.parseFloat(parts[4]);
            World world = Bukkit.getWorld(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("[MobArenav2] ⚠ Invalid location format: " + str);
            return null;
        }
    }

    public boolean joinArena(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();

        if (isPlaying(player)) {
            player.sendMessage("§c[MobArenav2] You are already in an arena!");
            return false;
        }

        if (plugin.getInventoryBackupManager().isPendingJoin(uuid)) {
            player.sendMessage("§c[MobArenav2] Please wait, your previous join request is still processing!");
            return false;
        }

        if (plugin.getInventoryBackupManager().hasBackup(uuid)) {
            player.sendMessage("§c[MobArenav2] Detected a hanging backup file! Restoring your items first...");
            plugin.getInventoryBackupManager().restoreBackup(player);
            return false;
        }

        Arena arena = getArena(arenaName);
        if (arena == null || arena.getLobby() == null) {
            player.sendMessage("§c[MobArenav2] Arena not found or Lobby is missing.");
            return false;
        }

        FileConfiguration config = arena.getConfig();
        boolean isEnabled = config != null && config.getBoolean("settings.enabled", true);
        if (!isEnabled) {
            player.sendMessage("§c[MobArenav2] This arena is currently disabled for maintenance!");
            return false;
        }

        // ==========================================
        // [MỚI] ARENA LOCK SYSTEM (KIỂM TRA TIẾN ĐỘ)
        // ==========================================
        if (config != null) {
            String requiredArena = config.getString("settings.required-arena", "");
            if (requiredArena != null && !requiredArena.isEmpty()) {
                if (!player.hasPermission("mobarenav2.bypass.arenalock")) {
                    if (!plugin.getUserDataManager().hasUnlockedArena(uuid, requiredArena)) {
                        player.sendMessage("§c[MobArenav2] This arena is locked! You must clear §e" + requiredArena + " §cfirst to enter.");
                        return false;
                    }
                }
            }
        }

        int maxPlayers = config != null ? config.getInt("settings.player-limits.max-players", 99) : 99;
        if (arena.getActivePlayers().size() >= maxPlayers) {
            player.sendMessage("§c[MobArenav2] This arena is currently full! (Max: " + maxPlayers + " players)");
            return false;
        }

        if (config != null && config.getBoolean("settings.entry-fee.enabled", false)) {
            String feeType = config.getString("settings.entry-fee.type", "VAULT").toUpperCase();
            if (feeType.equals("VAULT") && plugin.getEconomy() != null) {
                double amount = config.getDouble("settings.entry-fee.amount", 0.0);
                if (!plugin.getEconomy().withdrawPlayer(player, amount).transactionSuccess()) {
                    player.sendMessage("§c[MobArenav2] You do not have enough money! Cost: $" + amount);
                    return false;
                }
                player.sendMessage("§a[MobArenav2] Paid $" + amount + " entry fee.");
            } else if (feeType.equals("COMMAND")) {
                String cmd = config.getString("settings.entry-fee.command", "");
                if (!cmd.isEmpty()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        }

        plugin.getInventoryBackupManager().addPending(uuid);
        player.sendMessage("§e[MobArenav2] Scanning and backing up your inventory... Please stand still for 3 seconds!");

        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.getInventoryBackupManager().removePending(uuid);
                    this.cancel();
                    return;
                }

                if (countdown > 0) {
                    player.sendTitle("§a" + countdown, "§fPreparing for battle...", 0, 25, 0);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    countdown--;
                } else {
                    this.cancel();

                    plugin.getInventoryBackupManager().backupAndClear(player, () -> {
                        plugin.getInventoryBackupManager().removePending(uuid);

                        ArenaPlayerProfile profile = new ArenaPlayerProfile(uuid);
                        profile.saveOriginalLocation(player);

                        profile.setScoreboard(new ArenaScoreboard(player, plugin));
                        profile.getScoreboard().update(arena);
                        arena.getPlayerProfiles().put(uuid, profile);

                        Location lobby = arena.getLobby();
                        if (lobby != null && lobby.getWorld() != null) {
                            lobby.getWorld().getChunkAt(lobby).load(true);
                            player.teleport(lobby);
                        }

                        resetPlayerState(player);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                        player.sendMessage("§a[MobArenav2] You have joined arena: §e" + arena.getName());
                        player.sendMessage("§b➡ Type §e/ma class §bto select your loadout.");
                    });
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public boolean joinSpectator(Player player, String arenaName) {
        if (isPlaying(player)) {
            player.sendMessage("§c[MobArenav2] You are already in an arena!");
            return false;
        }

        Arena arena = getArena(arenaName);
        if (arena == null || arena.getSpectatorSpawn() == null) {
            player.sendMessage("§c[MobArenav2] Arena not found or spectator spawn is missing.");
            return false;
        }

        plugin.getInventoryBackupManager().backupAndClear(player, () -> {
            ArenaPlayerProfile profile = new ArenaPlayerProfile(player.getUniqueId());
            profile.saveOriginalLocation(player);

            profile.setScoreboard(new ArenaScoreboard(player, plugin));
            profile.getScoreboard().update(arena);

            arena.getPlayerProfiles().put(player.getUniqueId(), profile);
            arena.getSpectators().add(player.getUniqueId());

            Location specSpawn = arena.getSpectatorSpawn();
            if (specSpawn != null && specSpawn.getWorld() != null) {
                specSpawn.getWorld().getChunkAt(specSpawn).load(true);
                player.teleport(specSpawn);
            }

            resetPlayerState(player);

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvulnerable(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

            player.sendMessage("§7[MobArenav2] You are now spectating §e" + arena.getName());
        });

        return true;
    }

    public void handlePlayerDeath(Player player, Arena arena) {
        arena.getSpectators().add(player.getUniqueId());

        Location specSpawn = arena.getSpectatorSpawn();
        if (specSpawn != null && specSpawn.getWorld() != null) {
            specSpawn.getWorld().getChunkAt(specSpawn).load(true);
            player.teleport(specSpawn);
        }

        resetPlayerState(player);
        player.getInventory().clear();

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        arena.broadcastMessage("§c[MobArenav2] §e" + player.getName() + " §chas fallen and is now spectating!");

        Set<UUID> activePlayers = new HashSet<>(arena.getPlayerProfiles().keySet());
        activePlayers.removeAll(arena.getSpectators());

        if (activePlayers.isEmpty()) {
            arena.broadcastMessage("§c§l[MobArenav2] All players have died! The arena is over.");
            arena.endArena(plugin);
        }
    }

    public void resetPlayerState(Player player) {
        if (player == null || !player.isOnline()) return;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttr != null) hpAttr.setBaseValue(20.0);

        AttributeInstance dmgAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(1.0);

        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0.0f);

        player.setHealth(Math.min(player.getHealth(), 20.0));
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
        player.setInvulnerable(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2f);
    }

    public void leaveArena(Player player) {
        Arena arena = getArenaByPlayer(player);
        if (arena == null) return;

        boolean wasSpectator = arena.getSpectators().contains(player.getUniqueId());
        ArenaPlayerProfile profile = arena.getPlayerProfiles().remove(player.getUniqueId());

        if (wasSpectator) {
            arena.getSpectators().remove(player.getUniqueId());
        }

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        resetPlayerState(player);

        Location leaveLoc = null;
        if (profile != null && profile.getOriginalLocation() != null) {
            leaveLoc = profile.getOriginalLocation();
        } else if (arena.getLobby() != null && arena.getLobby().getWorld() != null) {
            leaveLoc = arena.getLobby().getWorld().getSpawnLocation();
        }

        if (leaveLoc != null && leaveLoc.getWorld() != null) {
            leaveLoc.getWorld().getChunkAt(leaveLoc).load(true);
            player.teleport(leaveLoc);
        }

        plugin.getInventoryBackupManager().restoreBackup(player);

        if (profile != null) {
            if (profile.getScoreboard() != null) profile.getScoreboard().remove();
            profile.distributeRewards(plugin);
            plugin.getUserDataManager().removeData(player);
        }

        player.sendMessage("§e[MobArenav2] You have left the arena and your items have been restored.");

        if (!wasSpectator && arena.isRunning()) {
            Set<UUID> activePlayers = new HashSet<>(arena.getPlayerProfiles().keySet());
            activePlayers.removeAll(arena.getSpectators());
            if (activePlayers.isEmpty()) {
                arena.broadcastMessage("§c§l[MobArenav2] All active players have left! The arena is over.");
                arena.endArena(plugin);
            }
        }
    }

    public void handlePlayerDisconnect(Player player) {
        Arena arena = getArenaByPlayer(player);
        if (arena != null) {
            leaveArena(player);
            plugin.getLogger().info("[MobArenav2] Player " + player.getName() + " disconnected mid-game. State saved and reset safely.");
        }
    }
}