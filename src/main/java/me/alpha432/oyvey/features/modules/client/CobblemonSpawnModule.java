package me.alpha432.oyvey.features.modules.client;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.network.ContainerOpenEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.PokeHuntDebugLogger;
import me.alpha432.oyvey.util.PokeHuntParser;
import me.alpha432.oyvey.util.PokeHuntParser.HuntTarget;
import me.alpha432.oyvey.util.TextUtil;
import me.alpha432.oyvey.util.chat.ChatUtil;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Set;

import static me.alpha432.oyvey.util.chat.SimpleSignature.from;

public class CobblemonSpawnModule extends Module {

    public enum Rarity {
        ALL, UNCOMMON, RARE, ULTRA_RARE;

        private static final List<String> ORDER = List.of("common", "uncommon", "rare", "ultra-rare");

        public boolean passes(String bucket) {
            return switch (this) {
                case ALL        -> true;
                case UNCOMMON   -> ORDER.indexOf(bucket) >= ORDER.indexOf("uncommon");
                case RARE       -> ORDER.indexOf(bucket) >= ORDER.indexOf("rare");
                case ULTRA_RARE -> bucket.equals("ultra-rare");
            };
        }
    }

    private static final ResourceLocation POKEMON_TYPE = ResourceLocation.parse("cobblemon:pokemon");

    /** Legendaries and mythicals → "ultra-rare" */
    public static final Set<String> LEGENDARIES = Set.of(
        "articuno","zapdos","moltres","mewtwo","mew",
        "raikou","entei","suicune","lugia","ho-oh","celebi",
        "regirock","regice","registeel","latias","latios","kyogre","groudon","rayquaza","jirachi","deoxys",
        "uxie","mesprit","azelf","dialga","palkia","heatran","regigigas","giratina","cresselia",
        "phione","manaphy","darkrai","shaymin","arceus",
        "victini","cobalion","terrakion","virizion","tornadus","thundurus","reshiram","zekrom",
        "landorus","kyurem","keldeo","meloetta","genesect",
        "xerneas","yveltal","zygarde","diancie","hoopa","volcanion",
        "tapukoko","tapulele","tapubulu","tapufini","cosmog","cosmoem","solgaleo","lunala",
        "nihilego","buzzwole","pheromosa","xurkitree","celesteela","kartana","guzzlord",
        "necrozma","magearna","marshadow","poipole","naganadel","stakataka","blacephalon","zeraora",
        "meltan","melmetal","zacian","zamazenta","eternatus","kubfu","urshifu",
        "regieleki","regidrago","glastrier","spectrier","calyrex","zarude",
        "enamorus","wo-chien","chien-pao","ting-lu","chi-yu","koraidon","miraidon",
        "okidogi","munkidori","fezandipiti","ogerpon","terapagos","pecharunt"
    );

    /** Pseudo-legendaries → "rare" */
    public static final Set<String> PSEUDO_LEGENDARIES = Set.of(
        "dragonite","tyranitar","salamence","metagross","garchomp",
        "hydreigon","goodra","kommo-o","dragapult","baxcalibur"
    );

