<div align="center">

# HClient
**Fabric mod for Minecraft 1.21.1** — custom title screen, server list with anticheat ratings, and client utilities built on the OyVey base.

---

## Features

### Multiplayer / Server List
- Fully custom server list UI with server **cards**
- **Anticheat rating** for 100+ known servers, bundled in the mod:
  - 🟢 **Seguro** — low detection risk
  - 🟡 **Cuidado** — moderate detection risk  
  - 🔴 **Peligro** — high detection risk (strong anticheat)
  - 🔵 **Sin Analizar** — server not in database
- **Warning screen** before connecting to Peligro or Sin Analizar servers — 5-second cooldown, dismissible permanently
- 
### Client utilities
- Module system (HUD modules, combat, misc, player)
- Watermark, coordinates HUD, and more
- Special Cobblemon Utilyties

---

## Installation
1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1
2. Drop the `HClient-*.jar` into your `mods/` folder
3. Launch the game

### Optional — Custom panoramas
Place cubemap folders inside `.minecraft/hclient/panoramas/`:
```
.minecraft/
  hclient/
    panoramas/
      my_panorama/
        panorama_0.png
        panorama_1.png
        panorama_2.png
        panorama_3.png
        panorama_4.png
        panorama_5.png
```

---

## Building
```bash
./gradlew build
```
Output JAR: `build/libs/HClient-*.jar`

---

</div>
