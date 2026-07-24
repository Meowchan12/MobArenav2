# 📚 MobArenav2 Official Wiki

Welcome to the official documentation for **MobArenav2**! This wiki contains everything you need to know about setting up, configuring, and mastering the plugin.

---

## 📖 Chapter 0: Foreword & Credits

### The Vision
MobArenav2 is not just a simple minigame; it is a full-scale **Roguelike Campaign Framework** for Minecraft. It is designed to give server owners the power to create deep, engaging PvE (Player vs Environment) experiences. With features like custom classes, smart wave progression, random roguelike upgrades, and map-unlocking campaigns, MobArenav2 turns your server into an RPG adventure.

### Inspiration & Credits
This plugin is heavily inspired by the legendary **MobArena** created by **garbagemule**. Their original work brought endless joy to the Minecraft community and set the golden standard for arena plugins. MobArenav2 was built as a tribute to that legacy, modernizing the concept with next-generation mechanics and highly customizable backend systems.

---

## 🏛️ Chapter 1: Architecture Overview

To master MobArenav2, you first need to understand how its files are organized and how a match actually works from start to finish.

### 1.1 The File Ecosystem
When you install MobArenav2, it generates a clean folder structure. Here is what each file does:

*   **`settings.yml`**: The main configuration file for global plugin settings.
*   **`classes.yml`**: Where you create combat classes (e.g., Knight, Archer, Juggernaut) with custom items, armor, potion effects, and arena restrictions.
*   **`upgrades.yml`**: The roguelike upgrade system. You define buffs (Normal and Super Upgrades) that players can choose during a match.
*   **`bosses.yml`**: Where you design custom bosses (health, speed, equipment, names).
*   **`bossskill.yml`**: Where you define the special abilities and magic spells for your custom bosses.
*   **`supply.yml`**: Configures the loot tables for "Supply Drops" (air drops) that spawn mid-game.
*   **`arenas/` (Folder)**: Contains a `.yml` file for every single arena you create (e.g., `stage1.yml`, `stage2.yml`). This is where you configure waves, map coordinates, entry fees, and arena locks.
*   **`userdata/` (Folder)**: The database. It stores lifetime stats (kills, wins) and unlocked arenas for every player based on their UUID.

### 1.2 The Match Lifecycle (How a game flows)
Every time a player plays MobArenav2, the plugin follows a strict, secure sequence:

1.  **The Join Request:** A player types `/ma join <arena>`. The system checks if the arena exists, if it is full, and if the player has unlocked it (Campaign Lock).
2.  **Inventory Security:** Before teleporting, the `InventoryBackupManager` safely saves the player's survival items to a file and clears their inventory. This ensures no items are ever lost.
3.  **Lobby & Loadout:** The player is teleported to the Lobby. They type `/ma class` to open the Class GUI. Some classes might be blocked depending on the current map.
4.  **Ready Phase:** Players type `/ma ready`. Once enough players are ready, the match begins.
5.  **The Fight:** Players are teleported into the arena. The `WaveManager` starts spawning normal mobs, custom bosses, and supply drops based on the arena's config. 
6.  **Mid-Game Upgrades:** During the fight, players get the chance to pick Roguelike Upgrades to become stronger.
7.  **The End Game:** 
    *   *Victory:* If players survive all waves, they get rewards, their stats increase, and they unlock the next map.
    *   *Defeat:* If all players die, the match ends immediately.
8.  **Restoration:** Players are teleported back to their original location. Their survival inventory is 100% restored. The `ArenaRestorer` automatically repairs any blocks destroyed by mobs (like Creepers) during the fight.

## ⚙️ Chapter 2: Commands & Permissions

MobArenav2 comes with a complete set of commands for both players and server administrators. Every command starts with the prefix `/ma` or `/mobarenav2`.

