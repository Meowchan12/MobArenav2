package vn.meowchan12.mobarenav2.arena;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.manager.UpgradeManager.UpgradeData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeGUI implements Listener {

    private final MobArenav2 plugin;

    public UpgradeGUI(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    private static String colorize(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}|#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String hexCode = text.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace("&#", "#");
            text = text.replace(hexCode, ChatColor.of(replaceSharp).toString());
            matcher = pattern.matcher(text);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // ==========================================
    // MENU XEM TRƯỚC UPGRADES (PREVIEW) - QoL
    // ==========================================
    public static void openPreview(Player player, MobArenav2 plugin) {
        Inventory gui = Bukkit.createInventory(null, 54, "§0§l📜 UPGRADE DIRECTORY");

        // 1. Vẽ dải phân cách bằng Kính đen ở giữa (Row 4: Slot 27 đến 35)
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 27; i <= 35; i++) {
            gui.setItem(i, glass);
        }

        List<UpgradeData> pool = plugin.getUpgradeManager().getAvailableUpgrades();

        // 2. Con trỏ tự động chạy nếu không set Slot cứng
        int normalAutoSlot = 0;  // Quét từ ô đầu tiên cho đồ Normal
        int superAutoSlot = 36;  // Quét từ Row 5 (dưới dải kính) cho đồ Super

        for (UpgradeData data : pool) {
            ItemStack item = new ItemStack(data.material);
            ItemMeta meta = item.getItemMeta();
            boolean isSuper = data.id.startsWith("super_") || data.type.startsWith("SUPER_");

            if (meta != null) {
                meta.setDisplayName(colorize(data.displayName.replace("%current_level%", "1").replace("%max_level%", String.valueOf(data.maxLevel))));

                List<String> coloredLore = new ArrayList<>();
                for (String loreLine : data.lore) {
                    coloredLore.add(colorize(loreLine
                            .replace("%current_level%", "1")
                            .replace("%max_level%", String.valueOf(data.maxLevel))
                            .replace("%current_value%", "Base")
                            .replace("%next_value%", "Next")));
                }
                meta.setLore(coloredLore);

                // Thêm hiệu ứng GLINT (phát sáng) cho Super Upgrades
                if (isSuper) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                item.setItemMeta(meta);
            }

            // 3. Logic xếp đồ vào Slot
            if (data.slot != -1 && data.slot < 53) {
                // Ưu tiên 1: Set cứng trong upgrades.yml
                gui.setItem(data.slot, item);
            } else {
                // Ưu tiên 2: Tự động xếp gọn gàng theo khu vực
                if (isSuper) {
                    while (superAutoSlot < 53 && gui.getItem(superAutoSlot) != null) superAutoSlot++;
                    if (superAutoSlot < 53) gui.setItem(superAutoSlot, item);
                } else {
                    while (normalAutoSlot < 27 && gui.getItem(normalAutoSlot) != null) normalAutoSlot++;
                    if (normalAutoSlot < 27) gui.setItem(normalAutoSlot, item);
                }
            }
        }

        // Nút Đóng
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c§lCLOSE PREVIEW");
            closeBtn.setItemMeta(closeMeta);
        }
        gui.setItem(53, closeBtn);

        player.openInventory(gui);
    }

    public static void open(Player player, MobArenav2 plugin) {
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;

        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
        if (profile == null) return;

        String title = plugin.getConfig().getString("gui.upgrade.title", "Choose Upgrade (Wave %wave%)");
        title = colorize(title.replace("%wave%", String.valueOf(arena.getCurrentWave())));

        Inventory inv = Bukkit.createInventory(null, 27, title);

        int maxSlots = plugin.getUpgradeManager().getMaxSlots(player, profile.getSelectedClass());
        int usedSlots = plugin.getUpgradeManager().getUsedSlots(profile);
        boolean isFullSlot = usedSlots >= maxSlots;

        List<UpgradeData> pool = new ArrayList<>();

        for (UpgradeData u : plugin.getUpgradeManager().getAvailableUpgrades()) {
            int currentLevel = profile.getUpgradeLevel(u.id);
            if (currentLevel < u.maxLevel) {
                if (isFullSlot) {
                    if (currentLevel > 0) pool.add(u);
                } else {
                    pool.add(u);
                }
            }
        }

        if (pool.isEmpty()) {
            ItemStack maxedOut = new ItemStack(Material.GOLDEN_APPLE);
            ItemMeta m = maxedOut.getItemMeta();
            m.setDisplayName(colorize("&a&lMaxed Out!"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7You have reached the maximum level"));
            lore.add(colorize("&7for all your active upgrade slots."));
            lore.add(colorize(""));
            lore.add(colorize("&eClick to receive a quick heal instead."));
            m.setLore(lore);

            NamespacedKey key = new NamespacedKey(plugin, "upgrade_id");
            m.getPersistentDataContainer().set(key, PersistentDataType.STRING, "MAXED_OUT_HEAL");
            maxedOut.setItemMeta(m);

            inv.setItem(13, maxedOut);
            player.openInventory(inv);
            return;
        }

        List<UpgradeData> selectedUpgrades = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 3 && !pool.isEmpty(); i++) {
            int totalWeight = pool.stream().mapToInt(u -> u.weight).sum();
            if (totalWeight <= 0) break;

            int value = random.nextInt(totalWeight);
            for (int j = 0; j < pool.size(); j++) {
                value -= pool.get(j).weight;
                if (value < 0) {
                    selectedUpgrades.add(pool.get(j));
                    pool.remove(j);
                    break;
                }
            }
        }

        int[] slots = {11, 13, 15};
        for (int i = 0; i < selectedUpgrades.size(); i++) {
            UpgradeData data = selectedUpgrades.get(i);

            int curLvl = profile.getUpgradeLevel(data.id);
            double curVal = (curLvl == 0) ? 0 : data.baseValue + (curLvl - 1) * data.increment;
            double nextVal = data.baseValue + curLvl * data.increment;

            ItemStack item = new ItemStack(data.material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String dName = data.displayName
                        .replace("%current_level%", String.valueOf(curLvl))
                        .replace("%max_level%", String.valueOf(data.maxLevel));
                meta.setDisplayName(colorize(dName));

                List<String> coloredLore = new ArrayList<>();
                for (String loreLine : data.lore) {
                    String formattedLine = loreLine
                            .replace("%current_level%", String.valueOf(curLvl))
                            .replace("%max_level%", String.valueOf(data.maxLevel))
                            .replace("%current_value%", String.format("%.1f", curVal))
                            .replace("%next_value%", String.format("%.1f", nextVal));
                    coloredLore.add(colorize(formattedLine));
                }

                coloredLore.add("");
                if (curLvl > 0) {
                    coloredLore.add(colorize("&a✔ Owned &7(Leveling up)"));
                } else {
                    coloredLore.add(colorize("&e⚠ Consumes 1 Upgrade Slot"));
                }
                coloredLore.add(colorize("&8[Slots: " + (curLvl > 0 ? usedSlots : usedSlots + 1) + "/" + maxSlots + "]"));
                meta.setLore(coloredLore);

                if (data.id.startsWith("super_") || data.type.startsWith("SUPER_")) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                NamespacedKey key = new NamespacedKey(plugin, "upgrade_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.id);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Ngăn click vào GUI Preview
        if (title.contains("UPGRADE DIRECTORY")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                event.getWhoClicked().closeInventory();
            }
            return;
        }

        if (title.contains("Choose Upgrade") || title.contains("Chọn Nâng Cấp")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            NamespacedKey key = new NamespacedKey(plugin, "upgrade_id");
            String upgradeId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

            if (upgradeId != null) {
                if (upgradeId.equals("MAXED_OUT_HEAL")) {
                    double maxHp = 20.0;
                    AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (hpAttr != null) maxHp = hpAttr.getValue();
                    player.setHealth(Math.min(player.getHealth() + 4.0, maxHp));
                    player.sendMessage("§a[MobArenav2] §eYou are fully upgraded! Healed 2 hearts.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    applyUpgrade(player, upgradeId);

                    // --- BÁO CÁO QoL: HIỂN THỊ DANH SÁCH UPGRADE ---
                    Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
                    if (arena != null) {
                        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
                        String playerClass = profile.getSelectedClass() != null ? profile.getSelectedClass() : "None";
                        int maxSlots = plugin.getUpgradeManager().getMaxSlots(player, playerClass);
                        int usedSlots = plugin.getUpgradeManager().getUsedSlots(profile);
                        Map<String, Integer> owned = profile.getOwnedUpgrades();

                        player.sendMessage("§8§m----------------------------------");
                        player.sendMessage("§a§l✔ UPGRADE SELECTED: §f" + event.getCurrentItem().getItemMeta().getDisplayName());
                        player.sendMessage("§fClass: §b" + playerClass + " §7(Slots Used: §e" + usedSlots + "§7/§e" + maxSlots + "§7)");
                        player.sendMessage(" ");
                        player.sendMessage("§6§lCURRENT BUFFS:");
                        for (Map.Entry<String, Integer> entry : owned.entrySet()) {
                            UpgradeData uData = plugin.getUpgradeManager().getUpgradeData(entry.getKey());
                            String name = (uData != null) ? uData.displayName : entry.getKey();
                            player.sendMessage(" §7• " + colorize(name) + " §8[Lv." + entry.getValue() + "]");
                        }
                        player.sendMessage("§8§m----------------------------------");
                    }

                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                }
                player.closeInventory();
            }
        }
    }

    private void applyUpgrade(Player player, String upgradeId) {
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;
        ArenaPlayerProfile profile = arena.getPlayerProfiles().get(player.getUniqueId());
        if (profile == null) return;

        UpgradeData targetData = plugin.getUpgradeManager().getUpgradeData(upgradeId);
        if (targetData == null) return;

        profile.addUpgradeLevel(upgradeId);
        int newLevel = profile.getUpgradeLevel(upgradeId);

        double valueToAdd = (newLevel == 1) ? targetData.baseValue : targetData.increment;

        switch (targetData.type) {
            case "HEAL":
                AttributeInstance hpAttrHeal = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHpHeal = (hpAttrHeal != null) ? hpAttrHeal.getValue() : 20.0;
                player.setHealth(Math.min(player.getHealth() + valueToAdd, maxHpHeal));
                break;

            case "MAX_HEALTH":
                AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (hpAttr != null) {
                    hpAttr.setBaseValue(hpAttr.getBaseValue() + valueToAdd);
                    player.setHealth(Math.min(player.getHealth() + valueToAdd, hpAttr.getValue()));
                }
                break;

            case "DAMAGE_MULTIPLIER":
                AttributeInstance dmgAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (dmgAttr != null) {
                    dmgAttr.setBaseValue(dmgAttr.getBaseValue() + valueToAdd);
                }
                break;

            case "POTION_EFFECT":
                NamespacedKey pKey = NamespacedKey.minecraft(targetData.effectType.toLowerCase());
                PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(pKey);
                if (pType != null) {
                    player.addPotionEffect(new PotionEffect(pType, targetData.duration, targetData.amplifier));
                }
                break;

            case "COMMAND":
                if (targetData.command != null && !targetData.command.isEmpty()) {
                    String cmd = targetData.command.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                break;

            case "RANDOM_EVENT":
                if (targetData.eventsSection != null) {
                    double roll = ThreadLocalRandom.current().nextDouble(100.0);
                    double currentSum = 0;
                    for (String eventKey : targetData.eventsSection.getKeys(false)) {
                        ConfigurationSection ev = targetData.eventsSection.getConfigurationSection(eventKey);
                        if (ev == null) continue;

                        currentSum += ev.getDouble("chance", 0.0);
                        if (roll <= currentSum) {
                            if (ev.contains("command")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ev.getString("command").replace("%player%", player.getName()));
                            }
                            if (ev.contains("true-damage")) {
                                double newHp = player.getHealth() - ev.getDouble("true-damage");
                                player.setHealth(Math.max(0, newHp));
                                player.damage(0.1);
                            }
                            if (ev.contains("message")) {
                                player.sendMessage(colorize(ev.getString("message")));
                            }
                            break;
                        }
                    }
                }
                break;
        }
    }
}