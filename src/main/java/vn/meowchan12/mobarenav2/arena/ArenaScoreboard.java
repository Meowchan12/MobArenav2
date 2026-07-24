package vn.meowchan12.mobarenav2.arena;

import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaScoreboard {

    private final MobArenav2 plugin;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Player player;

    private int animationTick = 0;
    private int lastLineCount = 0;

    public ArenaScoreboard(Player player, MobArenav2 plugin) {
        this.player = player;
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        FileConfiguration settings = plugin.getConfigManager().getSettings();
        String title = colorize(settings.getString("scoreboard.title", "&c&lMOB ARENA"));

        this.objective = scoreboard.registerNewObjective("ma_board", Criteria.DUMMY, Component.text(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (player.isOnline()) {
            player.setScoreboard(this.scoreboard);
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    public void tick() {
        animationTick++;
    }

    public void update(Arena arena) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration settings = plugin.getConfigManager().getSettings();
        if (!settings.getBoolean("scoreboard.enabled", true)) {
            remove();
            return;
        }

        int currentWave = arena.getCurrentWave();
        int kills = 0;
        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
        if (profile != null) kills = profile.getKills();

        String nextBossStr = getNextBossWave(arena, currentWave);
        String nextSupplyStr = getNextSupplyWave(arena, currentWave);

        List<String> rawLines = settings.getStringList("scoreboard.lines");
        List<String> formattedLines = new ArrayList<>();

        // --- THUẬT TOÁN PHÂN TRANG (PAGINATION) ---
        boolean insidePagination = false;
        List<String> paginationTemplate = new ArrayList<>();
        int playersPerPage = 4;

        List<ArenaPlayerProfile> allPlayers = new ArrayList<>(arena.getPlayerProfiles().values());
        allPlayers.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));

        int totalPages = (int) Math.ceil((double) allPlayers.size() / playersPerPage);
        if (totalPages == 0) totalPages = 1;

        // Đổi trang mỗi 10 ticks (tương đương 10 giây nếu gọi 1s/lần)
        int currentPage = (animationTick / 10) % totalPages;
        int startIndex = currentPage * playersPerPage;

        for (String raw : rawLines) {
            // Mở/đóng block phân trang khi gặp "+++"
            if (raw.trim().equals("+++")) {
                if (!insidePagination) {
                    insidePagination = true;
                    paginationTemplate.clear();
                } else {
                    insidePagination = false;
                    // Bắt đầu in dữ liệu Player cho trang hiện tại
                    for (int i = 0; i < playersPerPage; i++) {
                        int index = startIndex + i;
                        if (index < allPlayers.size()) {
                            ArenaPlayerProfile prof = allPlayers.get(index);
                            Player p = Bukkit.getPlayer(prof.getPlayerUUID());
                            String pName = (p != null) ? p.getName() : "Unknown";
                            String status = arena.getSpectators().contains(prof.getPlayerUUID()) ? "&cDead" : "&aAlive";

                            for (String tempLine : paginationTemplate) {
                                String parsed = tempLine
                                        .replace("%index%", String.valueOf(index + 1))
                                        .replace("%player%", pName)
                                        .replace("%kills%", String.valueOf(prof.getKills()))
                                        .replace("%status%", status);
                                formattedLines.add(colorize(parsed));
                            }
                        } else {
                            // Ô trống nếu không đủ 4 người
                            formattedLines.add(colorize(" &8- Empty -"));
                        }
                    }
                }
                continue;
            }

            if (insidePagination) {
                // Thu thập các dòng cấu trúc nằm giữa 2 dấu +++
                paginationTemplate.add(raw);
            } else {
                // Xử lý các dòng bình thường bên ngoài
                String line = raw
                        .replace("%arena%", arena.getName())
                        .replace("%wave%", String.valueOf(currentWave))
                        .replace("%next_wave%", String.valueOf(currentWave + 1))
                        .replace("%mobs%", String.valueOf(arena.getActiveMobs().size()))
                        .replace("%players%", String.valueOf(arena.getActivePlayers().size()))
                        .replace("%dead%", String.valueOf(arena.getSpectators().size()))
                        .replace("%kills%", String.valueOf(kills))
                        .replace("%next_boss%", nextBossStr)
                        .replace("%next_supply%", nextSupplyStr);

                formattedLines.add(colorize(line));
            }
        }

        if (lastLineCount > formattedLines.size()) {
            for (int i = formattedLines.size() + 1; i <= lastLineCount; i++) {
                String entry = "§" + getColorCode(i) + "§r";
                scoreboard.resetScores(entry);
            }
        }
        lastLineCount = formattedLines.size();

        int score = formattedLines.size();
        for (String lineText : formattedLines) {
            setLine(score, lineText);
            score--;
        }
    }

    private String getNextBossWave(Arena arena, int currentWave) {
        FileConfiguration config = arena.getConfig();
        if (config == null || !config.contains("waves")) return "None";
        ConfigurationSection wavesSec = config.getConfigurationSection("waves");
        if (wavesSec == null) return "None";

        for (int w = currentWave + 1; w <= currentWave + 100; w++) {
            for (String key : wavesSec.getKeys(false)) {
                ConfigurationSection sec = wavesSec.getConfigurationSection(key);
                if (sec != null && isBossSection(sec)) {
                    String waveCond = sec.getString("waves", "");
                    if (isWaveInRange(w, waveCond)) return String.valueOf(w);
                }
            }
        }
        return "None";
    }

    private boolean isBossSection(ConfigurationSection sec) {
        if (sec == null) return false;
        if (sec.getBoolean("is-boss", false) || sec.getBoolean("is_boss", false)) return true;
        String type = sec.getString("type", sec.getString("wave_type", sec.getString("wave_types", "")));
        if ("boss".equalsIgnoreCase(type) || "boss_wave".equalsIgnoreCase(type)) return true;
        return sec.contains("boss") || sec.contains("bosses");
    }

    private String getNextSupplyWave(Arena arena, int currentWave) {
        FileConfiguration config = arena.getConfig();
        if (config != null && config.contains("waves")) {
            ConfigurationSection wavesSec = config.getConfigurationSection("waves");
            if (wavesSec != null) {
                for (int w = currentWave + 1; w <= currentWave + 100; w++) {
                    for (String key : wavesSec.getKeys(false)) {
                        ConfigurationSection sec = wavesSec.getConfigurationSection(key);
                        if (sec != null && "supply".equalsIgnoreCase(sec.getString("wave_types"))) {
                            String waveCond = sec.getString("waves", "");
                            if (isWaveInRange(w, waveCond)) return String.valueOf(w);
                        }
                    }
                }
            }
        }
        if (config != null && config.contains("supplies")) {
            ConfigurationSection sec = config.getConfigurationSection("supplies");
            if (sec != null) {
                for (int w = currentWave + 1; w <= currentWave + 100; w++) {
                    if (sec.contains(String.valueOf(w))) return String.valueOf(w);
                }
            }
        }
        int triggerEvery = plugin.getUpgradeManager().getTriggerEvery();
        if (triggerEvery > 0) {
            int next = ((currentWave / triggerEvery) + 1) * triggerEvery;
            if (next > currentWave) return String.valueOf(next);
        }
        return "None";
    }

    private boolean isWaveInRange(int wave, String rangeStr) {
        if (rangeStr == null || rangeStr.isEmpty()) return false;
        if (rangeStr.contains(",")) {
            String[] parts = rangeStr.split(",");
            for (String part : parts) {
                try { if (Integer.parseInt(part.trim()) == wave) return true; } catch (Exception ignored) {}
            }
        }
        if (rangeStr.contains("-")) {
            String[] parts = rangeStr.split("-");
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return wave >= min && wave <= max;
            } catch (Exception ignored) {}
        }
        if (rangeStr.endsWith("+")) {
            try {
                int min = Integer.parseInt(rangeStr.replace("+", "").trim());
                return wave >= min;
            } catch (Exception ignored) {}
        }
        try { return Integer.parseInt(rangeStr.trim()) == wave; } catch (Exception ignored) { return false; }
    }

    private void setLine(int line, String text) {
        String entry = "§" + getColorCode(line) + "§r";
        Team team = scoreboard.getTeam("line" + line);
        if (team == null) {
            team = scoreboard.registerNewTeam("line" + line);
            team.addEntry(entry);
        }
        team.prefix(Component.text(text));
        objective.getScore(entry).setScore(line);
    }

    private char getColorCode(int line) { return "0123456789abcdef".charAt(line % 16); }

    public void remove() {
        if (player != null && player.isOnline()) player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}