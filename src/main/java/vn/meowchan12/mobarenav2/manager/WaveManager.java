package vn.meowchan12.mobarenav2.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;
import vn.meowchan12.mobarenav2.arena.MobFactory;
import vn.meowchan12.mobarenav2.arena.VoteGUI;
import vn.meowchan12.mobarenav2.utils.ArenaMath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WaveManager {

    private final MobArenav2 plugin;
    private final MobFactory mobFactory;

    public WaveManager(MobArenav2 plugin) {
        this.plugin = plugin;
        this.mobFactory = new MobFactory(plugin);
    }

    public void startArena(Arena arena) {
        arena.setRunning(true);
        arena.setCurrentWave(0);
        arena.setVoting(false);
        arena.setEndlessMode(false);
        arena.getEndlessWaveQueue().clear();
        arena.getActiveSupplies().clear();

        arena.broadcastMessage("§c§lThe arena has started! Prepare yourself!");

        new BukkitRunnable() {
            int halfSecondTicks = 0;
            int waveActiveHalfSeconds = 0;
            double nextWaveDelay = 6.0;
            boolean isWaiting = true;

            int lastMobCount = -1;
            int ticksSinceLastKill = 0;
            int spawnCooldown = 0;

            String currentWaveType = "mob";
            boolean currentWaveIsBoss = false;
            double currentSupplyDuration = 30.0;

            int lightningTimer = 0;

            @Override
            public void run() {
                if (!arena.isRunning() || arena.getActivePlayers().isEmpty()) {
                    arena.endArena(plugin);
                    this.cancel();
                    return;
                }

                FileConfiguration config = arena.getConfig();

                if (arena.isVoting()) {
                    arena.setVoteTicks(arena.getVoteTicks() + 1);
                    if (arena.getVoteTicks() >= 60) {
                        concludeVote(arena);
                    }
                    for (UUID uuid : arena.getActivePlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendActionBar(net.kyori.adventure.text.Component.text("§b⏳ Waiting for votes... " + (30 - (arena.getVoteTicks() / 2)) + "s"));
                    }
                    return;
                }

                arena.getActiveMobs().removeIf(mob -> mob == null || !mob.isValid() || mob.isDead());

                if (isWaiting) {
                    if (halfSecondTicks >= nextWaveDelay * 2) {
                        isWaiting = false;
                        halfSecondTicks = 0;
                        waveActiveHalfSeconds = 0;

                        lightningTimer = 0;
                        lastMobCount = -1;
                        ticksSinceLastKill = 0;
                        spawnCooldown = 6;

                        arena.setCurrentWave(arena.getCurrentWave() + 1);

                        ConfigurationSection targetWave = arena.getWaveConfig(arena.getCurrentWave());
                        currentWaveType = (targetWave != null) ? targetWave.getString("wave_types", "mob").toLowerCase() : "mob";
                        currentWaveIsBoss = currentWaveType.equals("boss") || (targetWave != null && targetWave.getBoolean("is-boss", false));

                        if (currentWaveType.equals("supply") || currentWaveType.equals("bonus")) {
                            currentSupplyDuration = (targetWave != null) ? targetWave.getDouble("duration", 30.0) : 30.0;
                        }

                        spawnWave(arena, arena.getCurrentWave(), targetWave, currentWaveType);
                    }
                } else {
                    waveActiveHalfSeconds++;
                    if (spawnCooldown > 0) spawnCooldown--;

                    boolean shouldAdvance = false;

                    if (!currentWaveType.equals("supply") && !currentWaveType.equals("bonus")) {
                        int currentMobCount = arena.getActiveMobs().size();

                        ConfigurationSection lightningSec = config.getConfigurationSection("settings.hardcore-mechanics.lightning-strike");
                        if (lightningSec == null) {
                            lightningSec = config.getConfigurationSection("hardcore-mechanics.lightning-strike");
                        }

                        boolean lightningEnabled = lightningSec != null && lightningSec.getBoolean("enabled", false);
                        int lightningInterval = (lightningSec != null ? lightningSec.getInt("interval", 15) : 15) * 2;

                        if (lightningEnabled) {
                            lightningTimer++;
                            if (lightningTimer >= lightningInterval) {
                                lightningTimer = 0;
                                triggerLightningStrike(arena, lightningSec);
                            }
                        }

                        if (lastMobCount == -1 || currentMobCount < lastMobCount) {
                            lastMobCount = currentMobCount;
                            ticksSinceLastKill = 0;
                        } else {
                            ticksSinceLastKill++;
                        }

                        if (ticksSinceLastKill == 120 && currentMobCount > 0) {
                            arena.broadcastMessage("§e[MobArenav2] The remaining monsters are hiding! They are now glowing.");
                            for (org.bukkit.entity.LivingEntity mob : arena.getActiveMobs()) {
                                if (mob != null && mob.isValid()) {
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                                }
                            }
                        }

                        boolean requireClear = config.getBoolean("settings.wave-conditions.clear-mob-before-next-wave", true);
                        boolean requireClearBoss = config.getBoolean("settings.wave-conditions.clear-mob-before-next-wave-boss", true);
                        boolean forceClearBoss = config.getBoolean("settings.wave-conditions.clear-boss-before-next-wave", true);
                        double delayRaw = config.getDouble("settings.wave-conditions.next-wave", 6.0);

                        if (spawnCooldown == 0 && currentMobCount == 0) {
                            shouldAdvance = true;
                        }

                        if (!requireClear && waveActiveHalfSeconds >= delayRaw * 2) {
                            if (currentWaveIsBoss && forceClearBoss && currentMobCount > 0) {
                                shouldAdvance = false;
                            } else if (currentWaveIsBoss && requireClearBoss && currentMobCount > 0) {
                                shouldAdvance = false;
                            } else {
                                shouldAdvance = true;
                            }
                        }
                    } else {
                        if (waveActiveHalfSeconds >= currentSupplyDuration * 2) {
                            shouldAdvance = true;

                            for (Location loc : arena.getActiveSupplies()) {
                                if (loc.getBlock().getState() instanceof Chest chest) {
                                    for (ItemStack item : chest.getInventory().getContents()) {
                                        if (item != null && item.getType() != Material.AIR) {
                                            loc.getWorld().dropItemNaturally(loc, item);
                                        }
                                    }
                                    chest.getInventory().clear();
                                }
                                loc.getBlock().setType(Material.AIR);
                            }
                            arena.getActiveSupplies().clear();
                        }
                    }

                    if (shouldAdvance) {
                        distributeWaveRewards(arena, arena.getCurrentWave());
                        for (Player p : arena.getPlayersAsObjects()) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        }

                        int maxWaves = config.getInt("settings.max-waves", 20);
                        if (arena.getCurrentWave() >= maxWaves && !arena.isEndlessMode()) {
                            startVote(arena);
                            return;
                        }

                        isWaiting = true;
                        halfSecondTicks = 0;
                        nextWaveDelay = config.getDouble("settings.wave-conditions.next-wave", 6.0);

                        int triggerEvery = plugin.getUpgradeManager().getTriggerEvery();
                        if (triggerEvery > 0 && arena.getCurrentWave() % triggerEvery == 0 && !currentWaveType.equals("supply")) {
                            nextWaveDelay = 10.0;
                            arena.broadcastMessage("§b§l[UPGRADE] §aWave " + arena.getCurrentWave() + " cleared! Choose your mid-game buff!");
                            for (UUID uuid : arena.getActivePlayers()) {
                                Player p = Bukkit.getPlayer(uuid);
                                if (p != null) vn.meowchan12.mobarenav2.arena.UpgradeGUI.open(p, plugin);
                            }
                        } else {
                            arena.broadcastMessage("§aWave cleared! Next wave in " + nextWaveDelay + " seconds.");
                        }
                    }
                }

                for (UUID uuid : arena.getPlayerProfiles().keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(uuid);

                        if (profile != null && profile.getScoreboard() != null) {
                            if (halfSecondTicks % 2 == 0) profile.getScoreboard().tick();
                            profile.getScoreboard().update(arena);
                        }

                        if (!arena.getSpectators().contains(uuid)) {
                            if (isWaiting) {
                                double timeLeft = Math.max(0.0, nextWaveDelay - (halfSecondTicks / 2.0));
                                p.sendActionBar(net.kyori.adventure.text.Component.text("§e⏳ Next wave in " + String.format("%.1f", timeLeft) + " seconds..."));
                            } else if (currentWaveType.equals("supply") || currentWaveType.equals("bonus")) {
                                double timeLeft = Math.max(0.0, currentSupplyDuration - (waveActiveHalfSeconds / 2.0));
                                p.sendActionBar(net.kyori.adventure.text.Component.text("§b🎁 Loot the chest! Drops items in: §e" + String.format("%.1f", timeLeft) + "s"));

                                for (Location loc : arena.getActiveSupplies()) {
                                    if (loc != null) {
                                        for (double y = 0.5; y <= 15.0; y += 1.0) {
                                            loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0.5, y, 0.5), 2, 0.1, 0.1, 0.1, 0.01);
                                        }

                                        org.bukkit.Particle.DustOptions glowingBox = new org.bukkit.Particle.DustOptions(org.bukkit.Color.AQUA, 1.5F);
                                        double[] edges = {0.0, 1.0};
                                        for (double x : edges) {
                                            for (double y : edges) {
                                                for (double z : edges) {
                                                    loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc.clone().add(x, y, z), 2, glowingBox);
                                                }
                                            }
                                        }

                                        loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0.05);
                                    }
                                }
                            } else {
                                p.sendActionBar(net.kyori.adventure.text.Component.text("§c⚔ Mobs remaining: §e" + arena.getActiveMobs().size() + " §c⚔"));
                            }
                        }
                    }
                }

                if (isWaiting) halfSecondTicks++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void triggerLightningStrike(Arena arena, ConfigurationSection lightningSec) {
        Location p1 = arena.getP1();
        Location p2 = arena.getP2();

        List<Player> validTargets = new ArrayList<>();

        if (p1 != null && p2 != null) {
            double minX = Math.min(p1.getX(), p2.getX());
            double maxX = Math.max(p1.getX(), p2.getX());
            double minZ = Math.min(p1.getZ(), p2.getZ());
            double maxZ = Math.max(p1.getZ(), p2.getZ());

            for (Player p : arena.getPlayersAsObjects()) {
                Location loc = p.getLocation();
                if (loc.getX() >= minX && loc.getX() <= maxX && loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                    validTargets.add(p);
                }
            }
        } else {
            validTargets.addAll(arena.getPlayersAsObjects());
        }

        if (validTargets.isEmpty() || lightningSec == null) return;

        int minStrikes = lightningSec.getInt("strikes-per-wave.min", 1);
        int maxStrikes = lightningSec.getInt("strikes-per-wave.max", 3);
        int strikes = ThreadLocalRandom.current().nextInt(minStrikes, maxStrikes + 1);

        double minDmg = lightningSec.getDouble("damage.min", 5.0);
        double maxDmg = lightningSec.getDouble("damage.max", 15.0);
        double explosionRadius = lightningSec.getDouble("damage.radius", 4.0);
        double radiusSquared = explosionRadius * explosionRadius;

        for (int i = 0; i < strikes; i++) {
            Player targetPlayer = validTargets.get(ThreadLocalRandom.current().nextInt(validTargets.size()));

            double silentSec = lightningSec.getDouble("phases.silent-aim", 1.0);
            double trackMinSec = lightningSec.getDouble("phases.tracking-min", 1.0);
            double trackMaxSec = lightningSec.getDouble("phases.tracking-max", 2.0);
            double lockSec = lightningSec.getDouble("phases.lock-on", 0.5);

            int silentScanTicks = (int) (silentSec * 20);
            int trackingTicks = ThreadLocalRandom.current().nextInt((int)(trackMinSec * 20), (int)(trackMaxSec * 20) + 1);
            int lockOnTicks = (int) (lockSec * 20);

            int totalTicks = silentScanTicks + trackingTicks + lockOnTicks;
            int startWarningTick = silentScanTicks;
            int startLockOnTick = silentScanTicks + trackingTicks;

            new BukkitRunnable() {
                int tick = 0;
                Location strikeLoc = targetPlayer.getLocation().clone();

                @Override
                public void run() {
                    if (!arena.isRunning() || !targetPlayer.isOnline()) {
                        this.cancel();
                        return;
                    }

                    Location currentLoc = targetPlayer.getLocation().clone();

                    if (tick < startWarningTick) {
                        strikeLoc = currentLoc;
                    }
                    else if (tick < startLockOnTick) {
                        strikeLoc = currentLoc;
                        drawCircle(strikeLoc, 2.5, org.bukkit.Particle.FLAME);

                        if (tick % 10 == 0) {
                            targetPlayer.sendActionBar(net.kyori.adventure.text.Component.text("§c⚠ YOU ARE TARGETED BY LIGHTNING STRIKE! KEEP MOVING! ⚠"));
                            targetPlayer.playSound(strikeLoc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                        }
                    }
                    else if (tick < totalTicks) {
                        if (tick == startLockOnTick) {
                            targetPlayer.playSound(strikeLoc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 1, false, false));
                        }
                        drawCircle(strikeLoc, 2.5, org.bukkit.Particle.SOUL_FIRE_FLAME);
                    }

                    if (tick >= totalTicks) {
                        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
                        double damage = ThreadLocalRandom.current().nextDouble(minDmg, maxDmg);

                        for (Player p : arena.getPlayersAsObjects()) {
                            if (p.getLocation().distanceSquared(strikeLoc) <= radiusSquared) {
                                double newHealth = p.getHealth() - damage;

                                if (newHealth <= 0) {
                                    plugin.getArenaManager().handlePlayerDeath(p, arena);
                                    p.sendMessage("§c⚡ You were struck down by the LIGHTNING Strike!");
                                } else {
                                    p.setHealth(newHealth);
                                    p.sendMessage("§c⚡ You were hit by the LIGHTNING Strike for " + String.format("%.1f", damage) + " true damage!");
                                }
                            }
                        }

                        this.cancel();
                        return;
                    }

                    tick += 2;
                }

                private void drawCircle(Location center, double radius, org.bukkit.Particle particle) {
                    for (int degree = 0; degree < 360; degree += 20) {
                        double radians = Math.toRadians(degree);
                        double x = radius * Math.cos(radians);
                        double z = radius * Math.sin(radians);
                        center.getWorld().spawnParticle(particle, center.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }

    private void startVote(Arena arena) {
        arena.setVoting(true);
        arena.setVoteTicks(0);
        arena.getActiveVotes().clear();

        arena.broadcastMessage("§e§l[MobArenav2] You have conquered the final wave!");
        arena.broadcastMessage("§bThe Voting Phase has begun. Do you want to continue to ENDLESS MODE?");

        for (UUID uuid : arena.getActivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) VoteGUI.open(p, plugin);
        }
    }

    public void castVote(Player player, Arena arena, boolean continueEndless) {
        if (!arena.getActiveVotes().containsKey(player.getUniqueId())) {
            arena.getActiveVotes().put(player.getUniqueId(), continueEndless);
            String choice = continueEndless ? "§aCONTINUE" : "§cSTOP";
            player.sendMessage("§e[MobArenav2] You voted to " + choice + "§e.");

            if (arena.getActiveVotes().size() >= arena.getActivePlayers().size()) {
                concludeVote(arena);
            }
        }
    }

    private void concludeVote(Arena arena) {
        if (!arena.isVoting()) return;
        arena.setVoting(false);

        int yes = 0, no = 0;
        for (Boolean v : arena.getActiveVotes().values()) {
            if (v) yes++; else no++;
        }

        arena.broadcastMessage("§e§l--- VOTE RESULTS ---");
        arena.broadcastMessage("§aContinue (Yes): " + yes);
        arena.broadcastMessage("§cStop (No): " + no);

        if (yes > no || (yes == no && yes > 0)) {
            arena.broadcastMessage("§a§lThe majority voted YES! Welcome to ENDLESS MODE!");
            arena.setEndlessMode(true);

            for(UUID uuid : arena.getActivePlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if(p != null) p.closeInventory();
            }
        } else {
            arena.broadcastMessage("§c§lThe players chose to STOP. The arena is now ending.");
            arena.endArena(plugin);
        }
    }

    private void spawnWave(Arena arena, int waveNumber, ConfigurationSection targetWave, String waveType) {
        if (waveType.equalsIgnoreCase("supply") || waveType.equalsIgnoreCase("bonus")) {
            arena.broadcastMessage("§b§lA Supply Drop has arrived!");
            String supplyId = (targetWave != null) ? targetWave.getString("supply-id") : null;

            if (supplyId != null) {
                List<ItemStack> items = plugin.getSupplyManager().getSupplyItems(supplyId);
                if (!items.isEmpty()) {
                    Location loc = getSafeSupplyLocation(arena);
                    loc.getBlock().setType(Material.CHEST);

                    if (loc.getBlock().getState() instanceof Chest chest) {
                        for (ItemStack item : items) {
                            if (item != null) chest.getInventory().addItem(item);
                        }
                        arena.getActiveSupplies().add(loc);

                        arena.broadcastMessage("§b[Supply Drop] §eA chest has safely landed at §bX: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ() + "§e!");
                        arena.broadcastMessage("§c⚠ Loot it quickly before it erupts! ⚠");

                        Location fireworkLoc = loc.clone().add(0.5, 1.0, 0.5);
                        if (fireworkLoc.getWorld() != null) {
                            fireworkLoc.getWorld().spawn(fireworkLoc, org.bukkit.entity.Firework.class, fw -> {
                                org.bukkit.inventory.meta.FireworkMeta fwm = fw.getFireworkMeta();
                                if (fwm != null) {
                                    fwm.addEffect(org.bukkit.FireworkEffect.builder()
                                            .withColor(org.bukkit.Color.AQUA, org.bukkit.Color.WHITE)
                                            .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                                            .withTrail()
                                            .withFlicker()
                                            .build());
                                    fwm.setPower(1);
                                    fw.setFireworkMeta(fwm);
                                }
                            });
                        }

                        for (Player p : arena.getPlayersAsObjects()) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
                        }
                    }
                }
            }
            return;
        }

        if (waveType.equalsIgnoreCase("boss") || (targetWave != null && targetWave.getBoolean("is-boss", false))) {
            arena.broadcastMessage("§4§l⚠ BOSS WAVE " + waveNumber + " HAS ARRIVED! ⚠");
            for (Player p : arena.getPlayersAsObjects()) {
                p.sendTitle("§4§lBOSS WAVE!", "§cPrepare for battle!", 10, 70, 20);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }
        } else {
            arena.broadcastMessage("§e§lWave " + waveNumber + " has begun!");
            for (Player p : arena.getPlayersAsObjects()) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.5f);
            }
        }

        int playerCount = arena.getActivePlayers().size();

        if (targetWave != null) {
            if (targetWave.contains("mobs")) {
                ConfigurationSection mobsSection = targetWave.getConfigurationSection("mobs");
                if (mobsSection != null) {
                    double globalMultiplier = waveType.equalsIgnoreCase("swarm") ? targetWave.getDouble("mob-multiplier", 1.0) : 1.0;

                    for (String mobName : mobsSection.getKeys(false)) {
                        ConfigurationSection mobSpecificConfig = mobsSection.getConfigurationSection(mobName);
                        int baseAmount = 1;
                        if (mobSpecificConfig != null && mobSpecificConfig.contains("amount")) {
                            baseAmount = mobSpecificConfig.getInt("amount", 1);
                        } else if (mobsSection.isInt(mobName)) {
                            baseAmount = mobsSection.getInt(mobName);
                        }

                        int amountToSpawn = ArenaMath.calculateSpawnAmount(baseAmount, playerCount, globalMultiplier);
                        for (int i = 0; i < amountToSpawn; i++) {
                            mobFactory.spawnArenaMob(arena, mobName, getSmartSpawnLocation(arena), mobSpecificConfig, targetWave);
                        }
                    }
                }
            } else {
                for (String mobName : targetWave.getKeys(false)) {
                    if (mobName.equals("waves") || mobName.equals("priority") || mobName.equals("is-boss") || mobName.equals("wave_types") || mobName.equals("wave") || mobName.equals("general-effect") || mobName.equals("maxhp-bonus") || mobName.equals("damage-bonus") || mobName.equals("movement-speed") || mobName.equals("explosion-damage") || mobName.equals("bow-speed") || mobName.equals("after-game") || mobName.equals("supply-id") || mobName.equals("duration")) continue;

                    int baseAmount = targetWave.getInt(mobName, 1);
                    int amountToSpawn = ArenaMath.calculateSpawnAmount(baseAmount, playerCount, 1.0);

                    for (int i = 0; i < amountToSpawn; i++) {
                        mobFactory.spawnArenaMob(arena, mobName, getSmartSpawnLocation(arena), null, targetWave);
                    }
                }
            }
        } else {
            int amountToSpawn = ArenaMath.calculateSpawnAmount(5, playerCount, 1.0);
            for (int i = 0; i < amountToSpawn; i++) {
                mobFactory.spawnArenaMob(arena, "ZOMBIE", getSmartSpawnLocation(arena), null, null);
            }
        }
    }

    private void distributeWaveRewards(Arena arena, int wave) {
        FileConfiguration config = arena.getConfig();
        if (config == null || !config.contains("rewards")) return;
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection == null) return;

        List<String> rewardsToGive = new ArrayList<>();

        for (String key : rewardsSection.getKeys(false)) {
            boolean match = false;

            if (key.equals(String.valueOf(wave))) {
                match = true;
            } else if (key.toLowerCase().startsWith("every ")) {
                try {
                    String[] parts = key.split(" ");
                    if (parts.length >= 2) {
                        int interval = Integer.parseInt(parts[1].trim());
                        if (interval > 0 && wave % interval == 0) {
                            match = true;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (match && rewardsSection.isList(key)) {
                List<String> rawList = rewardsSection.getStringList(key);
                boolean isAfterGame = false;
                List<String> cleanRewards = new ArrayList<>();

                for (String line : rawList) {
                    String cleanLine = line.trim().toLowerCase();
                    if (cleanLine.equals("after-game: true") || cleanLine.equals("\"after-game: true\"")) {
                        isAfterGame = true;
                    } else if (cleanLine.equals("after-game: false") || cleanLine.equals("\"after-game: false\"")) {
                        isAfterGame = false;
                    } else {
                        cleanRewards.add(line);
                    }
                }

                if (arena.isEndlessMode() == isAfterGame) {
                    rewardsToGive.addAll(cleanRewards);
                }
            }
        }

        if (rewardsToGive.isEmpty()) return;
        String rewardString = String.join(",", rewardsToGive);

        for (UUID uuid : arena.getActivePlayers()) {
            ArenaPlayerProfile profile = arena.getPlayerProfiles().get(uuid);
            if (profile != null) profile.cacheReward(rewardString);

            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§6[MobArenav2] §eYou secured a reward for clearing Wave " + wave + "!");
                p.sendMessage("§7(Rewards are stashed and will be given when you leave)");
            }
        }
    }

    private Location getSmartSpawnLocation(Arena arena) {
        List<Location> allSpawns = arena.getSpawnpoints();
        if (allSpawns == null || allSpawns.isEmpty()) return arena.getArenaSpawn();

        List<Location> validSpawns = new ArrayList<>();
        double minDistanceSq = Math.pow(arena.getSpawnMinDistance(), 2);

        for (Location spawn : allSpawns) {
            boolean isValid = true;
            for (Player player : arena.getPlayersAsObjects()) {
                if (spawn.distanceSquared(player.getLocation()) < minDistanceSq) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) validSpawns.add(spawn);
        }

        if (validSpawns.isEmpty()) {
            return allSpawns.get(ThreadLocalRandom.current().nextInt(allSpawns.size()));
        }
        return validSpawns.get(ThreadLocalRandom.current().nextInt(validSpawns.size()));
    }

    // THUẬT TOÁN ĐÃ ĐƯỢC THAY THẾ: Lấy tọa độ hòm thính từ danh sách đã Set
    private Location getSafeSupplyLocation(Arena arena) {
        List<Location> supplyPoints = arena.getSupplypoints();

        if (supplyPoints != null && !supplyPoints.isEmpty()) {
            return supplyPoints.get(ThreadLocalRandom.current().nextInt(supplyPoints.size()));
        }

        plugin.getLogger().warning("[MobArenav2] Arena " + arena.getName() + " does not have Supply Points set! Dropping at Arena Spawn.");
        return arena.getArenaSpawn();
    }
}