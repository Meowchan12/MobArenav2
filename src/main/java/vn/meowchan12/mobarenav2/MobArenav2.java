package vn.meowchan12.mobarenav2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.UpgradeGUI;
import vn.meowchan12.mobarenav2.command.ArenaCommand;
import vn.meowchan12.mobarenav2.listener.ArenaMobCombatListener;
import vn.meowchan12.mobarenav2.listener.ArenaPlayerDeathListener;
import vn.meowchan12.mobarenav2.listener.ArenaProtectionListener;
import vn.meowchan12.mobarenav2.listener.GUIListener;
import vn.meowchan12.mobarenav2.listener.MobAIListener;
import vn.meowchan12.mobarenav2.listener.PlayerConnectionListener;
import vn.meowchan12.mobarenav2.listener.SetupListener;
import vn.meowchan12.mobarenav2.listener.SupplyListener;
import vn.meowchan12.mobarenav2.manager.*;

public final class MobArenav2 extends JavaPlugin {

    private static MobArenav2 instance;
    private Economy econ = null;

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private SetupManager setupManager;
    private ClassManager classManager;
    private UpgradeManager upgradeManager;
    private InventoryBackupManager inventoryBackupManager;
    private UserDataManager userDataManager;
    private SupplyManager supplyManager;
    private WaveManager waveManager;
    private BossManager bossManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Khởi tạo Economy (Vault Hook)
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Tải toàn bộ cấu hình
        this.configManager = new ConfigManager(this);
        this.configManager.loadAllConfigs();

        // 3. Khởi tạo các Manager
        this.inventoryBackupManager = new InventoryBackupManager(this);
        this.userDataManager = new UserDataManager(this);
        this.classManager = new ClassManager(this);
        this.upgradeManager = new UpgradeManager(this);
        this.supplyManager = new SupplyManager(this);
        this.bossManager = new BossManager(this);
        this.waveManager = new WaveManager(this);
        this.arenaManager = new ArenaManager(this);
        this.setupManager = new SetupManager();

        // Nạp dữ liệu các Arena hiện có
        this.arenaManager.loadArenas();

        // 4. Đăng ký toàn bộ Event Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupListener(this, this.setupManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaPlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new SupplyListener(this), this);
        getServer().getPluginManager().registerEvents(new MobAIListener(this, this.arenaManager), this);
        getServer().getPluginManager().registerEvents(new ArenaMobCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradeGUI(this), this);

        // 5. Đăng ký Command chính
        if (getCommand("ma") != null) {
            getCommand("ma").setExecutor(new ArenaCommand(this, this.setupManager, this.arenaManager));
        }

        // --------------------------------------------------
        // 6. ĐĂNG KÝ PLACEHOLDERAPI EXPANSION
        // --------------------------------------------------
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ArenaExpansion(this).register();
            getLogger().info("[MobArenav2] Successfully hooked and registered PlaceholderAPI expansion!");
        } else {
            getLogger().warning("[MobArenav2] PlaceholderAPI not found! Fallback to standalone mode.");
        }

        // 7. Thông báo khởi động thành công
        getServer().getConsoleSender().sendMessage(Component.text("[MobArenav2] Successfully enabled - Ready for Battle!").color(NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        // Đảm bảo dọn dẹp và kết thúc an toàn mọi Arena đang chạy dở khi server tắt hoặc reload
        for (Arena arena : arenaManager.getAllArenas()) {
            if (arena.isRunning()) {
                arena.endArena(this);
            }
        }
        getServer().getConsoleSender().sendMessage(Component.text("[MobArenav2] Safely disabled and all arenas cleared.").color(NamedTextColor.RED));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // ==========================================
    // GETTERS CHO CÁC MANAGER
    // ==========================================
    public static MobArenav2 getInstance() { return instance; }
    public Economy getEconomy() { return econ; }
    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public SetupManager getSetupManager() { return setupManager; }
    public ClassManager getClassManager() { return classManager; }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public InventoryBackupManager getInventoryBackupManager() { return inventoryBackupManager; }
    public UserDataManager getUserDataManager() { return userDataManager; }
    public SupplyManager getSupplyManager() { return supplyManager; }
    public WaveManager getWaveManager() { return waveManager; }
    public BossManager getBossManager() { return bossManager; }
}