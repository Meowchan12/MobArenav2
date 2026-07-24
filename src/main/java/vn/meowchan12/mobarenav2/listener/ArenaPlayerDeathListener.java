package vn.meowchan12.mobarenav2.listener;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;

import java.util.List;

public class ArenaPlayerDeathListener implements Listener {

    private final MobArenav2 plugin;
    private final NamespacedKey bossKey;

    public ArenaPlayerDeathListener(MobArenav2 plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "arena_boss_id");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena != null && arena.isRunning() && !arena.getSpectators().contains(player.getUniqueId())) {

            if (player.getHealth() - event.getFinalDamage() <= 0) {

                // --- TOTEM & DEATH MARK LOGIC ---
                NamespacedKey dmKey = new NamespacedKey(plugin, "ma_death_mark");
                boolean hasDeathMark = false;

                if (player.getPersistentDataContainer().has(dmKey, PersistentDataType.LONG)) {
                    long expiry = player.getPersistentDataContainer().get(dmKey, PersistentDataType.LONG);
                    if (System.currentTimeMillis() <= expiry) {
                        hasDeathMark = true;
                    } else {
                        player.getPersistentDataContainer().remove(dmKey);
                    }
                }

                boolean hasTotem = false;
                if (player.getInventory().getItemInMainHand().getType() == org.bukkit.Material.TOTEM_OF_UNDYING ||
                        player.getInventory().getItemInOffHand().getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
                    hasTotem = true;
                }

                if (hasTotem && !hasDeathMark) {
                    return;
                }

                if (hasTotem && hasDeathMark) {
                    player.sendMessage("§4☠ The Death Mark has shattered your Totem! Goodbye!");
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.5f);
                }

                // --- ARENA FAKE DEATH HANDLING ---
                event.setCancelled(true);
                player.getPersistentDataContainer().remove(dmKey);

                // FIX: Hàm này đã bao gồm việc dịch chuyển vào Spectator Spawn rồi, không được gọi Teleport thêm lần nữa
                plugin.getArenaManager().handlePlayerDeath(player, arena);

                // FIX "Cascading Bug": CHỈ hiện bảng hỏi YES/NO nếu người chơi chết KHÔNG làm kết thúc Arena!
                // Nếu đây là người cuối cùng chết, arena.isRunning() sẽ là false vì trận đấu vừa bị hủy bỏ.
                if (arena.isRunning() && arena.getSpectators().contains(player.getUniqueId())) {
                    TextComponent question = new TextComponent("§eDo you want to leave the arena? ");

                    TextComponent yesBtn = new TextComponent("§a§l[YES]");
                    yesBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ma leave"));
                    yesBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to safely leave the arena")));

                    TextComponent space = new TextComponent(" §7/ ");

                    TextComponent noBtn = new TextComponent("§c§l[NO]");
                    noBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§cClick to continue spectating")));

                    question.addExtra(yesBtn);
                    question.addExtra(space);
                    question.addExtra(noBtn);

                    player.spigot().sendMessage(question);
                }
            }
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        org.bukkit.entity.LivingEntity entity = event.getEntity();

        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            if (arena.isRunning() && arena.getActiveMobs().contains(entity)) {

                boolean allowDrop = arena.getConfig().getBoolean("settings.mob-drop",
                        arena.getConfig().getBoolean("settings.rules.mob-drop", false));

                if (!allowDrop) {
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                }

                if (entity.getKiller() != null) {
                    ArenaPlayerProfile profile = arena.getPlayerProfiles().get(entity.getKiller().getUniqueId());
                    if (profile != null) {
                        profile.addKill();
                    }
                }

                if (entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {
                    String bossId = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
                    ConfigurationSection bossConfig = plugin.getBossManager().getBossesConfig().getConfigurationSection(bossId);

                    if (bossConfig != null && bossConfig.contains("loot-supply")) {
                        String supplyId = bossConfig.getString("loot-supply");
                        List<ItemStack> loots = plugin.getSupplyManager().getSupplyItems(supplyId);

                        Location deathLoc = entity.getLocation();

                        deathLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLoc, 100, 1.0, 1.0, 1.0, 0.5);
                        deathLoc.getWorld().playSound(deathLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);

                        for (ItemStack item : loots) {
                            if (item != null) {
                                deathLoc.getWorld().dropItemNaturally(deathLoc, item);
                            }
                        }
                    }
                }
                return;
            }
        }
    }
}