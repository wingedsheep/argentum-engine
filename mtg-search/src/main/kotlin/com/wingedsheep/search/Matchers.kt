package com.wingedsheep.search

/**
 * Matcher registry — the swappable part of the search language.
 *
 * Every supported key is one entry in [Matchers.REGISTRY]. A matcher receives
 * the parsed [AtomNode] and returns either a predicate or a typed error.
 *
 * Mirrors `web-client/src/components/deckbuilder/query/matchers.ts`. The two
 * implementations must agree on filter semantics — when adding a new matcher,
 * change both at once.
 */

typealias CardPredicate = (SearchCard) -> Boolean

sealed interface MatcherResult {
    data class Ok(val predicate: CardPredicate) : MatcherResult
    data class Err(val message: String, val suggestion: String? = null) : MatcherResult
}

interface Matcher {
    val aliases: List<String>
    val ops: List<Op>
    val description: String
    fun build(atom: AtomNode): MatcherResult
}

object Matchers {

    private val ALWAYS_TRUE: CardPredicate = { true }
    private val ALWAYS_FALSE: CardPredicate = { false }

    private val NUMERIC_OPS = listOf(Op.EQ, Op.EXACT, Op.NEQ, Op.LE, Op.GE, Op.LT, Op.GT)
    private val COLOR_OPS = listOf(Op.EQ, Op.EXACT, Op.LE, Op.GE, Op.LT, Op.GT)
    private val STRING_OPS = listOf(Op.EQ)

    private fun ok(p: CardPredicate): MatcherResult = MatcherResult.Ok(p)
    private fun err(message: String, suggestion: String? = null): MatcherResult =
        MatcherResult.Err(message, suggestion)

    // ---- numeric helpers --------------------------------------------------

    private val FIELDS: Map<String, (SearchCard) -> Int?> = mapOf(
        "cmc" to { it.cmc },
        "mv" to { it.cmc },
        "pow" to { parseLeadingInt(it.power) },
        "tou" to { parseLeadingInt(it.toughness) },
        "loy" to { _ -> null },
    )

    private fun parseLeadingInt(s: String?): Int? {
        if (s.isNullOrEmpty()) return null
        val m = Regex("^-?\\d+").find(s) ?: return null
        return m.value.toIntOrNull()
    }

    private fun numericCompare(op: Op, a: Int, b: Int): Boolean = when (op) {
        Op.EQ, Op.EXACT -> a == b
        Op.NEQ -> a != b
        Op.LT -> a < b
        Op.LE -> a <= b
        Op.GT -> a > b
        Op.GE -> a >= b
    }

    private fun buildNumeric(field: (SearchCard) -> Int?): (AtomNode) -> MatcherResult = { atom ->
        val v = atom.value.lowercase().trim()
        val fieldRef = FIELDS[v]
        when {
            fieldRef != null -> ok { c ->
                val left = field(c); val right = fieldRef(c)
                if (left == null || right == null) false else numericCompare(atom.op, left, right)
            }
            else -> {
                val n = v.toIntOrNull()
                if (n == null) err("Expected a number or field reference (cmc/pow/tou/loy), got \"${atom.value}\".")
                else ok { c ->
                    val left = field(c)
                    if (left == null) false else numericCompare(atom.op, left, n)
                }
            }
        }
    }

    // ---- name / oracle / type --------------------------------------------

    private fun buildName(target: NameTarget): (AtomNode) -> MatcherResult = { atom ->
        // A name atom matches the oracle name or any printed name; oracle text has one value.
        val get: (SearchCard) -> List<String> = when (target) {
            NameTarget.NAME -> { c -> if (c.printedNames.isEmpty()) listOf(c.name) else listOf(c.name) + c.printedNames }
            NameTarget.ORACLE -> { c -> listOf(c.oracleText ?: "") }
        }
        when {
            atom.regex -> {
                try {
                    val flags = if (atom.regexFlags.isEmpty()) "i" else atom.regexFlags
                    val re = Regex(atom.value, regexOptions(flags))
                    ok { c -> get(c).any { re.containsMatchIn(it) } }
                } catch (e: Exception) {
                    err("Invalid regex: ${e.message}.")
                }
            }
            atom.exact && target == NameTarget.NAME -> {
                val want = atom.value.lowercase()
                ok { c -> get(c).any { it.lowercase() == want } }
            }
            else -> {
                val needle = atom.value.lowercase()
                if (needle.isEmpty()) ok(ALWAYS_TRUE) else ok { c -> get(c).any { it.lowercase().contains(needle) } }
            }
        }
    }

