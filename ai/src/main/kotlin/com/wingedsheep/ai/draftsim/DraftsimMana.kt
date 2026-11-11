package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.engine.PipDemand

/**
 * Pure mana-cost-string primitives ported from the Draftsim bundle (`index-CgvY9PKD.pretty.js`).
 *
 * Everything here operates on the brace string form a card's cost is carried in
 * (`"{1}{U}{B}"`, `"{W/U}{2/G}"`, Phyrexian `"{G/P}"`). Split / DFC costs arrive as `"a // b"`;
 * the per-face helpers split on `//` and the callers reduce across faces (see [DraftsimScorer]).
 *
 * The bundle symbols each function ports are noted in comments; never the minified names in code.
 */
internal object DraftsimMana {

    /** Canonical color order `eW`. */
    val COLORS = listOf("W", "U", "B", "R", "G")

    /**
     * The key a `{C}` pip's requirement goes under beside the WUBRG letters ([faceRequirements],
     * [DraftsimCastability.Sources]). Colorless is a type of mana, not a colour (CR 106.1b), and
     * a `{C}` pip is paid only with it (CR 107.4c): no basic a deck adds makes it, only the
     * colorless sources it drafted. Never one of the build's colours, so no basic is ever made for it.
     */
    const val COLORLESS = "C"

    /**
     * The `{C}` pips of a cost — on its least demanding face, so a split card one half of which asks
     * none is cast off that half.
     */
    fun colorlessPips(cost: String): Int =
        faces(cost).minOf { face -> SYMBOL.findAll(face).count { it.groupValues[1].equals(COLORLESS, ignoreCase = true) } }

    /** The colorless sources a deck holding [cards] wants ([PipDemand.colorlessSourcesWanted]). */
    fun colorlessSourcesWanted(cards: Collection<ScorerCard>): Int =
        PipDemand.colorlessSourcesWanted(cards.maxOfOrNull { colorlessPips(it.manaCost) } ?: 0)

    private val SYMBOL = Regex("\\{([^}]+)}")

    /**
     * Converted mana cost of a single-face cost string. Numeric generic symbols add their value;
     * `{X}` adds 0; every other symbol (colored, hybrid, Phyrexian, snow) adds 1.
     */
    fun cmc(cost: String): Double {
        var total = 0.0
        for (m in SYMBOL.findAll(cost)) {
            val sym = m.groupValues[1]
            val n = sym.toIntOrNull()
            when {
                n != null -> total += n
                sym.equals("X", ignoreCase = true) -> {}
                else -> total += 1.0
            }
        }
        return total
    }

    /**
     * `colors(card)` (`jt`) for a cost string: the distinct WUBRG colors appearing in the cost,
     * counting both sides of every hybrid `{C/C2}` and the colored side of Phyrexian `{C/P}`.
     */
    fun colorsInCost(cost: String): List<String> {
        val found = LinkedHashSet<String>()
        for (sym in symbols(cost)) found += sym.colors
        return found.toList()
    }

    /** One mana symbol's classification: which WUBRG colors it references, and whether it's hybrid/Phyrexian. */
    data class CostSymbol(val colors: List<String>, val hybrid: Boolean, val phyrexian: Boolean)

    private fun symbols(cost: String): List<CostSymbol> =
        SYMBOL.findAll(cost).map { m ->
            val sym = m.groupValues[1].uppercase()
            CostSymbol(
                colors = COLORS.filter { sym.contains(it) },
                hybrid = sym.contains('/'),
                phyrexian = sym.contains("/P") || sym.contains("P/"),
            )
        }.toList()

    /**
     * `fitsColors(cost, allowed)` (`Te`): a hybrid is OK if any side is allowed; a plain colored
     * symbol must be allowed. Empty cost ⇒ true. Operates on the whole string (faces not split).
     */
    fun fitsColors(cost: String, allowed: Collection<String>): Boolean {
        val a = allowed.toSet()
        for (s in symbols(cost)) {
            if (s.hybrid) {
                if (s.colors.isNotEmpty() && s.colors.none { it in a }) return false
            } else {
                for (c in s.colors) if (c !in a) return false
            }
        }
        return true
    }

    /**
     * Whether a deck making [allowed] can pay the colored pips of an activated ability's [cost]: a
     * plain pip needs its colour, a hybrid either half; a Phyrexian pip (2 life) and a `{2/C}` pip
     * (two generic) always can. Mirrors [com.wingedsheep.ai.engine.ActivationMana.payable].
     */
    fun activationPayable(cost: String, allowed: Collection<String>): Boolean =
        SYMBOL.findAll(cost).all { m ->
            val sym = m.groupValues[1].uppercase()
            val colors = COLORS.filter { sym.contains(it) }
            colors.isEmpty() || sym.contains('P') || sym.any { it.isDigit() } || colors.any { it in allowed }
        }

