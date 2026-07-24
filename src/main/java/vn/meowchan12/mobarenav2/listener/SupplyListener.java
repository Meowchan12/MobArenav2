package vn.meowchan12.mobarenav2.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import vn.meowchan12.mobarenav2.MobArenav2;

public class SupplyListener implements Listener {

    private final MobArenav2 plugin;

    public SupplyListener(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.startsWith("Editing Supply: ")) {
            String supplyId = title.replace("Editing Supply: ", "").trim();

            plugin.getSupplyManager().saveSupplyFromGUI(supplyId, event.getInventory());
            player.sendMessage("§a[MobArenav2] Successfully saved items for Supply: §e" + supplyId);
        }
    }
}