### 2.1 Player Commands
These commands are used by normal players to interact with the arenas.

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/ma list` | View a list of all available arenas and their status. | `mobarenav2.player.list` |
| `/ma join <arena>` | Join an arena. | `mobarenav2.player.join` |
| `/ma leave` | Leave the current arena and get your items back. | `mobarenav2.player.leave` |
| `/ma class` | Open the Class selection menu in the lobby. | *(None, requires being in an arena)* |
| `/ma ready` | Mark yourself as ready to start the match. | *(None, requires being in an arena)* |
| `/ma upgrades` | Open the Upgrade Directory to preview all available roguelike buffs. | *(None)* |
| `/ma spec <arena>` | Join an arena as an invisible spectator. | `mobarenav2.player.spec` |
| `/ma top <arena> [page]`| View the match summary and MVP leaderboard of an arena. | *(None)* |

### 2.2 Admin Commands
These commands are strictly for server owners and administrators to build and manage arenas.

* **General Admin Permission:** `mobarenav2.admin` (Required for all commands below).

| Command | Description |
| :--- | :--- |
| `/ma setup <arena>` | Enter or exit visual setup mode to build a new arena. |
| `/ma setp1` / `/ma setp2`| Set the two corners of the arena region. |
| `/ma setspectator <arena>`| Set the spawn point for spectators. |
| `/ma save <arena>` | Save the arena coordinates to the config file. |
| `/ma savearena <arena>`| Create a block backup of the arena (used for auto-repair). |
| `/ma tp <arena>` | Teleport instantly to an arena's lobby or spawn point. |
| `/ma supply <action> <id>`| Create, edit, delete, or reset Supply Drops. |
| `/ma inv <player>` | Open the secure GUI to view and manage a player's backup inventory. |
| `/ma reload` | Safely hot-reload all plugin configuration files without restarting the server. |

### 2.3 Special Permissions
Apart from command permissions, MobArenav2 has some hidden permissions for VIPs or Admins:
* `mobarenav2.bypass.arenalock`: Allows a player to join any locked arena without clearing the previous stages.
* `mobarenav2.upgradeslot.<number>`: Grants a player a specific amount of maximum upgrade slots, overriding their class limit. (Example: `mobarenav2.upgradeslot.5` gives 5 slots).

---

## 🏟️ Chapter 3: Arena Configuration (The Basics)

Every arena in MobArenav2 is controlled by its own configuration file located in the `plugins/MobArenav2/arenas/` folder. When you create a new arena, it automatically copies the `default.yml` template.

Let's break down the **General Settings** (`settings` section) of an arena file.

### 3.1 Player Limits & Economy
You can control how many players are allowed in the arena and if it costs money to enter.

```yaml
settings:
  enabled: true
  
  # How many players are needed to start, and what is the maximum limit?
  player-limits:
    min-players: 1
    max-players: 4

  # The final wave of the normal game before Endless Mode starts
  max-waves: 20

  # Do players need to pay to enter? (Requires Vault plugin)
  entry-fee:
    enabled: true
    type: VAULT
    amount: 100.0
```
### 3.2 Arena Rules
You can strictly control what players can or cannot do while fighting in the arena.

```yaml
rules:
    mob-drop: false          # Should mobs drop normal vanilla items?
    player-pvp: false        # Can players hit each other? (Friendly fire)
    block-break: false       # Can players break map blocks?
    block-place: false       # Can players place blocks?
    item-drop: true          # Can players drop items from their inventory?
    allow-commands: false    # Can players use outside commands (like /spawn)?
    command-whitelist:       # Commands allowed during the match
      - "/ma leave"
      - "/ma class"
```

### 3.3 The Announcer (MVP System)
When a match ends, the plugin calculates the top 3 players with the most kills (MVPs) and broadcasts a message. You can fully customize this message!

Useful Placeholders:

`%arena%`: The name of the arena.

`%wave%`: The highest wave reached.

`%top1_player%`, `%top1_kills%`, `%top1_class%`: Details of the 1st place MVP (Works for top2 and top3 as well).

```yaml
announcer:
    announce-when-arena-ends: true
    messages:
      victory:
        - "&a&lVICTORY! &fArena &e%arena% &fhas been cleared!"
        - "&e&lTOP MVPs:"
        - " &61. &f%top1_player% &7- &c%top1_kills% Kills &8(%top1_class%)"
      defeat:
        - "&c&lMATCH OVER! &fYour team died in Arena &e%arena%"
        - "&7Reached Wave: &b%wave%"
```

### 3.4 Hardcore Mechanics (Lighting Strike)
Want to make the game harder? You can enable the lightning-strike event. This will randomly target players with a deadly red circle that locks on and strikes them with true damage if they don't run away in time!

```yaml
hardcore-mechanics:
    lightning-strike:
      enabled: true
      interval: 15       # Triggers every 15 seconds
      strikes-per-wave:
        min: 1
        max: 3           # Spawns 1 to 3 strikes at the same time
      damage:
        min: 5.0
        max: 15.0        # True damage dealt (bypasses armor)
        radius: 4.0      # Explosion size
