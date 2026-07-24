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