    /** `offPipsInString(cost, A)` (`Uf`): hybrids with no allowed side + plain colored pips off A. */
    private fun offPipsInString(cost: String, allowed: Set<String>): Int {
        var n = 0
        for (s in symbols(cost)) {
            if (s.hybrid) { if (s.colors.isNotEmpty() && s.colors.none { it in allowed }) n++ }
            else for (c in s.colors) if (c !in allowed) n++
        }
        return n
    }

    /** `offColorsInString(cost, A)` (`XU`): the set of off-colors (both hybrid sides when both off). */
    private fun offColorsInString(cost: String, allowed: Set<String>): Set<String> {
        val out = LinkedHashSet<String>()
        for (s in symbols(cost)) {
            if (s.hybrid) { if (s.colors.isNotEmpty() && s.colors.none { it in allowed }) out += s.colors }
            else for (c in s.colors) if (c !in allowed) out += c
        }
        return out
    }

    private fun faces(cost: String): List<String> =
        if ("//" in cost) cost.split("//").map { it.trim() } else listOf(cost)

    /** `offPips(card, allowed)` (`ai`): min off-pips across faces. */
    fun offPips(cost: String, allowed: Collection<String>): Int {
        val a = allowed.toSet()
        return faces(cost).minOf { offPipsInString(it, a) }
    }

    /** `offColors(card, allowed)` (`dX`): off-colors of the min-off-pip face. */
    fun offColors(cost: String, allowed: Collection<String>): Set<String> {
        val a = allowed.toSet()
        return faces(cost).minByOrNull { offPipsInString(it, a) }?.let { offColorsInString(it, a) } ?: emptySet()
    }

    /** A card's colored-mana requirement for the manabase Monte-Carlo (`G4`). */
    data class CastRequirement(val plainPips: Map<String, Int>, val hybridGroups: List<List<String>>)

    /**
     * Colored requirements of a cost: plain colored pips by color, plus each hybrid/Phyrexian
     * symbol's color sides as a group (paid from whichever side has mana). Generic pips are ignored
     * — `G4` checks color availability, not total mana.
     */
    fun castRequirement(cost: String): CastRequirement {
        val plain = HashMap<String, Int>()
        val groups = mutableListOf<List<String>>()
        for (s in symbols(cost)) {
            if (s.hybrid) { if (s.colors.isNotEmpty()) groups += s.colors }
            else for (c in s.colors) plain[c] = (plain[c] ?: 0) + 1
        }
        return CastRequirement(plain, groups)
    }

    /**
     * Each face's coloured requirement, for the castability weight ([DraftsimCastability]): what
     * [castRequirement] reads, per face, less the symbols that never strand a spell — a Phyrexian pip
     * (payable with life) and a `{2/G}` (payable with generic mana) — plus its `{C}` pips under
     * [COLORLESS], which only a colorless source pays.
     */
    fun faceRequirements(cost: String): List<CastRequirement> = faces(cost).map { face ->
        val plain = HashMap<String, Int>()
        val groups = mutableListOf<List<String>>()
        for (m in SYMBOL.findAll(face)) {
            val raw = m.groupValues[1].uppercase()
            if (raw == COLORLESS) { plain[COLORLESS] = (plain[COLORLESS] ?: 0) + 1; continue }
            val s = symbols("{$raw}").single()
            if (s.phyrexian || s.colors.isEmpty() || raw.any { it.isDigit() }) continue
            if (s.hybrid) groups += s.colors else for (c in s.colors) plain[c] = (plain[c] ?: 0) + 1
        }
        CastRequirement(plain, groups)
    }

    /**
     * `CX(cost, allowed?)` pip counting (`SPEC_deckbuild.md` §2). Hybrid: one allowed side → +1 to it,
     * both → +0.5 each, neither → nothing; no `allowed` → +0.5 to each color side. Phyrexian → +0.5 to
     * its color. Plain colored → +1.
     */
    fun pipCounts(cost: String, allowed: Collection<String>? = null): Map<String, Double> {
        val a = allowed?.toSet()
        val out = HashMap<String, Double>()
        fun add(c: String, amt: Double) { out[c] = (out[c] ?: 0.0) + amt }
        for (s in symbols(cost)) {
            when {
                s.phyrexian -> s.colors.forEach { add(it, 0.5) }
                s.hybrid -> {
                    if (a == null) s.colors.forEach { add(it, 0.5) }
                    else {
                        val allowedSides = s.colors.filter { it in a }
                        when (allowedSides.size) {
                            0 -> {}
                            1 -> add(allowedSides[0], 1.0)
                            else -> allowedSides.forEach { add(it, 0.5) }
                        }
                    }
                }
                else -> s.colors.forEach { add(it, 1.0) }
            }
        }
        return out
    }
}