```

## 📊 Chapter 4: PlaceholderAPI Integration

MobArenav2 natively supports **PlaceholderAPI**[cite: 16]. You can use these placeholders in other plugins like TAB, DecentHolograms, DeluxeMenus, or any scoreboard plugin to display real-time data. All placeholders start with the identifier `mobarenav2_`[cite: 16].

### 4.1 Global Arena Stats
Use these to display real-time information about specific arenas (replace `<arenaName>` with the exact name of your arena)[cite: 16].

*   `%mobarenav2_arena_status_<arenaName>%`: Current status of the arena (e.g., In Progress, Waiting, Disabled)[cite: 16].
*   `%mobarenav2_arena_players_<arenaName>%`: Number of active players inside (e.g., 3, 0)[cite: 16].
*   `%mobarenav2_arena_wave_<arenaName>%`: The current wave the arena is on[cite: 16].
*   `%mobarenav2_total_kills_<arenaName>%`: Total combined kills of all players in that arena[cite: 16].

### 4.2 Lifetime Stats (Player Specific)
Use these to display a player's permanent account progression and leaderboard stats[cite: 16].

*   `%mobarenav2_lifetime_kills%`: Total lifetime kills across all matches[cite: 16].
*   `%mobarenav2_games_played%`: Total number of matches played[cite: 16].
*   `%mobarenav2_games_won%`: Total number of arenas successfully cleared (Victory)[cite: 16].
*   `%mobarenav2_highest_wave%`: The absolute highest wave the player has ever reached[cite: 16].

### 4.3 In-Game State (Player Specific)
Use these on an in-game scoreboard to show the player's current match data[cite: 16].

*   `%mobarenav2_arena%`: The name of the arena the player is in[cite: 16].
*   `%mobarenav2_player_state%`: The player's combat state (e.g., Lobby, Alive, Spectator, Waiting)[cite: 16].
*   `%mobarenav2_player_is_ready%`: Ready status in the lobby (Ready or Not Ready)[cite: 16].
*   `%mobarenav2_class%`: The class the player has selected[cite: 16].
*   `%mobarenav2_kills%`: Player's total kills in the current match[cite: 16].
*   `%mobarenav2_current_mobs%`: Total active mobs currently alive in the arena[cite: 16].

### 4.4 In-Game Live Leaderboard
Use these to create a live top-killer leaderboard on your scoreboard (replace `<rank>` with a number like 1, 2, or 3)[cite: 16].

*   `%mobarenav2_top_player_<rank>%`: Name of the player at the specific rank[cite: 16].
*   `%mobarenav2_top_kills_<rank>%`: Kills of the player at the specific rank[cite: 16].
*   `%mobarenav2_top_status_<rank>%`: Life status of the player at the specific rank (Alive or Dead)[cite: 16].

## 🗺️ Chapter 5: Campaign Progression (Arena Locks)

One of the most powerful features of MobArenav2 is the ability to create a **Story Mode** or a **Campaign**[cite: 10]. Instead of letting players join any arena they want, you can force them to clear arenas in a specific order.

### 5.1 How the Lock System Works
When a team successfully defeats all waves in an arena, the plugin records a "Victory" for every surviving player. This achievement is permanently saved in their personal data file (`userdata/<UUID>.yml`).

If an arena is "locked," players cannot join it until they have the Victory achievement from the required previous arena.

### 5.2 Configuring the Arena Lock
To lock an arena, open its configuration file (e.g., `arenas/stage2.yml`) and find the `settings` section[cite: 10]. Look for the `required-arena` option[cite: 10].

*   **Unlocked Arena:** Leave it blank (`""`) if everyone can join freely[cite: 10].
*   **Locked Arena:** Type the exact name of the previous arena that must be cleared first.

**Example Configuration:**
```yaml
# Inside arenas/stage2.yml
settings:
  enabled: true
  required-arena: "stage1"  # Players MUST beat stage1 to enter stage2
  player-limits:
    min-players: 1
    max-players: 4
