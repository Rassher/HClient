package me.alpha432.oyvey.util;

import me.alpha432.oyvey.event.impl.network.ContainerOpenEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Apex Cobblemon CATCHING HUNTS GUI into HuntTarget objects.
 *
 * GUI format (title = "CATCHING HUNTS"):
 *   item cobblemon:pokemon_model → one hunt entry
 *     display name  : " Wooper " (trimmed)
 *     lore[0]       : "Catching Hunt (Easy/Medium/Hard)"
 *     lore[2]       : "ʀᴇǫᴜɪʀᴇᴍᴇɴᴛs"
 *     lore[3..]     : "↪ <requirement>"
 *       ↪ None
 *       ↪ Female Gender | ↪ Male Gender
 *       ↪ 117 (62%) Minimum IV Count
 *       ↪ Natura1, Natura2 or Natura3 Nature
 */
public final class PokeHuntParser {

    public static final String CATCHING_HUNTS_TITLE = "CATCHING HUNTS";

    private static final Pattern IV_PATTERN      = Pattern.compile("\\((\\d+)%\\)\\s*Minimum IV Count");
    private static final Pattern GENDER_PATTERN  = Pattern.compile("↪\\s*(Female|Male)\\s+Gender", Pattern.CASE_INSENSITIVE);
    private static final Pattern NATURE_PATTERN  = Pattern.compile("↪\\s*(.+?)\\s+Nature$");
    private static final String  REQ_MARKER      = "ʀᴇǫᴜɪʀᴇᴍᴇɴᴛs";
    private static final String  ARROW            = "↪";

    // Spanish Pokémon nature names → English (Cobblemon internal)
    private static final Map<String, String> ES_TO_EN = new HashMap<>();
    static {
        ES_TO_EN.put("fuerte",    "hardy");
        ES_TO_EN.put("huraña",    "lonely");
        ES_TO_EN.put("audaz",     "brave");
        ES_TO_EN.put("firme",     "adamant");
        ES_TO_EN.put("grosera",   "naughty");
        ES_TO_EN.put("osada",     "bold");
        ES_TO_EN.put("dócil",     "docile");
        ES_TO_EN.put("docil",     "docile");
        ES_TO_EN.put("plácida",   "relaxed");
        ES_TO_EN.put("placida",   "relaxed");
        ES_TO_EN.put("pícara",    "impish");
        ES_TO_EN.put("picara",    "impish");
        ES_TO_EN.put("afable",    "lax");
        ES_TO_EN.put("tímida",    "timid");
        ES_TO_EN.put("timida",    "timid");
        ES_TO_EN.put("activa",    "hasty");
        ES_TO_EN.put("seria",     "serious");
        ES_TO_EN.put("alegre",    "jolly");
        ES_TO_EN.put("ingenua",   "naive");
        ES_TO_EN.put("modesta",   "modest");
        ES_TO_EN.put("flexible",  "mild");
        ES_TO_EN.put("tranquila", "quiet");
        ES_TO_EN.put("rara",      "bashful");
        ES_TO_EN.put("alocada",   "rash");
        ES_TO_EN.put("serena",    "calm");
        ES_TO_EN.put("amable",    "gentle");
        ES_TO_EN.put("agitada",   "sassy");
        ES_TO_EN.put("prudente",  "careful");
        ES_TO_EN.put("cauta",     "careful");
        ES_TO_EN.put("miedosa",   "timid");
        ES_TO_EN.put("extraña",   "quirky");
        ES_TO_EN.put("extrana",   "quirky");
    }

    private PokeHuntParser() {}

    public static boolean isCatchingHuntsGui(ContainerOpenEvent e) {
        return CATCHING_HUNTS_TITLE.equalsIgnoreCase(e.getTitleText().trim());
    }

    /** Parse all hunt entries from a CATCHING HUNTS GUI event. */
    public static List<HuntTarget> parse(ContainerOpenEvent e) {
        List<HuntTarget> result = new ArrayList<>();
        for (ItemStack stack : e.getItems()) {
            if (stack.isEmpty()) continue;
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (!"cobblemon:pokemon_model".equals(itemId)) continue;

            HuntTarget target = parseItem(stack);
            if (target != null) result.add(target);
        }
        return result;
    }

    private static HuntTarget parseItem(ItemStack stack) {
        String rawName = stack.getHoverName().getString().trim();
        if (rawName.isBlank()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        List<String> lines = new ArrayList<>();
        if (lore != null) {
            for (var comp : lore.lines()) lines.add(comp.getString());
        }

        // Difficulty
        String difficulty = "Easy";
        if (!lines.isEmpty()) {
            String first = lines.get(0);
            if (first.contains("Medium")) difficulty = "Medium";
            else if (first.contains("Hard")) difficulty = "Hard";
        }

        // Find requirement lines (after ʀᴇǫᴜɪʀᴇᴍᴇɴᴛs marker)
        int reqIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(REQ_MARKER)) { reqIdx = i; break; }
        }
        if (reqIdx < 0) return new HuntTarget(rawName, difficulty, "any", -1, Collections.emptyList());

        String gender   = "any";
        int    minIvPct = -1;
        List<String> natures = new ArrayList<>();

        for (int i = reqIdx + 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.startsWith(ARROW)) continue;

            // None
            if (line.contains("None")) continue;

            // IV count
            Matcher ivM = IV_PATTERN.matcher(line);
            if (ivM.find()) { minIvPct = Integer.parseInt(ivM.group(1)); continue; }

            // Gender
            Matcher gM = GENDER_PATTERN.matcher(line);
            if (gM.find()) { gender = gM.group(1).toLowerCase(); continue; }

            // Nature  — "↪ Tímida, Osada or Alegre Nature"
            Matcher nM = NATURE_PATTERN.matcher(line);
            if (nM.find()) {
                String raw = nM.group(1); // "Tímida, Osada or Alegre"
                // split on ", " and " or "
                String[] parts = raw.split(",\\s*| or ");
                for (String p : parts) {
                    String n = p.trim().toLowerCase();
                    String en = ES_TO_EN.getOrDefault(n, n); // fallback to original
                    if (!en.isBlank()) natures.add(en);
                }
            }
        }

        return new HuntTarget(rawName, difficulty, gender, minIvPct, natures);
    }

    /** Maps a Spanish nature name to English (lowercase). Returns original if not found. */
    public static String spanishToEnglish(String esName) {
        return ES_TO_EN.getOrDefault(esName.toLowerCase().trim(), esName.toLowerCase().trim());
    }

    // ── Data model ────────────────────────────────────────────────────────────

    public record HuntTarget(
            String name,
            String difficulty,   // Easy / Medium / Hard
            String gender,       // "any" / "male" / "female"
            int    minIvPercent, // -1 = no requirement
            List<String> natures // empty = any; English lowercase names
    ) {
        /** True if this target has no requirements beyond species. */
        public boolean isNoRequirements() {
            return "any".equals(gender) && minIvPercent < 0 && natures.isEmpty();
        }

        @Override public String toString() {
            return name + " [" + difficulty + "]"
                    + (!"any".equals(gender) ? " gender=" + gender : "")
                    + (minIvPercent >= 0 ? " ivs≥" + minIvPercent + "%" : "")
                    + (!natures.isEmpty() ? " nature=" + natures : "");
        }
    }
}
