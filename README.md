<div align="center">

# HClient
**Fabric mod for Minecraft 1.21.1** — custom title screen, server list with anticheat ratings, and client utilities built on the OyVey base.

---

## Features

### Title Screen
- Custom **HClient** logo with rainbow wave effect (Kaushan Script font rendered via Java2D)
- **RGB Particles** background mode — animated particle network with rainbow connections
- **Custom panorama** support — drop cubemap sets in `.minecraft/hclient/panoramas/<name>/` (6 faces: `panorama_0.png` → `panorama_5.png`)
- Background selection persists between sessions via `hclient/ui.json`
- Minecraft version + HClient version shown bottom-left

### Multiplayer / Server List
- Fully custom server list UI with server **cards**
- **Anticheat rating** for 100+ known servers, bundled in the mod:
  - 🟢 **Seguro** — low detection risk
  - 🟡 **Cuidado** — moderate detection risk  
  - 🔴 **Peligro** — high detection risk (strong anticheat)
  - 🔵 **Sin Analizar** — server not in database
- Ping displayed in colour (green ≤100 ms, yellow ≤200 ms, red >200 ms)
- **Warning screen** before connecting to Peligro or Sin Analizar servers — 5-second cooldown, dismissible permanently
- Custom Add / Edit server screens consistent with the overall theme

### Client utilities
- Module system (HUD modules, combat, misc, player)
- Watermark, coordinates HUD, and more

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

## Anticheat database
The rating list is at [`src/main/resources/anticheatlvl.txt`](src/main/resources/anticheatlvl.txt).  
Scale: **1 = Seguro · 2 = Cuidado · 3 = Peligro**  
Pull requests to add or correct entries are welcome.

</div>