    /** Uncommon Pokémon: all starters (every stage), fossils, weather/condition-exclusive, rare spawns */
    public static final Set<String> UNCOMMONS = Set.of(
        // ── All starter lines (every stage) ──────────────────────────────────
        // Kanto
        "bulbasaur","ivysaur","venusaur",
        "charmander","charmeleon","charizard",
        "squirtle","wartortle","blastoise",
        // Johto
        "chikorita","bayleef","meganium",
        "cyndaquil","quilava","typhlosion",
        "totodile","croconaw","feraligatr",
        // Hoenn
        "treecko","grovyle","sceptile",
        "torchic","combusken","blaziken",
        "mudkip","marshtomp","swampert",
        // Sinnoh
        "turtwig","grotle","torterra",
        "chimchar","monferno","infernape",
        "piplup","prinplup","empoleon",
        // Unova
        "snivy","servine","serperior",
        "tepig","pignite","emboar",
        "oshawott","dewott","samurott",
        // Kalos
        "chespin","quilladin","chesnaught",
        "fennekin","braixen","delphox",
        "froakie","frogadier","greninja",
        // Alola
        "rowlet","dartrix","decidueye",
        "litten","torracat","incineroar",
        "popplio","brionne","primarina",
        // Galar
        "grookey","thwackey","rillaboom",
        "scorbunny","raboot","cinderace",
        "sobble","drizzile","inteleon",
        // Paldea
        "sprigatito","floragato","meowscarada",
        "fuecoco","crocalor","skeledirge",
        "quaxly","quaxwell","quaquaval",

        // ── Fossil Pokémon ────────────────────────────────────────────────────
        "omanyte","omastar","kabuto","kabutops","aerodactyl",
        "lileep","cradily","anorith","armaldo",
        "cranidos","rampardos","shieldon","bastiodon",
        "tirtouga","carracosta","archen","archeops",
        "tyrunt","tyrantrum","amaura","aurorus",
        "dracozolt","arctozolt","dracovish","arctovish",

        // ── Weather-conditional spawns ────────────────────────────────────────
        // Rain / Water weather
        "surskit","masquerain","lotad","lombre","ludicolo",
        "shellos","gastrodon","castform",
        "tympole","palpitoad","seismitoad",
        // Snow / Blizzard / Ice weather
        "snorunt","glalie","froslass",
        "swinub","piloswine","mamoswine",
        "vanillite","vanillish","vanilluxe",
        "cubchoo","beartic","bergmite","avalugg",
        "snover","abomasnow","cryogonal","cloyster",
        "smoochum","jynx","delibird","lapras",
        // Thunder / Electric weather
        "jolteon","raichu","electrode","electabuzz","electivire",
        "zebstrika","blitzle","emolga","dedenne","togedemaru",
        // Fog / Night / Dark conditions
        "misdreavus","mismagius","haunter","gengar",
        "murkrow","honchkrow","spiritomb",
        "zorua","zoroark","absol",
        // Dawn / Dusk time-gated
        "espeon","umbreon","riolu","lucario",

        // ── Hard-to-find / low-rate singles ──────────────────────────────────
        "snorlax","ditto","chansey","blissey","kangaskhan","tauros",
        "sigilyph","tropius","rotom","kecleon","feebas","milotic",
        "togepi","togetic","togekiss","eevee",
        "leafeon","glaceon","sylveon","vaporeon","flareon",
        "ralts","kirlia","gardevoir","gallade",
        "beldum","metang",
        "bagon","shelgon",
        "deino","zweilous",
        "goomy","sliggoo",
        "jangmo-o","hakamo-o",
        "dreepy","drakloak",
        "scyther","scizor","magmar","magmortar",
        "porygon","porygon2","porygonz",
        "mawile","shroomish","breloom",
        "slakoth","vigoroth","slaking",
        "magikarp","gyarados","slowpoke","slowbro","slowking",
        "sneasel","weavile","gligar","gliscor"
    );

    /** IDs we have already processed this session — avoids re-notifying the same spawn */
    private final Set<Integer> seen = new HashSet<>();
    /** IDs that passed the filter — ESP + active list uses these */
    public final Map<Integer, TrackedPokemon> tracked = new HashMap<>();

    public final Setting<Integer> range      = num("Range",      64,  1, 256);
    public final Setting<Rarity>  minRarity  = mode("MinRarity",  Rarity.ALL);
    public final Setting<Boolean> shinies    = bool("Shinies",    true);
    public final Setting<String>  filter     = str("Filter",      "");
    public final Setting<Boolean> esp        = bool("ESP",        false);
    public final Setting<Boolean> huntDebug  = bool("HuntDebug",  false);
    public final Setting<Boolean> autoHunt   = bool("AutoHunt",   true);
    public final Setting<Boolean> sound      = bool("Sound",      true);

    private final PokeHuntDebugLogger debugLogger = new PokeHuntDebugLogger();

    /** Active hunt targets parsed from the CATCHING HUNTS GUI. */
    public final List<HuntTarget> activeHunts = new ArrayList<>();

    // Auto-hunt join flow state
    private boolean wasConnected = false;
    private int joinCountdown = -1;           // ticks until we send /pokehunt
    private int pendingSlotClick = -1;        // slot to click next tick
    private int pendingClickDelay = -1;       // ticks until we click