    private enum class NameTarget { NAME, ORACLE }

    private fun regexOptions(flags: String): Set<RegexOption> = buildSet {
        if (flags.contains('i')) add(RegexOption.IGNORE_CASE)
        if (flags.contains('m')) add(RegexOption.MULTILINE)
        if (flags.contains('s')) add(RegexOption.DOT_MATCHES_ALL)
    }

    private val typeMatcher: (AtomNode) -> MatcherResult = { atom ->
        val words = atom.value.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) ok(ALWAYS_TRUE)
        else ok { c ->
            val all = (c.cardTypes + c.supertypes + c.subtypes).map { it.lowercase() }
            words.all { w -> all.any { it.contains(w) } }
        }
    }

    // ---- color / cost -----------------------------------------------------

    private fun buildColor(get: (SearchCard) -> List<String>): (AtomNode) -> MatcherResult = { atom ->
        when (val parsed = ColorSugar.parseColorValue(atom.value)) {
            is ColorSugar.ColorParse.Error -> err(parsed.message)
            is ColorSugar.ColorParse.Count -> ok { c -> numericCompare(atom.op, get(c).size, parsed.value) }
            is ColorSugar.ColorParse.Colorless -> when (atom.op) {
                Op.EQ, Op.EXACT -> ok { c -> get(c).isEmpty() }
                else -> ok(ALWAYS_FALSE)
            }
            is ColorSugar.ColorParse.Multi -> when (atom.op) {
                Op.EQ, Op.EXACT -> ok { c -> get(c).size >= 2 }
                else -> ok(ALWAYS_FALSE)
            }
            is ColorSugar.ColorParse.Mono -> when (atom.op) {
                Op.EQ, Op.EXACT -> ok { c -> get(c).size == 1 }
                else -> ok(ALWAYS_FALSE)
            }
            is ColorSugar.ColorParse.Colors -> {
                val wanted = parsed.set
                when (atom.op) {
                    Op.EQ, Op.GE -> ok { c -> wanted.all { it in get(c) } }
                    Op.EXACT -> ok { c -> val cs = get(c); cs.size == wanted.size && wanted.all { it in cs } }
                    Op.LE -> ok { c -> get(c).all { it in wanted } }
                    Op.GT -> ok { c -> val cs = get(c); wanted.all { it in cs } && cs.size > wanted.size }
                    Op.LT -> ok { c -> val cs = get(c); cs.size < wanted.size && cs.all { it in wanted } }
                    else -> err("Unsupported operator \"${atom.op.text}\" on color.")
                }
            }
        }
    }

    // ---- discrete keys ----------------------------------------------------

    private val RARITY_ALIAS = mapOf(
        "c" to "COMMON", "common" to "COMMON",
        "u" to "UNCOMMON", "uncommon" to "UNCOMMON",
        "r" to "RARE", "rare" to "RARE",
        "m" to "MYTHIC", "mythic" to "MYTHIC",
        "s" to "SPECIAL", "special" to "SPECIAL",
        "bonus" to "BONUS",
    )

    /**
     * Ordinal rank for `r>=...` / `r<=...` comparators. Scryfall orders
     * common < uncommon < rare < mythic; we mirror that and place the
     * non-standard rarities at the extremes so they don't accidentally
     * sort into the middle.
     */
    private val RARITY_RANK = mapOf(
        "COMMON" to 1, "UNCOMMON" to 2, "RARE" to 3, "MYTHIC" to 4,
        "SPECIAL" to 5, "BONUS" to 6,
    )

    private val rarity: (AtomNode) -> MatcherResult = { atom ->
        val target = RARITY_ALIAS[atom.value.lowercase()] ?: atom.value.uppercase()
        when (atom.op) {
            Op.EQ, Op.EXACT -> ok { c -> c.rarity == target }
            Op.NEQ -> ok { c -> c.rarity != target }
            Op.LT, Op.LE, Op.GT, Op.GE -> {
                val wantedRank = RARITY_RANK[target]
                if (wantedRank == null) err("Cannot order \"${atom.value}\" — known rarities are common/uncommon/rare/mythic.")
                else ok { c ->
                    val rank = RARITY_RANK[c.rarity] ?: return@ok false
                    numericCompare(atom.op, rank, wantedRank)
                }
            }
        }
    }

    private val set: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.lowercase().trim()
        if (target.isEmpty()) ok(ALWAYS_TRUE)
        else ok { c ->
            // Match the canonical printing or any reprint set the card has a printing in.
            // Reprint coverage is what makes `s:EOE Banishing Light` work for cards whose
            // canonical [setCode] is the original printing (BLB) but which also have an
            // EOE reprint row in PrintingRegistry.
            c.setCode?.lowercase() == target ||
                c.printingSetCodes.any { it.lowercase() == target }
        }
    }

    private val format: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.uppercase().trim()
        if (target.isEmpty()) ok(ALWAYS_TRUE) else ok { c -> target in c.legalFormats }
    }

    private val keyword: (AtomNode) -> MatcherResult = { atom ->
        val target = atom.value.lowercase().replace(Regex("\\s+"), "_")
        if (target.isEmpty()) ok(ALWAYS_TRUE) else ok { c -> c.keywords.any { it.lowercase() == target } }
    }

    private val layout: (AtomNode) -> MatcherResult = { atom ->
        when (atom.value.lowercase()) {
            "transform", "mdfc", "modal_dfc", "dfc", "doublefaced", "double_faced" ->
                ok { c -> c.isDoubleFaced }
            "normal" -> ok { c -> !c.isDoubleFaced }
            else -> err("Unknown layout \"${atom.value}\". Try transform / mdfc / normal.")
        }
    }

    /**
     * Scryfall `is:` flags we recognise but can't evaluate yet because the
     * underlying data isn't on `SearchCard`. We emit a clear "not yet
     * supported" error rather than silently falling through to keyword
     * lookup — silent fallthrough is exactly the bug this query rewrite
     * is meant to fix.
     */
    private val IS_KNOWN_UNSUPPORTED = mapOf(
        "reprint" to "print metadata not stamped on the catalog",
        "firstprint" to "print metadata not stamped on the catalog",
        "unique" to "print metadata not stamped on the catalog",
        "hybrid" to "mana-symbol metadata not parsed",
        "phyrexian" to "mana-symbol metadata not parsed",
        "split" to "split-card layout not modelled",
        "flip" to "flip-card layout not modelled",
        "meld" to "meld-card layout not modelled",
        "leveler" to "leveler-card layout not modelled",
        "modal" to "modal-spell metadata not surfaced",
        "party" to "party-tribe metadata not surfaced",
        "outlaw" to "outlaw-supertypes metadata not surfaced",
        "manland" to "land-cycle metadata not surfaced",
        "bikeland" to "land-cycle metadata not surfaced",
        "cycleland" to "land-cycle metadata not surfaced",
        "checkland" to "land-cycle metadata not surfaced",
        "shockland" to "land-cycle metadata not surfaced",
        "fetchland" to "land-cycle metadata not surfaced",
        "painland" to "land-cycle metadata not surfaced",
        "scryland" to "land-cycle metadata not surfaced",
        "fastland" to "land-cycle metadata not surfaced",
        "slowland" to "land-cycle metadata not surfaced",
        "filterland" to "land-cycle metadata not surfaced",
        "gainland" to "land-cycle metadata not surfaced",
        "triland" to "land-cycle metadata not surfaced",
        "triome" to "land-cycle metadata not surfaced",
        "creatureland" to "land-cycle metadata not surfaced",
        "dual" to "land-cycle metadata not surfaced",
        "funny" to "set-style metadata not surfaced",
        "digital" to "print game-mode not surfaced",
        "alchemy" to "print game-mode not surfaced",
        "rebalanced" to "print game-mode not surfaced",
        "promo" to "print metadata not surfaced",
        "spotlight" to "print metadata not surfaced",
        "scryfallpreview" to "print metadata not surfaced",
        "foil" to "physical printing not surfaced",
        "nonfoil" to "physical printing not surfaced",
        "etched" to "physical printing not surfaced",
        "glossy" to "physical printing not surfaced",
        "hires" to "image metadata not surfaced",
        "universesbeyond" to "set-style metadata not surfaced",
        "default" to "print-prefer metadata not surfaced",
        "atypical" to "print-prefer metadata not surfaced",
        "old" to "border metadata not surfaced",
        "new" to "border metadata not surfaced",
        "datestamped" to "print metadata not surfaced",
        "booster" to "set metadata not surfaced",
        "league" to "set metadata not surfaced",
        "buyabox" to "set metadata not surfaced",
        "giftbox" to "set metadata not surfaced",
        "intro_pack" to "set metadata not surfaced",
        "gameday" to "set metadata not surfaced",
        "prerelease" to "set metadata not surfaced",
        "release" to "set metadata not surfaced",
        "fnm" to "set metadata not surfaced",
        "judge_gift" to "set metadata not surfaced",
        "arena_league" to "set metadata not surfaced",
        "player_rewards" to "set metadata not surfaced",
        "media_insert" to "set metadata not surfaced",
        "instore" to "set metadata not surfaced",
        "convention" to "set metadata not surfaced",
        "set_promo" to "set metadata not surfaced",
        "planeswalker_deck" to "set metadata not surfaced",
        "commander" to "ambiguous (use t:legendary t:creature instead)",
        "brawler" to "format-eligibility metadata not surfaced",
        "companion" to "format-eligibility metadata not surfaced",
        "duelcommander" to "format-eligibility metadata not surfaced",
        "oathbreaker" to "format-eligibility metadata not surfaced",
        "partner" to "format-eligibility metadata not surfaced",
        "gamechanger" to "format-eligibility metadata not surfaced",
        "reserved" to "reserved-list metadata not surfaced",
        "tdfc" to "DFC layout subtype not surfaced (use is:dfc)",
        "meldpart" to "meld-card layout not modelled",
        "meldresult" to "meld-card layout not modelled",
        "newinpauper" to "format-eligibility delta not surfaced",
    )

    private val isMatcher: (AtomNode) -> MatcherResult = { atom ->
        val v = atom.value.lowercase()
        when (v) {
            "land" -> ok { c -> "LAND" in c.cardTypes }
            "creature" -> ok { c -> "CREATURE" in c.cardTypes }
            "instant" -> ok { c -> "INSTANT" in c.cardTypes }
            "sorcery" -> ok { c -> "SORCERY" in c.cardTypes }
            "enchantment" -> ok { c -> "ENCHANTMENT" in c.cardTypes }
            "artifact" -> ok { c -> "ARTIFACT" in c.cardTypes }
            "planeswalker" -> ok { c -> "PLANESWALKER" in c.cardTypes }
            "kindred" -> ok { c -> "KINDRED" in c.cardTypes }
            "permanent" -> ok { c ->
                "CREATURE" in c.cardTypes || "LAND" in c.cardTypes ||
                "ARTIFACT" in c.cardTypes || "ENCHANTMENT" in c.cardTypes ||
                "PLANESWALKER" in c.cardTypes
            }
            "spell" -> ok { c -> "LAND" !in c.cardTypes }
            "legendary" -> ok { c -> "LEGENDARY" in c.supertypes }
            "basic" -> ok { c -> c.basicLand }
            "colorless" -> ok { c -> c.colorIdentity.isEmpty() }
            "multicolor", "multicolour" -> ok { c -> c.colorIdentity.size >= 2 }
            "monocolor", "monocolour", "mono" -> ok { c -> c.colorIdentity.size == 1 }
            "dfc", "mdfc", "transform", "doublefaced", "double_faced" -> ok { c -> c.isDoubleFaced }
            "vanilla" -> ok { c -> "CREATURE" in c.cardTypes && (c.oracleText.isNullOrBlank()) }
            "frenchvanilla", "french_vanilla" -> ok { c -> "CREATURE" in c.cardTypes && c.keywords.isNotEmpty() }
            "bear" -> ok { c ->
                "CREATURE" in c.cardTypes && c.cmc == 2 &&
                parseLeadingInt(c.power) == 2 && parseLeadingInt(c.toughness) == 2
            }
            "historic" -> ok { c -> "LEGENDARY" in c.supertypes || "ARTIFACT" in c.cardTypes || "SAGA" in c.subtypes }
            in IS_KNOWN_UNSUPPORTED -> err("`is:$v` is not yet supported (${IS_KNOWN_UNSUPPORTED[v]}).")
            else -> keyword(atom.copy(value = v))
        }
    }

    private val mana: (AtomNode) -> MatcherResult = { atom ->
        ok { c -> ManaCostMatch.matches(atom.op, atom.value, c.manaCost) }
    }

    // ---- registry ---------------------------------------------------------

    private val ENTRIES: List<Matcher> = listOf(
        entry(listOf("name", "n"), STRING_OPS, "Card name (substring or regex)", buildName(NameTarget.NAME)),
        entry(listOf("oracle", "o"), STRING_OPS, "Oracle text contains", buildName(NameTarget.ORACLE)),
        entry(listOf("type", "t"), STRING_OPS, "Type line (AND of words)", typeMatcher),
        entry(listOf("c", "color", "colour", "id", "identity"), COLOR_OPS, "Color identity (CR 903.4)", buildColor { it.colorIdentity }),
        entry(listOf("cost"), COLOR_OPS, "Printed mana-cost colors", buildColor { it.colors }),
        entry(listOf("mana", "m"), NUMERIC_OPS, "Mana cost symbols (multiset compare)", mana),
        entry(listOf("cmc", "mv", "manavalue"), NUMERIC_OPS, "Mana value", buildNumeric { it.cmc }),
        entry(listOf("pow", "power"), NUMERIC_OPS, "Power (numeric only)", buildNumeric { parseLeadingInt(it.power) }),
        entry(listOf("tou", "toughness"), NUMERIC_OPS, "Toughness (numeric only)", buildNumeric { parseLeadingInt(it.toughness) }),
        entry(listOf("loy", "loyalty"), NUMERIC_OPS, "Loyalty (not surfaced yet)", buildNumeric { _ -> null }),
        entry(listOf("r", "rarity"), NUMERIC_OPS, "Rarity (ordinal: common < uncommon < rare < mythic)", rarity),
        entry(listOf("s", "set", "e", "edition"), STRING_OPS, "Set code", set),
        entry(listOf("f", "format", "legal"), STRING_OPS, "Format legality", format),
        entry(listOf("kw", "keyword"), STRING_OPS, "Keyword ability", keyword),
        entry(listOf("layout"), STRING_OPS, "Card layout", layout),
        entry(listOf("is"), STRING_OPS, "Boolean flag", isMatcher),
    )

    val REGISTRY: Map<String, Matcher> = buildMap {
        for (e in ENTRIES) for (a in e.aliases) put(a, e)
    }

    val ALL_KEYS: List<String> = ENTRIES.flatMap { it.aliases }.distinct().sorted()

    fun suggestKey(unknown: String): String? {
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (k in ALL_KEYS) {
            val d = levenshtein(unknown, k)
            if (d < bestDist) { bestDist = d; best = k }
        }
        return if (bestDist <= 2) best else null
    }

    private fun entry(aliases: List<String>, ops: List<Op>, description: String, build: (AtomNode) -> MatcherResult): Matcher =
        object : Matcher {
            override val aliases = aliases
            override val ops = ops
            override val description = description
            override fun build(atom: AtomNode) = build.invoke(atom)
        }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            prev = curr.copyOf()
        }
        return prev[b.length]
    }
}