```

*Note: Admins with the `mobarenav2.bypass.arenalock` permission can bypass this system and join any arena for testing purposes.*

---

## ⚔️ Chapter 6: Combat Classes

The `classes.yml` file is where you define the different playstyles available to players. You can create tanks, healers, snipers, or mages by giving them specific equipment and potion effects[cite: 9].

### 6.1 Creating a Class
Each class has a unique name (e.g., Knight, Archer, Alchemist, Juggernaut) and a set of properties[cite: 9]. The plugin uses a smart item parser to read your configuration.

Here is what you can configure for each class:
*   **limit:** The maximum number of players who can pick this class per match[cite: 9]. Set to `-1` for unlimited slots, or `1` for a rare/overpowered class[cite: 9].
*   **allowed-arenas:** A list of arenas where this class can be used[cite: 9]. If you omit this, the class can be used in all arenas[cite: 9].
*   **items:** Weapons, food, and utilities[cite: 9]. Format: `material:amount enchant_name:level`. Separate multiple enchantments with a semicolon (`;`) and multiple items with a comma (`,`)[cite: 9].
*   **armor:** Helmet, Chestplate, Leggings, and Boots[cite: 9]. Format is the same as items.
*   **effects:** Permanent potion effects granted to the player[cite: 9]. Format: `effect_name:amplifier` (Remember that Level 0 in code means Tier 1 in-game)[cite: 9].

### 6.2 Example Class Configuration
Here is an example of a perfectly balanced setup featuring an unlimited Knight, a limited Alchemist (Healer), and a map-restricted Juggernaut (Tank)[cite: 9].

```yaml
classes:
  Knight:
    limit: -1 # Unlimited slots
    items: "netherite_sword:1 sharpness:5;unbreaking:3;fire_aspect:2;mending:1, golden_apple:16, cooked_beef:64"
    armor: "netherite_helmet:1 protection:4, netherite_chestplate:1 protection:4;thorns:3, netherite_leggings:1 protection:4, netherite_boots:1 protection:4;feather_falling:4"
    effects: "health_boost:4, resistance:0"

  Alchemist:
    limit: 2 # Limited to 2 support players per arena
    items: "golden_sword:1 smite:5;looting:3, splash_potion:32 instant_damage:2, splash_potion:16 instant_heal:2, golden_apple:32"
    armor: "golden_helmet:1 protection:4, golden_chestplate:1 protection:4, golden_leggings:1 protection:4, golden_boots:1 protection:4"
    effects: "regeneration:0, speed:0"

  Juggernaut:
    limit: 1 # Only 1 player can select this ultimate tank class
    allowed-arenas: 
      - "stage2"
      - "stage3"
      - "boss_room"
    items: "netherite_axe:1 sharpness:6;smite:3;unbreaking:5, shield:1 unbreaking:5, enchanted_golden_apple:3, cooked_beef:64"
    armor: "netherite_helmet:1 protection:5, netherite_chestplate:1 protection:5;projectile_protection:4, netherite_leggings:1 protection:5, netherite_boots:1 protection:5;blast_protection:4"
    effects: "slowness:1, resistance:2, health_boost:9" 
```

### 6.3 Map Restrictions (`allowed-arenas`)
In the example above, the **Juggernaut** class is extremely bulky. By adding `allowed-arenas`, we prevent players from using this class in small, parkour-based arenas (like `stage1`)[cite: 9]. 

If a player tries to select the Juggernaut in `stage1`, the class icon will turn into a red Barrier block marked as **[RESTRICTED]** in their GUI.

## 🌊 Chapter 7: The Smart Wave System

The Smart Wave System in MobArenav2 is designed to give you absolute control over how and when enemies spawn. Instead of hardcoding every single wave, you define "Wave Profiles" with specific conditions, priorities, and modifiers.

This configuration is located in **Area 4** of your arena's `.yml` file (e.g., `arenas/stage1.yml`).

### 7.1 Wave Spawning Syntax
You can tell the plugin exactly when a wave profile should trigger using flexible conditions:
*   **Range:** `waves: 1-9` (This profile will spawn randomly between wave 1 and wave 9).
*   **Exact Match:** `waves: 3, 7` (This profile will ONLY spawn on wave 3 and wave 7).
*   **Endless/Infinite:** `waves: 21+` (This profile will spawn from wave 21 until the players die).

### 7.2 Priority System
When multiple wave profiles overlap (e.g., a boss is set to spawn on wave 10, but a normal mob profile is set to `1-10`), the plugin uses the `priority` setting to resolve the conflict.
*   **Priority 1:** The absolute highest. Use this for Bosses and Supply Drops. It overrides everything else.
*   **Priority 5:** The lowest (Default for standard mobs).
*   *Note: If two profiles have the exact same priority, the plugin will pick one randomly.*

### 7.3 Wave Types & Modifiers
You can change the behavior of the wave using `wave_types` and apply buffs to the monsters to make them scale up as the game progresses.

*   `mob`: Standard enemy spawning.
*   `swarm`: Spawns massive hordes (use alongside `mob-multiplier: 2.0` to double the mob count).
*   `boss`: Summons a custom entity from `bosses.yml`.
*   `supply`: Spawns an airdrop chest instead of monsters (requires `supply-id` and `duration`).

**Example: A Mid-Game Veteran Wave (Waves 11-20)**
```yaml
  'veteran_mobs':
    waves: 11-20
    priority: 3
    wave_types: mob
    maxhp-bonus: "x1.5"        # Multiplies all mobs' health by 1.5
    damage-bonus: "+2.0"       # Adds 2.0 flat damage to their attacks
    general-effect: "resistance:1" # Gives them Resistance II
    mobs:
      husk: 6
      stray: 4
      creeper: 2
