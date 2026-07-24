package vn.meowchan12.mobarenav2.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.io.File;
import java.io.IOException;

public class BossManager {

    private final MobArenav2 plugin;

    private File bossesFile;
    private YamlConfiguration bossesConfig;

    private File skillsFile;
    private YamlConfiguration skillsConfig;

    public BossManager(MobArenav2 plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        bossesFile = new File(plugin.getDataFolder(), "bosses.yml");
        if (!bossesFile.exists()) {
            try {
                bossesFile.createNewFile();
                bossesConfig = YamlConfiguration.loadConfiguration(bossesFile);
                bossesConfig.set("zombie_king.Boss_displayname", "&#FF5555&lZombie King");
                bossesConfig.set("zombie_king.Bossbar", true);
                bossesConfig.set("zombie_king.nametagshow", true);
                bossesConfig.set("zombie_king.HP", 100.0);
                bossesConfig.set("zombie_king.Damage", 10.0);
                bossesConfig.set("zombie_king.effect-bonus", "speed:2, resistance:2");
                bossesConfig.save(bossesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[MobArenav2] Could not create bosses.yml!");
            }
        }
        bossesConfig = YamlConfiguration.loadConfiguration(bossesFile);

        skillsFile = new File(plugin.getDataFolder(), "bossskill.yml");
        if (!skillsFile.exists()) {
            try {
                skillsFile.createNewFile();
                skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);

                // --- OLD SKILL (RETAINED) ---
                skillsConfig.set("earthquake.type", "AREA_DAMAGE");
                skillsConfig.set("earthquake.radius", 5.0);
                skillsConfig.set("earthquake.damage", 5.0);

                // --- 9 NEW ADVANCED SKILLS ---
                skillsConfig.set("magnetic_pull.type", "MAGNETIC_PULL");
                skillsConfig.set("magnetic_pull.display-name", "&5&lMagnetic Pull");
                skillsConfig.set("magnetic_pull.radius", 12.0);
                skillsConfig.set("magnetic_pull.pull-strength", 1.5);
                skillsConfig.set("magnetic_pull.effects", java.util.Arrays.asList("BLINDNESS:40:1"));

                skillsConfig.set("earthquake_smash.type", "EARTHQUAKE_SMASH");
                skillsConfig.set("earthquake_smash.display-name", "&6&lEarthquake Smash");
                skillsConfig.set("earthquake_smash.radius", 8.0);
                skillsConfig.set("earthquake_smash.jump-velocity", 1.5);
                skillsConfig.set("earthquake_smash.knockup-power", 1.2);
                skillsConfig.set("earthquake_smash.damage", 10.0);

                skillsConfig.set("toxic_nova.type", "TOXIC_NOVA");
                skillsConfig.set("toxic_nova.display-name", "&a&lToxic Nova");
                skillsConfig.set("toxic_nova.max-radius", 10.0);
                skillsConfig.set("toxic_nova.expansion-speed", 0.5);
                skillsConfig.set("toxic_nova.damage-per-tick", 2.0);
                skillsConfig.set("toxic_nova.effects", java.util.Arrays.asList("WITHER:100:1", "SLOWNESS:100:2"));

                skillsConfig.set("minion_call.type", "MINION_CALL");
                skillsConfig.set("minion_call.display-name", "&c&lRise of Minions");
                skillsConfig.set("minion_call.mob-type", "VINDICATOR");
                skillsConfig.set("minion_call.amount", 4);
                skillsConfig.set("minion_call.lifespan", 15);

                skillsConfig.set("dash_and_ranged.type", "DASH_AND_RANGED");
                skillsConfig.set("dash_and_ranged.display-name", "&b&lSniper Dash");
                skillsConfig.set("dash_and_ranged.dash.power", 2.5);
                skillsConfig.set("dash_and_ranged.dash.invulnerable-duration", 3);
                skillsConfig.set("dash_and_ranged.ranged-stance.duration", 10);
                skillsConfig.set("dash_and_ranged.ranged-stance.max-target-distance", 20.0);
                skillsConfig.set("dash_and_ranged.ranged-stance.attack-interval", 1.0);

                skillsConfig.set("invis_teleporter.type", "INVIS_TELEPORTER");
                skillsConfig.set("invis_teleporter.display-name", "&8&lPhantom Assassin");
                skillsConfig.set("invis_teleporter.duration", 12);
                skillsConfig.set("invis_teleporter.teleport-interval", 3);
                skillsConfig.set("invis_teleporter.teleport-distance.min", 4.0);
                skillsConfig.set("invis_teleporter.teleport-distance.max", 8.0);

                skillsConfig.set("spammer_bow.type", "SPAMMER_BOW");
                skillsConfig.set("spammer_bow.display-name", "&c&lArrow Rain");
                skillsConfig.set("spammer_bow.duration", 6);
                skillsConfig.set("spammer_bow.attack-interval", 1.5);
                skillsConfig.set("spammer_bow.guaranteed-hit", true);
                skillsConfig.set("spammer_bow.knockback-force", 1.2);
                skillsConfig.set("spammer_bow.debuff.chance", 15.0);
                skillsConfig.set("spammer_bow.debuff.effects", java.util.Arrays.asList("POISON:80:1"));

                skillsConfig.set("saitama_punch.type", "SAITAMA_PUNCH");
                skillsConfig.set("saitama_punch.display-name", "&4&lSaitama Punch");
                skillsConfig.set("saitama_punch.duration", 5);
                skillsConfig.set("saitama_punch.true-damage", 5.0);

                skillsConfig.set("lifesteal_aura.type", "LIFESTEAL_AURA");
                skillsConfig.set("lifesteal_aura.display-name", "&4&lVampiric Touch");
                skillsConfig.set("lifesteal_aura.duration", 10);
                skillsConfig.set("lifesteal_aura.heal-percent.min", 1.0);
                skillsConfig.set("lifesteal_aura.heal-percent.max", 1.5);

                skillsConfig.save(skillsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[MobArenav2] Could not create bossskill.yml!");
            }
        }
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
    }

    // --- [MỚI] THUẬT TOÁN NẠP LẠI DỮ LIỆU ---
    public void reloadConfigs() {
        if (!bossesFile.exists() || !skillsFile.exists()) {
            loadConfigs();
            return;
        }
        bossesConfig = YamlConfiguration.loadConfiguration(bossesFile);
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
    }

    public void saveConfigs() {
        try {
            bossesConfig.save(bossesFile);
            skillsConfig.save(skillsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[MobArenav2] Could not save Boss configuration files!");
        }
    }

    public YamlConfiguration getBossesConfig() {
        return bossesConfig;
    }

    public YamlConfiguration getSkillsConfig() {
        return skillsConfig;
    }
}