    private static final int JOIN_DELAY_TICKS = 100; // ~5s after join
    private static final int CLICK_DELAY_TICKS = 5;  // ticks after POKE HUNTS opens

    public CobblemonSpawnModule() {
        super("CobblemonSpawn", "Notifies when Pokemon spawn nearby", Category.COBBLEMON);
    }

    @Override
    public String getDisplayInfo() {
        if (!activeHunts.isEmpty()) return activeHunts.size() + " hunts";
        return null;
    }

    @Override public void onEnable()  { seen.clear(); tracked.clear(); wasConnected = false; joinCountdown = -1; }
    @Override public void onDisable() { seen.clear(); tracked.clear(); activeHunts.clear(); debugLogger.stop(); wasConnected = false; joinCountdown = -1; }

    // ── Poll current screen for PokeHunt GUI ─────────────────────────────────

    private String lastScreenTitle = "";

    private void tickScreenPoll() {
        if (mc.screen == null) { lastScreenTitle = ""; return; }
        String title = mc.screen.getTitle().getString().trim();
        if (title.equals(lastScreenTitle)) return; // no change
        lastScreenTitle = title;

        if ("POKE HUNTS".equalsIgnoreCase(title) && autoHunt.getValue()) {
            // Find the Catching Hunts slot and schedule a click
            if (mc.player.containerMenu != null) {
                var items = mc.player.containerMenu.getItems();
                int slot = 20;
                for (int i = 0; i < items.size(); i++) {
                    if (!items.get(i).isEmpty()) {
                        String n = items.get(i).getHoverName().getString();
                        if (n.contains("Catching")) { slot = i; break; }
                    }
                }
                dbg("§7[AutoHunt] POKE HUNTS open — clicking slot " + slot);
                pendingSlotClick = slot;
                pendingClickDelay = CLICK_DELAY_TICKS;
            }
            return;
        }

        if ("CATCHING HUNTS".equalsIgnoreCase(title)) {
            if (mc.player.containerMenu == null) return;
            var items = mc.player.containerMenu.getItems();
            dbg("§7[AutoHunt] CATCHING HUNTS open (" + items.size() + " slots), parsing...");
            // Build a ContainerOpenEvent-like parse directly from the menu
            List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>(items);
            ContainerOpenEvent fake = new ContainerOpenEvent(
                net.minecraft.network.chat.Component.literal("CATCHING HUNTS"), list);
            List<HuntTarget> parsed = PokeHuntParser.parse(fake);
            if (parsed.isEmpty()) { dbg("§c[AutoHunt] Parsed 0 hunts!"); return; }
            activeHunts.clear();
            activeHunts.addAll(parsed);
            seen.clear();
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "§b[HClient] §aPokeHunt loaded: §f" + parsed.size() + " hunts active"),
                false);
        }
    }

    // ── onTick: detect new spawns ─────────────────────────────────────────────

    @Override
    public void onTick() {
        if (nullCheck()) return;

        // Toggle hunt debug logger on/off based on setting
        if (huntDebug.getValue() && !debugLogger.isActive()) debugLogger.start();
        if (!huntDebug.getValue() && debugLogger.isActive()) debugLogger.stop();

        tickScreenPoll();

        // ── Auto-hunt: detect server join ─────────────────────────────────────
        if (autoHunt.getValue()) {
            boolean connected = mc.player != null && mc.getConnection() != null;
            if (connected && !wasConnected) {
                // Just joined — start countdown
                wasConnected = true;
                joinCountdown = JOIN_DELAY_TICKS;
            }
            if (!connected) wasConnected = false;

            if (joinCountdown > 0) {
                // Pause countdown while any screen is open (server welcome GUIs, etc.)
                if (mc.screen == null) {
                    joinCountdown--;
                    if (joinCountdown == 0) {
                        joinCountdown = -1;
                        mc.player.connection.sendCommand("pokehunt");
                        dbg("§7[AutoHunt] Sent /pokehunt");
                    }
                }
            }
        }

        // ── Pending slot click (for auto-navigating POKE HUNTS → Catching) ───
        if (pendingClickDelay > 0) {
            pendingClickDelay--;
            if (pendingClickDelay == 0 && pendingSlotClick >= 0) {
                int slot = pendingSlotClick;
                pendingSlotClick = -1;
                if (mc.player.containerMenu != null) {
                    int cid = mc.player.containerMenu.containerId;
                    dbg("§7[AutoHunt] Clicking slot " + slot + " containerId=" + cid);
                    mc.gameMode.handleInventoryMouseClick(
                        cid, slot, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP,
                        mc.player
                    );
                } else {
                    dbg("§ccontainerMenu null — click skipped");
                }
            }
        }

        // Build two sets: all loaded pokemon IDs, and those within the configured range
        Set<Integer> allLoaded = new HashSet<>();
        Set<Integer> inRange   = new HashSet<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isPokemon(entity)) continue;
            int id = entity.getId();
            allLoaded.add(id);
            if (entity.distanceTo(mc.player) <= range.getValue()) inRange.add(id);
        }

        // Notify despawn for tracked pokemon that left all loaded chunks
        for (Map.Entry<Integer, TrackedPokemon> entry : tracked.entrySet()) {
            if (!allLoaded.contains(entry.getKey())) {
                TrackedPokemon tp = entry.getValue();
                ChatUtil.sendMessage(
                        TextUtil.text(buildDespawnMessage(tp.name(), tp.shiny(), tp.rarity())),
                        from("cobblemon-despawn-" + entry.getKey())
                );
            }
        }

        // Detect new spawns within range
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isPokemon(entity)) continue;
            if (!inRange.contains(entity.getId())) continue;

            int id = entity.getId();
            if (seen.contains(id)) continue;

            Object pokemon = getPokemon(entity);
            if (isOwned(entity, pokemon)) { seen.add(id); continue; }

            String name    = resolveName(pokemon, entity);
            boolean shiny  = pokemon != null && isShiny(pokemon);
            String rarity  = pokemon != null ? getSpawnBucket(pokemon) : "common";

            seen.add(id);

            boolean pinned   = isPinned(name);
            boolean rarityOk = minRarity.getValue().passes(rarity);
            boolean shinyOk  = shinies.getValue() && shiny;
            HuntTarget huntMatch = matchesHunt(name, pokemon);
            boolean huntOk   = huntMatch != null;

            if (!rarityOk && !shinyOk && !pinned && !huntOk) continue;

            int dex = getDexNumber(pokemon);
            tracked.put(id, new TrackedPokemon(entity, name, shiny, rarity, dex, pinned, huntOk));

            int x = entity.getBlockX(), y = entity.getBlockY(), z = entity.getBlockZ();
            ChatUtil.sendMessage(
                    TextUtil.text(buildMessage(name, x, y, z, shiny, rarity, shinyOk && !rarityOk, pinned, huntOk)),
                    from("cobblemon-spawn-" + id)
            );
            showActionBar(name, shiny, rarity, pinned, huntOk);
            if (sound.getValue()) playSpawnSound(shiny, huntOk);
        }

        // seen tracks range — allows re-notification when a pokemon re-enters range
        seen.removeIf(id -> !inRange.contains(id));
        // tracked tracks loaded chunks — removed when truly despawned
        tracked.keySet().removeIf(id -> !allLoaded.contains(id));
    }

    // ── onRender3D: ESP boxes ─────────────────────────────────────────────────

    @Override
    public void onRender3D(Render3DEvent e) {
        if (!esp.getValue() || tracked.isEmpty()) return;

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        for (TrackedPokemon tp : tracked.values()) {
            Color color = tp.shiny
                    ? new Color(255, 215, 0, 200)
                    : new Color(OyVey.colorManager.getColorWithAlpha(0, 200), true);
            RenderUtil.drawBox(e.getMatrix(), tp.entity.getBoundingBox(), color, 1.5f);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    // ── reflection helpers ────────────────────────────────────────────────────

    private boolean isPokemon(Entity entity) {
        return POKEMON_TYPE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private Object getPokemon(Entity entity) {
        try { return entity.getClass().getMethod("getPokemon").invoke(entity); }
        catch (Exception e) { return null; }
    }

    private boolean isShiny(Object pokemon) {
        for (String m : new String[]{"getShiny", "isShiny"}) {
            try { return Boolean.TRUE.equals(pokemon.getClass().getMethod(m).invoke(pokemon)); }
            catch (Exception ignored) {}
        }
        return false;
    }

    /** Returns true if this Pokémon belongs to a player (sent out, not wild). */
    private boolean isOwned(Entity entity, Object pokemon) {
        // Check entity-level ownerUUID (fastest path)
        for (String m : new String[]{"getOwnerUUID", "ownerUUID"}) {
            try {
                Object uuid = entity.getClass().getMethod(m).invoke(entity);
                if (uuid != null) return true;
            } catch (Exception ignored) {}
        }
        // Check pokemon-level owner
        if (pokemon != null) {
            for (String m : new String[]{"getOwnerUUID", "getOwner", "ownerUUID"}) {
                try {
                    Object owner = pokemon.getClass().getMethod(m).invoke(pokemon);
                    if (owner != null) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    /**
     * Determines rarity from a hardcoded tier list — more reliable than Cobblemon's
     * spawn bucket API which is not exposed on the entity object.
     * Tier: ultra-rare = legendaries/mythicals, rare = pseudo-legendaries, else = common.
     */
    private String getSpawnBucket(Object pokemon) {
        String name = resolveName(pokemon, null);
        if (name == null) return "common";
        // Cobblemon internal names are lowercase, strip spaces/hyphens for lookup
        String key = name.toLowerCase().replace(" ", "").replace("-", "");
        if (LEGENDARIES.contains(key)       || LEGENDARIES.contains(name.toLowerCase()))       return "ultra-rare";
        if (PSEUDO_LEGENDARIES.contains(key) || PSEUDO_LEGENDARIES.contains(name.toLowerCase())) return "rare";
        if (UNCOMMONS.contains(key)          || UNCOMMONS.contains(name.toLowerCase()))          return "uncommon";
        return "common";
    }

    private String resolveName(Object pokemon, Entity entity) {
        if (pokemon != null) {
            try {
                Object species = pokemon.getClass().getMethod("getSpecies").invoke(pokemon);
                try { return (String) species.getClass().getMethod("getName").invoke(species); }
                catch (Exception e) {
                    return species.getClass().getMethod("getTranslatedName").invoke(species).toString();
                }
            } catch (Exception ignored) {}
        }
        return entity.getDisplayName().getString();
    }

    // ── Hunt matching ─────────────────────────────────────────────────────────

    /** Returns the first matching HuntTarget for this entity, or null. */
    private HuntTarget matchesHunt(String name, Object pokemon) {
        if (activeHunts.isEmpty()) {
            // Hunts not loaded — notify once to help debug
            return null;
        }
        if (pokemon == null) return null;
        for (HuntTarget target : activeHunts) {
            if (!target.name().trim().equalsIgnoreCase(name.trim())) continue;
            if (huntDebug.getValue()) logHuntCandidate(name, pokemon, target);
            if (target.isNoRequirements()) return target;
            boolean gOk = checkGender(pokemon, target.gender());
            boolean nOk = checkNature(pokemon, target.natures());
            boolean iOk = checkIvs(pokemon, target.minIvPercent());
            if (gOk && nOk && iOk) return target;
            // Name matched but requirements failed — always notify in chat
            if (mc.player != null) {
                String g = readGenderStr(pokemon);
                String n = readNatureStr(pokemon);
                String ivs = readIvsStr(pokemon);
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§e[HUNT] §f" + name + " §7spawned but FAILED reqs: "
                        + "gender=" + g + " nature=" + n + " ivs=" + ivs
                        + " | need: " + target.gender() + " " + target.natures()),
                    false);
            }
        }
        return null;
    }

    /** Writes the Pokémon's actual gender/nature/IVs to the debug file. */
    private void logHuntCandidate(String name, Object pokemon, HuntTarget target) {
        try {
            String gender = readGenderStr(pokemon);
            String nature = readNatureStr(pokemon);
            String ivs    = readIvsStr(pokemon);
            String line = "[HUNT-CANDIDATE] " + name
                    + " | gender=" + gender
                    + " | nature=" + nature
                    + " | ivs=" + ivs
                    + " | target=" + target;
            java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter("pokehunt_debug.txt", true));
            pw.println(line);
            pw.close();
        } catch (Exception ignored) {}
    }

    private String readGenderStr(Object pokemon) {
        try {
            Object g = pokemon.getClass().getMethod("getGender").invoke(pokemon);
            return g != null ? g.toString() : "null";
        } catch (Exception e) { return "ERR:" + e.getMessage(); }
    }

    private String readNatureStr(Object pokemon) {
        try {
            Object nature = pokemon.getClass().getMethod("getNature").invoke(pokemon);
            if (nature == null) return "null";
            for (String m : new String[]{"getName", "getDisplayName", "name"}) {
                try { return nature.getClass().getMethod(m).invoke(nature).toString(); }
                catch (Exception ignored) {}
            }
            return nature.toString();
        } catch (Exception e) { return "ERR:" + e.getMessage(); }
    }

    private String readIvsStr(Object pokemon) {
        try {
            Object ivs = pokemon.getClass().getMethod("getIvs").invoke(pokemon);
            if (ivs == null) return "null";
            // Try getTotal()
            try {
                int total = (int) ivs.getClass().getMethod("getTotal").invoke(ivs);
                return total + "/186 (" + Math.round(total / 186f * 100) + "%)";
            } catch (Exception ignored) {}
            // Try listing all methods that return int
            StringBuilder sb = new StringBuilder("{");
            for (java.lang.reflect.Method m : ivs.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == int.class
                        && !m.getName().equals("hashCode")) {
                    try { sb.append(m.getName()).append("=").append(m.invoke(ivs)).append(","); }
                    catch (Exception ignored) {}
                }
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) { return "ERR:" + e.getMessage(); }
    }

    private boolean checkGender(Object pokemon, String required) {
        if ("any".equals(required)) return true;
        try {
            Object gender = pokemon.getClass().getMethod("getGender").invoke(pokemon);
            String gName = gender.toString().toLowerCase();
            return gName.contains(required.toLowerCase());
        } catch (Exception e) { return true; }
    }

    private boolean checkNature(Object pokemon, List<String> required) {
        if (required.isEmpty()) return true;
        try {
            Object nature = pokemon.getClass().getMethod("getNature").invoke(pokemon);
            String nName = null;
            for (String m : new String[]{"getName", "getDisplayName", "name"}) {
                try {
                    Object r = nature.getClass().getMethod(m).invoke(nature);
                    nName = r.toString().toLowerCase().trim(); break;
                } catch (Exception ignored) {}
            }
            if (nName == null) return true;
            for (String req : required) {
                if (req.equalsIgnoreCase(nName)) return true;
            }
            return false;
        } catch (Exception e) { return true; }
    }

    private boolean checkIvs(Object pokemon, int minPct) {
        if (minPct < 0) return true;
        try {
            Object ivs = pokemon.getClass().getMethod("getIvs").invoke(pokemon);
            int total = 0;
            try {
                total = (int) ivs.getClass().getMethod("getTotal").invoke(ivs);
            } catch (Exception ignored) {
                for (String stat : new String[]{"hp","attack","defence","defense","specialAttack","specialDefence","specialDefense","speed"}) {
                    try {
                        Object val = ivs.getClass().getMethod("get", String.class).invoke(ivs, stat);
                        if (val instanceof Number n) total += n.intValue();
                    } catch (Exception ignored2) {}
                }
            }
            if (total <= 0) return true;
            int pct = Math.round(total / 186f * 100);
            return pct >= minPct;
        } catch (Exception e) { return true; }
    }

    /** Returns true when filter is non-empty AND this name is explicitly listed — bypasses rarity. */
    private boolean isPinned(String name) {
        String f = filter.getValue().trim();
        if (f.isEmpty()) return false;
        for (String entry : f.split(",")) {
            if (entry.trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void dbg(String msg) {
        if (!huntDebug.getValue() || mc.player == null) return;
        mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false);
    }

    // ── sound ─────────────────────────────────────────────────────────────────

    private void playSpawnSound(boolean shiny, boolean hunt) {
        net.minecraft.sounds.SoundEvent sfx = net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value();
        float pitch = hunt ? 2.0f : shiny ? 1.8f : 1.5f;
        mc.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sfx, pitch)
        );
    }

    // ── message builder ───────────────────────────────────────────────────────

    private String buildMessage(String name, int x, int y, int z,
                                boolean shiny, String rarity, boolean shinyOnly, boolean pinned, boolean hunt) {
        StringBuilder sb = new StringBuilder();
        if (hunt)           sb.append("{gold}[HUNT]{gray} ");
        else if (pinned)    sb.append("{yellow}[!]{gray} ");
        if (shiny) sb.append("{yellow}★ SHINY{gray} ");
        sb.append(name);
        if (!rarity.equals("common")) {
            String c = switch (rarity) {
                case "uncommon"   -> "{green}";
                case "rare"       -> "{aqua}";
                case "ultra-rare" -> "{light_purple}";
                default           -> "{gray}";
            };
            sb.append(" ").append(c).append("(").append(rarity).append("){gray}");
        } else if (hunt) {
            sb.append(" {gold}(hunt)");
        } else if (pinned) {
            sb.append(" {gray}(pinned)");
        }
        sb.append(" @ {white}").append(x).append(" ").append(y).append(" ").append(z);
        return sb.toString();
    }

    private String buildDespawnMessage(String name, boolean shiny, String rarity) {
        StringBuilder sb = new StringBuilder("{gray}");
        if (shiny) sb.append("{yellow}★{gray} ");
        sb.append(name);
        if (!rarity.equals("common")) {
            String c = switch (rarity) {
                case "uncommon"   -> "{green}";
                case "rare"       -> "{aqua}";
                case "ultra-rare" -> "{light_purple}";
                default           -> "{gray}";
            };
            sb.append(" ").append(c).append("(").append(rarity).append("){gray}");
        }
        sb.append(" {red}despawned");
        return sb.toString();
    }

    private int getDexNumber(Object pokemon) {
        if (pokemon == null) return 0;
        try {
            Object species = pokemon.getClass().getMethod("getSpecies").invoke(pokemon);
            for (String m : new String[]{"getNationalPokedexNumber", "nationalPokedexNumber"}) {
                try { return (int) species.getClass().getMethod(m).invoke(species); }
                catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public record TrackedPokemon(Entity entity, String name, boolean shiny, String rarity, int dexNumber, boolean pinned, boolean hunt) {}

    // ── Subtitle notification ─────────────────────────────────────────────────

    private void showActionBar(String name, boolean shiny, String rarity, boolean pinned, boolean hunt) {
        net.minecraft.network.chat.MutableComponent text = net.minecraft.network.chat.Component.empty();
        if (hunt) {
            text.append(net.minecraft.network.chat.Component.literal("[HUNT] ")
                    .withStyle(s -> s.withColor(0xFF8800).withBold(true)));
        } else if (shiny) {
            text.append(net.minecraft.network.chat.Component.literal("★ SHINY ")
                    .withStyle(s -> s.withColor(0xFFDD44)));
        } else if (pinned) {
            text.append(net.minecraft.network.chat.Component.literal("[!] ")
                    .withStyle(s -> s.withColor(0xFFDD44)));
        }
        int nameColor = hunt ? 0xFF8800 : shiny ? 0xFFDD44 : switch (rarity) {
            case "ultra-rare" -> 0xFF55FF;
            case "rare"       -> 0x55FFFF;
            case "uncommon"   -> 0x55FF55;
            default           -> pinned ? 0xFFAA00 : 0xFFFFFF;
        };
        text.append(net.minecraft.network.chat.Component.literal(
                Character.toUpperCase(name.charAt(0)) + name.substring(1))
                .withStyle(s -> s.withColor(nameColor)));
        text.append(net.minecraft.network.chat.Component.literal(" spawned!")
                .withStyle(s -> s.withColor(0xAAAAAA)));

        mc.player.displayClientMessage(text, true);
    }
}