```

---

## 🃏 Chapter 8: Roguelike Upgrades System

To make every match unique and highly replayable, MobArenav2 features a Roguelike Upgrade System. At specific intervals, players are presented with a GUI to choose powerful buffs. 

This is configured globally in `upgrades.yml`.

### 8.1 Upgrade Settings & Class Limits
In the `settings` section of `upgrades.yml`, you define how often the upgrade menu appears and how many upgrade slots each class is allowed to hold.

```yaml
settings:
  enabled: true
  trigger-every: 5       # The menu opens every 5 waves (Wave 5, 10, 15...)
  default-max-slots: 3   # Default maximum active upgrades a player can hold
  class-slots:           # Override slots for specific classes
    Knight: 3
    Archer: 3
    Alchemist: 4         # Supports get more utility slots!
    Juggernaut: 2        # Tanks are restricted to fewer slots
```

### 8.2 Creating a Normal Upgrade
Upgrades rely on probability (Weight) and level scaling (Base Value + Increment). You can also lock the upgrade to a specific GUI slot to keep your interface clean and organized.

*   **weight:** The higher the weight, the more likely this upgrade will appear in the random selection pool.
*   **slot:** The exact inventory slot (0-53) where this item will appear in the Upgrade GUI.
*   **base-value:** The stat granted at Level 1.
*   **increment:** The stat added for every subsequent level.

**Example: Critical Strike Upgrade**
```yaml
  critical_strike:
    display-name: '&#FFAA00&lCritical Strike &8[Lv.%current_level%/%max_level%]'
    material: GOLDEN_SWORD
    weight: 35
    slot: 2
    type: CRITICAL_CHANCE
    max-level: 5
    base-value: 10.0     # Level 1 gives 10% critical chance
    increment: 5.0       # Each level adds 5% (Max Level 5 = 30% chance)
    lore:
      - '&7&m---------------------'
      - '&fGrants a chance to deal &c&lDOUBLE&f damage!'
      - '&fCurrent Chance: &c%current_value%%'
      - '&fNext Level: &c%next_value%%'
      - '&7&m---------------------'
```

### 8.3 Legendary Super Upgrades
Super Upgrades are ultra-rare, game-changing mechanics. They usually have a maximum level of 1 and a very low weight. We recommend placing them at the bottom of the GUI (e.g., slots 38-42) to make them stand out.

*   `SUPER_EXECUTE`: Instantly kills non-boss mobs below a certain health threshold and causes an AoE explosion.
*   `SUPER_REVIVE`: Prevents fatal damage ONCE per match, granting brief invulnerability and knocking back enemies.
*   `SUPER_OVERCLOCK`: Completely removes the 1.9+ weapon attack cooldown for maximum swing speed.
*   `SUPER_AUTO_STRIKE`: Automatically drops an orbital strike on the densest group of mobs every X seconds.

**Example: Aegis of Immortality (Revive)**
```yaml
  super_aegis:
    display-name: '&#FFAA00&l★ SUPER: Aegis of Immortality ★'
    material: NETHER_STAR
    weight: 2            # Extremely rare!
    slot: 39             # Centered at the bottom of the GUI
    type: SUPER_REVIVE
    max-level: 1
    lore:
      - '&7&m---------------------'
      - '&e&lLEGENDARY SUPER ITEM!'
      - '&fPrevents fatal damage &aONCE&f per match.'
      - '&7&m---------------------'
