package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ArenaRestorer {

    private final MobArenav2 plugin;
    private final File backupFolder;

    public ArenaRestorer(MobArenav2 plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    public void backupArena(Arena arena) {
        // SỬA LỖI TẠI ĐÂY: Dùng getP1() và getP2()
        if (arena.getP1() == null || arena.getP2() == null) {
            plugin.getLogger().warning("Cannot backup arena " + arena.getName() + " - Missing P1 or P2!");
            return;
        }

        File backupFile = new File(backupFolder, arena.getName() + ".mabackup");
        World world = arena.getP1().getWorld();

        int minX = Math.min(arena.getP1().getBlockX(), arena.getP2().getBlockX());
        int minY = Math.min(arena.getP1().getBlockY(), arena.getP2().getBlockY());
        int minZ = Math.min(arena.getP1().getBlockZ(), arena.getP2().getBlockZ());

        int maxX = Math.max(arena.getP1().getBlockX(), arena.getP2().getBlockX());
        int maxY = Math.max(arena.getP1().getBlockY(), arena.getP2().getBlockY());
        int maxZ = Math.max(arena.getP1().getBlockZ(), arena.getP2().getBlockZ());

        List<String> blockSnapshot = new ArrayList<>();
        blockSnapshot.add("ORIGIN:" + minX + "," + minY + "," + minZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        int relX = x - minX;
                        int relY = y - minY;
                        int relZ = z - minZ;
                        blockSnapshot.add(relX + "," + relY + "," + relZ + ";" + block.getBlockData().getAsString());
                    }
                }
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
                for (String line : blockSnapshot) {
                    writer.write(line);
                    writer.newLine();
                }
                plugin.getLogger().info("Successfully backed up " + (blockSnapshot.size() - 1) + " blocks for arena: " + arena.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to backup arena: " + arena.getName());
                e.printStackTrace();
            }
        });
    }

    public void restoreArena(Arena arena) {
        // SỬA LỖI TẠI ĐÂY: Dùng getP1() và getP2()
        if (arena.getP1() == null || arena.getP2() == null) return;

        File backupFile = new File(backupFolder, arena.getName() + ".mabackup");
        if (!backupFile.exists()) {
            plugin.getLogger().warning("No backup file found for arena: " + arena.getName());
            return;
        }

        World world = arena.getP1().getWorld();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int minX = Math.min(arena.getP1().getBlockX(), arena.getP2().getBlockX());
                int minY = Math.min(arena.getP1().getBlockY(), arena.getP2().getBlockY());
                int minZ = Math.min(arena.getP1().getBlockZ(), arena.getP2().getBlockZ());
                int maxX = Math.max(arena.getP1().getBlockX(), arena.getP2().getBlockX());
                int maxY = Math.max(arena.getP1().getBlockY(), arena.getP2().getBlockY());
                int maxZ = Math.max(arena.getP1().getBlockZ(), arena.getP2().getBlockZ());

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block b = world.getBlockAt(x, y, z);
                            if (b.getType() != Material.AIR) {
                                b.setType(Material.AIR, false);
                            }
                        }
                    }
                }

                int originX = 0, originY = 0, originZ = 0;
                for (String line : lines) {
                    if (line.startsWith("ORIGIN:")) {
                        String[] coords = line.substring(7).split(",");
                        originX = Integer.parseInt(coords[0]);
                        originY = Integer.parseInt(coords[1]);
                        originZ = Integer.parseInt(coords[2]);
                        continue;
                    }

                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String[] relCoords = parts[0].split(",");
                        int x = originX + Integer.parseInt(relCoords[0]);
                        int y = originY + Integer.parseInt(relCoords[1]);
                        int z = originZ + Integer.parseInt(relCoords[2]);

                        BlockData blockData = Bukkit.createBlockData(parts[1]);
                        world.getBlockAt(x, y, z).setBlockData(blockData, false);
                    }
                }
                plugin.getLogger().info("Arena " + arena.getName() + " has been successfully restored!");
            });
        });
    }
}