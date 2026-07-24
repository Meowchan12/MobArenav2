package vn.meowchan12.mobarenav2.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;
import vn.meowchan12.mobarenav2.arena.ArenaRestorer;
import vn.meowchan12.mobarenav2.arena.ClassGUI;
import vn.meowchan12.mobarenav2.arena.UpgradeGUI;
import vn.meowchan12.mobarenav2.manager.ArenaManager;
import vn.meowchan12.mobarenav2.manager.SetupManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaCommand implements TabExecutor {

    private final MobArenav2 plugin;
    private final SetupManager setupManager;
    private final ArenaManager arenaManager;

    public ArenaCommand(MobArenav2 plugin, SetupManager setupManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.setupManager = setupManager;
        this.arenaManager = arenaManager;
    }

    private static class PlayerStat {
        String name;
        String clazz;
        int kills;

        PlayerStat(String name, String clazz, int kills) {
            this.name = name;
            this.clazz = clazz;
            this.kills = kills;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use MobArenav2 commands!");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "upgrades" -> {
                UpgradeGUI.openPreview(player, plugin);
            }
            case "top" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma top <arena> [page]");
                    return true;
                }
                Arena arena = arenaManager.getArena(args[1]);
                if (arena == null) {
                    player.sendMessage("§cArena not found!");
                    return true;
                }

                FileConfiguration config = arena.getConfig();
                if (config == null || !config.contains("last-match.players")) {
                    player.sendMessage("§cNo match records found for this arena yet!");
                    return true;
                }

                int wave = config.getInt("last-match.wave", 0);
                boolean isEndless = config.getBoolean("last-match.is-endless", false);

                ConfigurationSection playersSec = config.getConfigurationSection("last-match.players");
                if (playersSec == null) return true;

                List<PlayerStat> stats = new ArrayList<>();
                for (String uuidStr : playersSec.getKeys(false)) {
                    String path = uuidStr + ".";
                    String name = playersSec.getString(path + "name", "Unknown");
                    String clazz = playersSec.getString(path + "class", "None");
                    int kills = playersSec.getInt(path + "kills", 0);
                    stats.add(new PlayerStat(name, clazz, kills));
                }

                stats.sort((a, b) -> Integer.compare(b.kills, a.kills));

                int page = 1;
                if (args.length >= 3) {
                    try { page = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }

                int perPage = 5;
                int totalPages = (int) Math.ceil((double) stats.size() / perPage);
                if (totalPages == 0) totalPages = 1;
                if (page < 1) page = 1;
                if (page > totalPages) page = totalPages;

                player.sendMessage("§8§m----------------------------------");
                player.sendMessage("§6§l MATCH SUMMARY §7- §e" + arena.getName());
                player.sendMessage("§f Final Wave: §b" + wave + (isEndless ? " §8(Endless)" : ""));
                player.sendMessage("§8§m----------------------------------");

                int start = (page - 1) * perPage;
                int end = Math.min(start + perPage, stats.size());

                for (int i = start; i < end; i++) {
                    PlayerStat stat = stats.get(i);
                    player.sendMessage(" §e" + (i + 1) + ". §f" + stat.name + " §7- §c" + stat.kills + " Kills");
                    player.sendMessage("    §8↳ Class: §7" + stat.clazz);
                }

                player.sendMessage(" ");

                TextComponent footer = new TextComponent("§7Page " + page + "/" + totalPages + "   ");

                if (page > 1) {
                    TextComponent prev = new TextComponent("§a[◀ Previous] ");
                    prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ma top " + arena.getName() + " " + (page - 1)));
                    footer.addExtra(prev);
                }
                if (page < totalPages) {
                    TextComponent next = new TextComponent("§a[Next ▶]");
                    next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ma top " + arena.getName() + " " + (page + 1)));
                    footer.addExtra(next);
                }

                player.spigot().sendMessage(footer);
                player.sendMessage("§8§m----------------------------------");
            }

            case "join" -> {
                if (!player.hasPermission("mobarenav2.player.join")) {
                    player.sendMessage("§cYou do not have permission to join arenas.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma join <arena>");
                    return true;
                }
                arenaManager.joinArena(player, args[1]);
            }

            case "leave" -> {
                if (!player.hasPermission("mobarenav2.player.leave")) {
                    player.sendMessage("§cYou do not have permission to leave.");
                    return true;
                }
                arenaManager.leaveArena(player);
            }

            case "class" -> {
                Arena arena = arenaManager.getArenaByPlayer(player);
                if (arena == null) {
                    player.sendMessage("§cYou must join an arena first to select a class!");
                    return true;
                }
                if (arena.isRunning()) {
                    player.sendMessage("§cYou cannot change your class after the arena has started!");
                    return true;
                }
                ClassGUI.open(player, plugin);
            }

            case "ready" -> {
                Arena arena = arenaManager.getArenaByPlayer(player);
                if (arena == null) {
                    player.sendMessage("§cYou must join an arena first to get ready!");
                    return true;
                }
                if (arena.isRunning()) {
                    player.sendMessage("§cThe arena has already started!");
                    return true;
                }

                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
                if (profile != null) {
                    if (profile.getSelectedClass() == null) {
                        player.sendMessage("§c[MobArenav2] You must select a Class before getting ready! Use /ma class");
                        return true;
                    }

                    if (profile.isReady()) {
                        player.sendMessage("§eYou are already ready!");
                        return true;
                    }

                    profile.setReady(true);
                    arena.broadcastMessage("§a[MobArenav2] §e" + player.getName() + " §ais ready!");

                    if (arena.getArenaSpawn() != null) {
                        player.teleport(arena.getArenaSpawn());
                    } else {
                        player.sendMessage("§cWarning: Arena spawn is not set properly!");
                    }

                    arena.checkAndStart(plugin);
                }
            }

            case "spec" -> {
                if (!player.hasPermission("mobarenav2.player.spec")) {
                    player.sendMessage("§cYou do not have permission to spectate.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma spec <arena>");
                    return true;
                }
                arenaManager.joinSpectator(player, args[1]);
            }

            case "list" -> {
                if (!player.hasPermission("mobarenav2.player.list")) {
                    player.sendMessage("§cYou do not have permission to view arenas.");
                    return true;
                }
                player.sendMessage("§a--- Available Arenas ---");
                if (arenaManager.getAllArenas().isEmpty()) {
                    player.sendMessage("§7No arenas available.");
                } else {
                    for (Arena a : arenaManager.getAllArenas()) {
                        String status = a.isRunning() ? "§c[In Progress]" : "§a[Waiting]";
                        player.sendMessage("§f- §e" + a.getName() + " " + status + " §7(" + a.getActivePlayers().size() + " players)");
                    }
                }
            }

            case "setup", "setp1", "setp2", "setspectator", "savearena", "save", "reload", "tp", "supply", "inv" -> {
                if (!player.hasPermission("mobarenav2.admin")) {
                    player.sendMessage("§cYou do not have permission to use admin commands.");
                    return true;
                }
                handleAdminCommands(player, subCommand, args);
            }
            default -> player.sendMessage("§cUnknown command. Type /ma for help.");
        }
        return true;
    }

    private void handleAdminCommands(Player player, String subCommand, String[] args) {
        switch (subCommand) {
            case "inv" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma inv <player>");
                    return;
                }
                String targetName = args[1];

                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

                if (plugin.getInventoryBackupManager() != null) {
                    String displayName = target.getName() != null ? target.getName() : targetName;
                    plugin.getInventoryBackupManager().openBackupGUI(player, target.getUniqueId(), displayName);
                } else {
                    player.sendMessage("§c[MobArenav2] The Inventory Backup System is currently disabled or failed to load!");
                }
            }

            case "save" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma save <arena>");
                    return;
                }
                String arenaName = args[1].toLowerCase();
                Arena arena = arenaManager.getArena(arenaName);
                if (arena != null) {
                    plugin.getConfigManager().saveArenaCoords(arena);
                    player.sendMessage("§a[MobArenav2] Coordinates for arena §e" + arena.getName() + " §ahave been successfully saved to file!");
                } else {
                    player.sendMessage("§cArena not found!");
                }
            }

            case "reload" -> {
                plugin.getConfigManager().loadAllConfigs();
                plugin.getSupplyManager().loadConfig();
                plugin.getBossManager().reloadConfigs();
                plugin.getArenaManager().reloadArenas();
                player.sendMessage("§a[MobArenav2] All data (Settings, Classes, Arenas, Bosses, Supplies, Upgrades) has been reloaded successfully!");
            }

            case "tp" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma tp <arena>");
                    return;
                }
                Arena arena = arenaManager.getArena(args[1]);
                if (arena == null) {
                    player.sendMessage("§cArena not found!");
                    return;
                }
                if (arena.getLobby() != null) {
                    player.teleport(arena.getLobby());
                    player.sendMessage("§aTeleported to the lobby of §e" + arena.getName());
                } else if (arena.getArenaSpawn() != null) {
                    player.teleport(arena.getArenaSpawn());
                    player.sendMessage("§aTeleported to the spawn of §e" + arena.getName());
                } else {
                    player.sendMessage("§cThis arena has no spawn or lobby set yet!");
                }
            }
            case "setup" -> {
                if (setupManager.isInSetupMode(player)) {
                    setupManager.exitSetupMode(player);
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma setup <arena_name>");
                    return;
                }
                String arenaName = args[1].toLowerCase();
                Arena arena = arenaManager.getArena(arenaName);

                if (arena == null) {
                    plugin.getConfigManager().createArenaTemplate(arenaName);
                    arena = new Arena(arenaName);
                    java.io.File file = new java.io.File(plugin.getConfigManager().getArenasFolder(), arenaName + ".yml");
                    arena.setConfig(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file));
                    arenaManager.addArena(arena);
                    player.sendMessage("§aCreated new arena blueprint: §e" + arenaName);
                }
                setupManager.enterSetupMode(player, arena);
            }
            case "setp1" -> player.sendMessage("§aPoint 1 has been set at your current location.");
            case "setp2" -> player.sendMessage("§aPoint 2 has been set at your current location.");

            case "setspectator" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma setspectator <arena>");
                    return;
                }
                Arena arena = arenaManager.getArena(args[1]);
                if (arena != null) {
                    arena.setSpectatorSpawn(player.getLocation());
                    plugin.getConfigManager().saveArenaCoords(arena);
                    player.sendMessage("§aSpectator spawn point has been set at your location for arena: §e" + arena.getName());
                } else {
                    player.sendMessage("§cArena not found!");
                }
            }

            case "savearena" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ma savearena <arena>");
                    return;
                }
                String arenaName = args[1];
                Arena arena = arenaManager.getArena(arenaName);
                if (arena != null) {
                    player.sendMessage("§aStarting block backup for arena: §e" + arenaName + "§a...");
                    new ArenaRestorer(plugin).backupArena(arena);
                } else {
                    player.sendMessage("§cArena not found!");
                }
            }
            case "supply" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /ma supply <create|delete|reset|edit> <id>");
                    return;
                }
                String action = args[1].toLowerCase();
                String id = args[2].toLowerCase();

                switch (action) {
                    case "create" -> {
                        if (plugin.getSupplyManager().createSupply(id)) {
                            player.sendMessage("§aCreated supply drop: §e" + id);
                        } else {
                            player.sendMessage("§cSupply '" + id + "' already exists!");
                        }
                    }
                    case "delete" -> {
                        if (plugin.getSupplyManager().deleteSupply(id)) {
                            player.sendMessage("§aDeleted supply drop: §e" + id);
                        } else {
                            player.sendMessage("§cSupply '" + id + "' does not exist!");
                        }
                    }
                    case "reset" -> {
                        if (plugin.getSupplyManager().resetSupply(id)) {
                            player.sendMessage("§aCleared all items from supply: §e" + id);
                        } else {
                            player.sendMessage("§cSupply '" + id + "' does not exist!");
                        }
                    }
                    case "edit" -> {
                        plugin.getSupplyManager().openEditGUI(player, id);
                    }
                    default -> player.sendMessage("§cUnknown supply action. Use create/delete/reset/edit.");
                }
            }
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§c§l MobArenav2 §7- Commands");
        player.sendMessage("§a /ma join <arena> §7- Join an arena");
        player.sendMessage("§a /ma class §7- Select your combat class");
        player.sendMessage("§a /ma ready §7- Mark yourself as ready");
        player.sendMessage("§a /ma upgrades §7- Preview all available upgrades");
        player.sendMessage("§a /ma spec <arena> §7- Spectate an arena");
        player.sendMessage("§a /ma leave §7- Leave your current arena");
        player.sendMessage("§a /ma top <arena> [page] §7- View match summary");
        player.sendMessage("§a /ma list §7- View all available arenas");
        if (player.hasPermission("mobarenav2.admin")) {
            player.sendMessage("§c /ma inv <player> §7- View & inspect a player's backup inventory");
            player.sendMessage("§c /ma tp <arena> §7- Teleport to an arena");
            player.sendMessage("§c /ma setup <arena> §7- Enter/Exit visual setup mode");
            player.sendMessage("§c /ma setspectator <arena> §7- Set spectator spawn location");
            player.sendMessage("§c /ma save <arena> §7- Force save arena coordinates");
            player.sendMessage("§c /ma reload §7- Reload all configuration files");
            player.sendMessage("§c /ma savearena <arena> §7- Backup arena blocks to file");
            player.sendMessage("§c /ma supply <create|edit|delete|reset> <id> §7- Manage supply drops");
        }
        player.sendMessage("§8§m----------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("join", "leave", "class", "ready", "spec", "list", "top", "upgrades"));
            if (sender.hasPermission("mobarenav2.admin")) {
                subCommands.addAll(Arrays.asList("setup", "setp1", "setp2", "setspectator", "savearena", "save", "reload", "tp", "supply", "inv"));
            }
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("join", "spec", "tp", "save", "savearena", "setup", "setspectator", "top").contains(sub)) {
                List<String> arenaNames = arenaManager.getAllArenas().stream().map(Arena::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], arenaNames, completions);
            } else if (sub.equals("supply") && sender.hasPermission("mobarenav2.admin")) {
                List<String> supplyActions = Arrays.asList("create", "delete", "reset", "edit");
                StringUtil.copyPartialMatches(args[1], supplyActions, completions);
            } else if (sub.equals("inv") && sender.hasPermission("mobarenav2.admin")) {
                return null;
            }
        }

        Collections.sort(completions);
        return completions;
    }
}