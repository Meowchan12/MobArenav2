package vn.meowchan12.mobarenav2.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.utils.ItemParser;

import java.util.List;

public class GUIListener implements Listener {

    private final MobArenav2 plugin;

    public GUIListener(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // ==========================================
        // 0. BẢO VỆ MENU PREVIEW UPGRADES QoL
        // ==========================================
        if (title.contains("UPGRADE DIRECTORY")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                player.closeInventory();
            }
            return;
        }

        // ==========================================
        // 1. CLASS SELECTION MENU (PRE-GAME)
        // ==========================================
        if (title.contains("Select your Class")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            if (clickedItem.getType() == Material.BARRIER) {
                player.sendMessage("§c[MobArenav2] This class is already full!");
                return;
            }

            NamespacedKey key = new NamespacedKey(plugin, "class_id");
            if (clickedItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String className = clickedItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                boolean success = plugin.getClassManager().equipClass(player, className);
                if (success) {
                    player.closeInventory();
                    player.sendMessage("§eType §a/ma ready §ewhen you are prepared to fight!");
                }
            }
        }

        // ==========================================
        // 2. MID-GAME UPGRADE SELECTION MENU
        // ==========================================
        else if (title.contains("Select your Upgrade")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            NamespacedKey key = new NamespacedKey(plugin, "upgrade_id");
            if (clickedItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String upgradeId = clickedItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                ConfigurationSection upgConfig = plugin.getConfigManager().getUpgrades().getConfigurationSection("upgrades." + upgradeId);
                if (upgConfig != null) {
                    List<String> actions = upgConfig.getStringList("actions");
                    for (String action : actions) {
                        if (action.startsWith("[EFFECT]")) {
                            String[] data = action.replace("[EFFECT]", "").trim().split(":");
                            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(data[0].toLowerCase()));
                            if (type != null) {
                                int amplifier = data.length > 1 ? Integer.parseInt(data[1]) : 0;
                                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false, true));
                            }
                        } else if (action.startsWith("[GIVE]")) {
                            String itemStr = action.replace("[GIVE]", "").trim();
                            ItemStack reward = ItemParser.parseItem(itemStr);
                            if (reward != null) {
                                player.getInventory().addItem(reward);
                            }
                        }
                    }
                    player.sendMessage("§a[MobArenav2] Upgrade applied successfully!");
                }
                player.closeInventory();
            }
        }

        // ==========================================
        // 3. ENDLESS MODE VOTING MENU
        // ==========================================
        else if (title.contains("Vote: Endless Mode?")) {
            event.setCancelled(true);

            Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
            if (arena == null || !arena.isVoting()) {
                player.closeInventory();
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            if (clickedItem.getType() == Material.LIME_WOOL) {
                plugin.getWaveManager().castVote(player, arena, true);
                player.closeInventory();
            } else if (clickedItem.getType() == Material.RED_WOOL) {
                plugin.getWaveManager().castVote(player, arena, false);
                player.closeInventory();
            }
        }
    }
}