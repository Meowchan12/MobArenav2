package vn.meowchan12.mobarenav2.manager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;

import java.util.ArrayList;
import java.util.List;

public class ArenaExpansion extends PlaceholderExpansion {

    private final MobArenav2 plugin;

    public ArenaExpansion(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "mobarenav2"; }

    @Override
    public @NotNull String getAuthor() { return "Meowchan12"; }

    @Override
    public @NotNull String getVersion() { return "2.0"; }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {

        // ==========================================
        // NHÓM 1: GLOBAL ARENA STATS
        // ==========================================
        if (params.startsWith("arena_status_")) {
            String arenaName = params.substring(13);
            Arena target = plugin.getArenaManager().getArena(arenaName);
            if (target == null) return "Disabled";
            return target.isRunning() ? "In Progress" : "Waiting";
        }

        if (params.startsWith("arena_players_")) {
            String arenaName = params.substring(14);
            Arena target = plugin.getArenaManager().getArena(arenaName);
            return target == null ? "0" : String.valueOf(target.getActivePlayers().size());
        }

        if (params.startsWith("arena_wave_")) {
            String arenaName = params.substring(11);
            Arena target = plugin.getArenaManager().getArena(arenaName);
            return target == null ? "0" : String.valueOf(target.getCurrentWave());
        }

        if (params.startsWith("total_kills_")) {
            String arenaName = params.substring(12);
            Arena target = plugin.getArenaManager().getArena(arenaName);
            if (target == null) return "0";
            int total = 0;
            for (ArenaPlayerProfile prof : target.getPlayerProfiles().values()) total += prof.getKills();
            return String.valueOf(total);
        }

        if (player == null) return "";

        // ==========================================
        // NHÓM 2: LIFETIME STATS (Thống kê vĩnh viễn)
        // ==========================================
        if (params.equalsIgnoreCase("lifetime_kills")) {
            return String.valueOf(plugin.getUserDataManager().getStat(player.getUniqueId(), "lifetime_kills"));
        }
        if (params.equalsIgnoreCase("games_played")) {
            return String.valueOf(plugin.getUserDataManager().getStat(player.getUniqueId(), "games_played"));
        }
        if (params.equalsIgnoreCase("games_won")) {
            return String.valueOf(plugin.getUserDataManager().getStat(player.getUniqueId(), "games_won"));
        }
        if (params.equalsIgnoreCase("highest_wave")) {
            return String.valueOf(plugin.getUserDataManager().getStat(player.getUniqueId(), "highest_wave"));
        }

        // ==========================================
        // NHÓM 3: IN-GAME STATE
        // ==========================================
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);

        if (params.equalsIgnoreCase("arena")) {
            return arena != null ? arena.getName() : "None";
        }

        if (params.equalsIgnoreCase("player_state")) {
            if (arena == null) return "Lobby";
            if (arena.getSpectators().contains(player.getUniqueId())) return "Spectator";
            if (arena.isRunning()) return "Alive";
            return "Waiting";
        }

        if (params.equalsIgnoreCase("player_is_ready")) {
            if (arena != null) {
                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
                if (profile != null) return profile.isReady() ? "Ready" : "Not Ready";
            }
            return "None";
        }

        if (params.equalsIgnoreCase("class")) {
            if (arena != null) {
                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
                return (profile != null && profile.getSelectedClass() != null) ? profile.getSelectedClass() : "None";
            }
            return "None";
        }

        if (params.equalsIgnoreCase("kills")) {
            if (arena != null) {
                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
                return profile != null ? String.valueOf(profile.getKills()) : "0";
            }
            return "0";
        }

        if (params.equalsIgnoreCase("current_mobs")) {
            return arena != null ? String.valueOf(arena.getActiveMobs().size()) : "0";
        }

        // ==========================================
        // NHÓM 4: BẢNG XẾP HẠNG TOP TRONG TRẬN ĐẤU (MỚI)
        // (Dùng: %mobarenav2_top_player_1%, %mobarenav2_top_kills_1%, v.v.)
        // ==========================================
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                String type = parts[1];
                int rank;
                try {
                    rank = Integer.parseInt(parts[2]) - 1;
                } catch (NumberFormatException e) {
                    return "";
                }

                if (arena != null) {
                    List<ArenaPlayerProfile> allPlayers = new ArrayList<>(arena.getPlayerProfiles().values());
                    allPlayers.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));

                    if (rank >= 0 && rank < allPlayers.size()) {
                        ArenaPlayerProfile prof = allPlayers.get(rank);
                        if (type.equals("player")) {
                            Player p = Bukkit.getPlayer(prof.getPlayerUUID());
                            return p != null ? p.getName() : "Unknown";
                        } else if (type.equals("kills")) {
                            return String.valueOf(prof.getKills());
                        } else if (type.equals("status")) {
                            return arena.getSpectators().contains(prof.getPlayerUUID()) ? "Dead" : "Alive";
                        }
                    } else {
                        if (type.equals("player")) return "- Empty -";
                        if (type.equals("kills")) return "0";
                        if (type.equals("status")) return "-";
                    }
                }
            }
        }

        return null;
    }
}