```

## 🐉 Chapter 9: Custom Bosses & Skills

A great PvE campaign needs epic boss battles. MobArenav2 allows you to design custom bosses with unique equipment, stats, and magic abilities using `bosses.yml` and `bossskill.yml`.

### 9.1 Designing a Boss (`bosses.yml`)
To create a boss, you assign a unique ID and configure its base properties. 

*   **entity:** The base Minecraft entity type (e.g., ZOMBIE, SKELETON, WITHER_SKELETON).
*   **display-name:** The boss's name tag (supports color codes).
*   **health:** Total hitpoints. The boss will automatically receive a boss bar at the top of the screen.
*   **speed-multiplier:** Makes the boss faster or slower than normal.
*   **equipment:** Armor and weapons (Syntax: `material:amount enchant:level`).
*   **skills:** A list of skill IDs from `bossskill.yml` that this boss can cast.

**Example: The Abyssal Warlord**
```yaml
# Inside bosses.yml
bosses:
  abyssal_warlord:
    entity: WITHER_SKELETON
    display-name: "&4&lAbyssal Warlord"
    health: 1500.0
    speed-multiplier: 1.2
    equipment:
      helmet: "netherite_helmet:1 protection:4"
      chestplate: "netherite_chestplate:1 protection:4"
      leggings: "netherite_leggings:1 protection:4"
      boots: "netherite_boots:1 protection:4"
      main_hand: "netherite_axe:1 sharpness:5;fire_aspect:2"
    skills:
      - "meteor_shower"
      - "summon_minions"
```

### 9.2 Creating Boss Skills (`bossskill.yml`)
Skills make your bosses threatening. A skill activates based on a trigger condition and executes an action.

*   **trigger:** When does the skill happen?
    *   `interval`: Casts every X seconds.
    *   `on_health_drop`: Casts when the boss drops below a certain health percentage (e.g., 50%).
    *   `on_death`: Casts a final attack or summons upon dying.
*   **action:** What does the skill do?
    *   `AOE_DAMAGE`: Deals damage to players within a radius.
    *   `SUMMON`: Spawns standard mobs to protect the boss.
    *   `POTION_PULSE`: Gives negative effects (like Wither or Blindness) to nearby players.

**Example: Meteor Shower Skill**
```yaml
# Inside bossskill.yml
skills:
  meteor_shower:
    trigger:
      type: interval
      value: 20          # Casts every 20 seconds
    action:
      type: AOE_DAMAGE
      radius: 8.0
      damage: 15.0       # Deals 15 damage (7.5 hearts)
      visual: FLAME      # Spawns flame particles
      message: "&cThe Warlord calls down a rain of fire! Take cover!"
```

---

## 🎁 Chapter 10: Supplies & Reward System

To keep players motivated, MobArenav2 features a dynamic reward system and mid-game Supply Drops (Airdrops) to help them survive Endless Mode.

### 10.1 Supply Drops (`supply.yml`)
Supply drops are chests that spawn at pre-configured coordinates (`supplypoints` in your arena config) during specific waves. 

You define the loot table using percentages (`chance`).

**Example: Supply Drop Configuration**
```yaml
# Inside supply.yml
supplies:
  supply1:
    display-name: "&b&l★ Care Package ★"
    hologram: true
    items:
      - item: "golden_apple:4"
        chance: 100.0    # Guaranteed drop
      - item: "splash_potion:2 instant_heal:2"
        chance: 50.0     # 50% chance to spawn
      - item: "enchanted_golden_apple:1"
        chance: 5.0      # Ultra-rare drop
```

*To spawn this supply drop, assign it to a Priority 1 Wave in your arena's `waves` section using `wave_types: supply`.*

### 10.2 The Match Reward System
Players earn permanent rewards based on how many waves they clear. You configure this in **Area 3** of the arena config file (`arenas/<map>.yml`).

**Wave Syntax Keys:**
*   `'X'`: Gives the reward exactly when Wave X is cleared (e.g., `'10'`).
*   `'every X normal'`: Gives the reward on recurring waves during the normal campaign (e.g., `'every 5 normal'`).
*   `'every X endless'`: Gives the reward on recurring waves during Endless Mode.

**Supported Reward Types:**
*   `vault:<amount>`: Direct money deposit (Requires Vault).
*   `cmd(<command>)`: Executes a console command. Use `%player%` to target the player.
*   `after-game: true`: Add this flag if you want the reward to be given *only* after the match ends, rather than instantly during combat.

**Example: Dynamic Economy Rewards**
```yaml
# Inside arenas/stage1.yml (Area 3)
rewards:
  'every 5 normal':
    - "vault:150.0"     # Players get $150 every 5 waves
  '10':
    - "vault:300.0"
    - "cmd(give %player% diamond 1)"  # Bonus diamond for beating wave 10
  'every 5 endless':
    - "after-game: true"              # Safely given after the match
    - "vault:500.0"
    - "cmd(eco give %player% 1000)"
```
