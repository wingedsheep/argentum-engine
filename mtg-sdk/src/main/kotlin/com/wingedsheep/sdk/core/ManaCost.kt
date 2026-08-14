package com.wingedsheep.sdk.core

import com.wingedsheep.sdk.serialization.ManaCostStringSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ManaCostStringSerializer::class)
data class ManaCost(val symbols: List<ManaSymbol>) {

    val cmc: Int
        get() = symbols.sumOf { it.cmc }

    val colors: Set<Color>
        get() = symbols.flatMap { symbol ->
            when (symbol) {
                is ManaSymbol.Colored -> listOf(symbol.color)
                is ManaSymbol.Hybrid -> listOf(symbol.color1, symbol.color2)
                is ManaSymbol.Phyrexian -> listOf(symbol.color)
                is ManaSymbol.MonocolorHybrid -> listOf(symbol.color)
                else -> emptyList()
            }
        }.toSet()

    val colorCount: Map<Color, Int>
        get() = symbols
            .filterIsInstance<ManaSymbol.Colored>()
            .groupingBy { it.color }
            .eachCount()

    val genericAmount: Int
        get() = symbols
            .filterIsInstance<ManaSymbol.Generic>()
            .sumOf { it.amount }

    val colorlessAmount: Int
        get() = symbols.count { it is ManaSymbol.Colorless }

    val hasX: Boolean
        get() = symbols.any { it is ManaSymbol.X }

    val xCount: Int
        get() = symbols.count { it is ManaSymbol.X }

    fun isEmpty(): Boolean = symbols.isEmpty()

    /**
     * Substitute an announced value of X into this cost (CR 107.3a), turning each `{X}` symbol into
     * that much generic mana: `{X}{G}{G}` with X=2 becomes `{2}{G}{G}`, and `{X}{X}{R}` with X=3
     * becomes `{6}{R}`. A cost with no `{X}` is returned unchanged.
     *
     * The point is to hand downstream payment code a cost with no X left in it, so the ordinary
     * pool/solver path pays it without a separate X-spending pass.
     */
    fun withXAs(x: Int): ManaCost {
        if (!hasX) return this
        val xGeneric = x.coerceAtLeast(0) * xCount
        val withoutX = symbols.filterNot { it is ManaSymbol.X }
        val newGenericAmount = genericAmount + xGeneric
        val nonGeneric = withoutX.filterNot { it is ManaSymbol.Generic }
        return if (newGenericAmount > 0) {
            ManaCost(listOf(ManaSymbol.Generic(newGenericAmount)) + nonGeneric)
        } else {
            ManaCost(nonGeneric)
        }
    }

    /**
     * Reduce the generic mana portion of this cost by [amount].
     * Colored/hybrid/phyrexian symbols are unaffected.
     */
    fun reduceGeneric(amount: Int): ManaCost {
        if (amount <= 0) return this
        val coloredSymbols = symbols.filter { it !is ManaSymbol.Generic }
        val newGenericAmount = (genericAmount - amount).coerceAtLeast(0)
        return if (newGenericAmount > 0) {
            ManaCost(listOf(ManaSymbol.Generic(newGenericAmount)) + coloredSymbols)
        } else {
            ManaCost(coloredSymbols)
        }
    }

    /**
     * Increase the generic mana portion of this cost by [amount] (CR 601.2f / 602.2b — cost
     * increases are applied to the mana part of the cost). Colored/hybrid/Phyrexian symbols are
     * untouched, and a cost with no mana at all (`{T}:` abilities) gains the generic outright:
     * `{}` → `{2}`, `{1}{U}` → `{3}{U}`.
     */
    fun increaseGeneric(amount: Int): ManaCost {
        if (amount <= 0) return this
        val nonGeneric = symbols.filter { it !is ManaSymbol.Generic }
        return ManaCost(listOf(ManaSymbol.Generic(genericAmount + amount)) + nonGeneric)
    }

    /**
     * Reduce the generic portion by [amount], but never below a *total* of [minTotalMana] mana
     * (generic + every other mana symbol). Colored/hybrid/phyrexian/colorless symbols are never
     * removed; only generic mana is reduced, and only down to the point where the whole cost still
     * has at least [minTotalMana] mana symbols' worth.
     *
     * Power Artifact's "this effect can't reduce the mana in that cost to less than one mana"
     * (`minTotalMana = 1`): `{2}` → `{1}`, `{1}` → `{1}` (unchanged), `{3}{U}` → `{1}{U}`,
     * `{2}{U}` → `{U}` (already one mana), `{U}` → `{U}`. With `minTotalMana = 0` this is exactly
     * [reduceGeneric].
     */
    fun reduceGenericWithManaFloor(amount: Int, minTotalMana: Int): ManaCost {
        if (amount <= 0) return this
        if (minTotalMana <= 0) return reduceGeneric(amount)
        val nonGeneric = symbols.filter { it !is ManaSymbol.Generic }
        // Mana value contributed by the kept (non-generic) symbols — colored/hybrid/etc. each ≥ 1.
        val nonGenericMana = nonGeneric.sumOf { it.cmc }
        // Generic may drop to whatever keeps total mana >= minTotalMana, but not below 0.
        val genericFloor = (minTotalMana - nonGenericMana).coerceAtLeast(0)
        val newGenericAmount = (genericAmount - amount).coerceAtLeast(genericFloor)
        return if (newGenericAmount > 0) {
            ManaCost(listOf(ManaSymbol.Generic(newGenericAmount)) + nonGeneric)
        } else {
            ManaCost(nonGeneric)
        }
    }

    /**
     * Reduce this cost by every mana symbol in [reduction], following the general cost-reduction
     * rules of CR 118.7. This is the operation behind power-up (CR 702.193b) and offering
     * (CR 702.48c), both of which reduce a cost by another object's whole printed mana cost:
     * "Generic mana in the permanent's mana cost reduces generic mana in the cost to activate its
     * power-up ability. Colored and colorless mana in the permanent's mana cost reduces mana of the
     * same type, and any excess reduces that much generic mana."
     *
     * Unlike [reduceGeneric], which is the CR 118.7a generic-only reduction every other reduction
     * in the engine uses, this one is *pip-wise*: `{5}{W}{W}` − `{3}{W}{W}` = `{2}`, and
     * `{C}{W}{U}{B}{R}{G}` − `{R}{W}{B}` = `{C}{U}{G}` (Thanos).
     *
     * Matching runs in three tiers, so a reduction pip is always spent the most valuable way the
     * rules allow. Where CR 118.7e leaves the payer a free choice, the choice taken is the one that
     * reduces this cost the most — a rational payer's choice, and the only one that makes the
     * result independent of the order the pips happen to be printed in:
     *  1. **Exact symbol match** — `{R}` cancels `{R}`, `{C}` cancels `{C}`, `{R/G}` cancels
     *     `{R/G}` (Abomination: `{5}{R/G}{R/G}` − `{3}{R/G}` = `{2}{R/G}`).
     *  2. **Relaxed match** — a hybrid reduction pip pays one of its colored halves (CR 118.7e: the
     *     payer chooses a half as the reduction is applied) and a Phyrexian pip pays its color
     *     (CR 118.7f); conversely a colored pip may pay a Phyrexian pip of that color, which is a
     *     requirement for that same color. These pips remove one mana either way, so paying a cost
     *     pip always beats spilling — it clears a *color* requirement for the same one mana. Which
     *     pip each pays is a maximum bipartite matching rather than first-fit, since first-fit can
     *     let one pip take the only cost pip another could have paid: `{W}{U}` − `{W/U}{W/B}`
     *     cancels both only under the assignment `{W/U}`→`{U}`, `{W/B}`→`{W}`.
     *  3. **Spill to generic** — anything still unmatched reduces generic mana instead
     *     (CR 118.7b/c/d), one per pip, floored at zero. A monocolored hybrid ("twobrid") is the one
     *     pip whose two halves are worth different amounts, so it is decided last and by size: its
     *     generic half removes up to that half's number, its colored half removes exactly one, and
     *     it takes whichever is bigger (`{3}{W}` − `{2/W}` = `{1}{W}`, not `{3}`). Ties go to the
     *     colored half, which removes the same mana *and* a color requirement.
     *
     * `{X}` is inert on both sides: an `{X}` in [reduction] is a permanent's printed `{X}`, whose
     * value on the battlefield is 0 (CR 202.3b), and an `{X}` in this cost survives untouched so a
     * variable power-up still asks for X (Stature: `{X}{U}{U}` − `{U}` = `{X}{U}`).
     */
    fun subtract(reduction: ManaCost): ManaCost {
        if (reduction.isEmpty() || isEmpty()) return this
        // Non-generic symbols in printed order; `{X}` rides along and is never matched, so it
        // survives into the result exactly as [reduceGeneric] leaves it.
        val remaining = symbols.filterNot { it is ManaSymbol.Generic }.toMutableList()
        // CR 118.7a: generic in the reduction only ever reduces generic in the cost.
        var genericReduction = reduction.genericAmount

        // Tier 1 — exact matches, so an identical pip is never wasted paying something else.
        val unmatched = mutableListOf<ManaSymbol>()
        for (symbol in reduction.symbols) {
            if (symbol is ManaSymbol.Generic || symbol is ManaSymbol.X) continue
            val index = remaining.indexOfFirst { it == symbol }
            if (index >= 0) remaining.removeAt(index) else unmatched.add(symbol)
        }

        // Tier 2 — single-mana relaxed pips, assigned all at once so no pip strands another.
        val singlePips = unmatched.filterNot { it is ManaSymbol.MonocolorHybrid }
        val matchedIndices = maximumRelaxedMatching(singlePips, remaining)
        genericReduction += singlePips.size - matchedIndices.size
        for (index in matchedIndices.sortedDescending()) remaining.removeAt(index)

        // Tier 3 — monocolored hybrids, the one pip shape whose two halves differ in size. Compare
        // against the generic actually left to remove: spilling {2/W} into a cost with one generic
        // mana removes one, not two, and then the colored half is worth just as much.
        for (symbol in unmatched.filterIsInstance<ManaSymbol.MonocolorHybrid>()) {
            val costIndex = remaining.indexOfFirst { paysFor(symbol, it) }
            val spillGain = minOf(symbol.generic, (genericAmount - genericReduction).coerceAtLeast(0))
            if (costIndex >= 0 && spillGain <= 1) remaining.removeAt(costIndex)
            else genericReduction += symbol.generic
        }

        val newGeneric = (genericAmount - genericReduction).coerceAtLeast(0)
        val genericList = if (newGeneric > 0) listOf(ManaSymbol.Generic(newGeneric)) else emptyList()
        return ManaCost(genericList + remaining)
    }

    /**
     * The largest set of [costPips] indices that [reductionPips] can pay under [paysFor], one pip
     * each — a maximum bipartite matching by augmenting paths (Kuhn's algorithm). Both sides are a
     * handful of symbols, so the cubic worst case is irrelevant; what matters is that it is exact,
     * because a greedy first-fit answer would depend on printed order (see [subtract] tier 2).
     */
    private fun maximumRelaxedMatching(
        reductionPips: List<ManaSymbol>,
        costPips: List<ManaSymbol>
    ): List<Int> {
        val payerOfCostPip = IntArray(costPips.size) { -1 }

        fun assign(payer: Int, tried: BooleanArray): Boolean {
            for (costIndex in costPips.indices) {
                if (tried[costIndex] || !paysFor(reductionPips[payer], costPips[costIndex])) continue
                tried[costIndex] = true
                // Free, or its current payer can be re-seated somewhere else.
                if (payerOfCostPip[costIndex] == -1 || assign(payerOfCostPip[costIndex], tried)) {
                    payerOfCostPip[costIndex] = payer
                    return true
                }
            }
            return false
        }

        for (payer in reductionPips.indices) assign(payer, BooleanArray(costPips.size))
        return payerOfCostPip.indices.filter { payerOfCostPip[it] != -1 }
    }

    /**
     * Whether a [reductionSymbol] that found no identical pip may still pay [costSymbol] — the
     * relaxed tier of [subtract]. A hybrid pays either colored half (CR 118.7e) and a Phyrexian
     * pays its color (CR 118.7f); a colored pip pays a Phyrexian pip of the same color, since that
     * pip is a requirement for that color. Colorless and generic never relax — they spill to
     * generic instead (CR 118.7a/d).
     */
    private fun paysFor(reductionSymbol: ManaSymbol, costSymbol: ManaSymbol): Boolean =
        when (reductionSymbol) {
            is ManaSymbol.Hybrid -> costSymbol is ManaSymbol.Colored &&
                (costSymbol.color == reductionSymbol.color1 || costSymbol.color == reductionSymbol.color2)
            is ManaSymbol.Phyrexian -> costSymbol is ManaSymbol.Colored && costSymbol.color == reductionSymbol.color
            is ManaSymbol.MonocolorHybrid -> costSymbol is ManaSymbol.Colored && costSymbol.color == reductionSymbol.color
            is ManaSymbol.Colored -> costSymbol is ManaSymbol.Phyrexian && costSymbol.color == reductionSymbol.color
            else -> false
        }

    /**
     * Reduce this cost by the maximum convoke contribution from the given creatures.
     * Each creature pays for one colored symbol matching its color, or one generic mana.
     * Colored symbols are matched greedily first, then remaining creatures pay generic.
     */
    fun reduceByConvoke(creatureColors: List<Set<Color>>): ManaCost {
        if (creatureColors.isEmpty()) return this
        val remainingColored = symbols.filter { it is ManaSymbol.Colored }.toMutableList()
        var genericReduction = 0

        for (colors in creatureColors) {
            // Try to match a colored symbol first
            val matchIndex = remainingColored.indexOfFirst { symbol ->
                symbol is ManaSymbol.Colored && symbol.color in colors
            }
            if (matchIndex >= 0) {
                remainingColored.removeAt(matchIndex)
            } else {
                // Pay generic
                genericReduction++
            }
        }

        val otherSymbols = symbols.filter { it !is ManaSymbol.Generic && it !is ManaSymbol.Colored }
        val newGenericAmount = (genericAmount - genericReduction).coerceAtLeast(0)
        val genericSymbols = if (newGenericAmount > 0) listOf(ManaSymbol.Generic(newGenericAmount)) else emptyList()
        return ManaCost(genericSymbols + remainingColored + otherSymbols)
    }

    /**
     * Return a relaxed cost where every colored, hybrid, phyrexian, and colorless requirement
     * is converted into generic mana — suitable for "mana of any type can be spent" effects
     * (e.g. Taster of Wares, Cruelclaw's Heist).
     *
     * Generic, X, and existing generic-equivalent symbols are preserved; only color/type
     * requirements are dropped.
     */
    fun relaxColors(): ManaCost {
        if (symbols.none { it is ManaSymbol.Colored || it is ManaSymbol.Hybrid ||
                it is ManaSymbol.Phyrexian || it is ManaSymbol.Colorless ||
                it is ManaSymbol.MonocolorHybrid }) return this
        var addedGeneric = 0
        val keptSymbols = mutableListOf<ManaSymbol>()
        for (symbol in symbols) {
            when (symbol) {
                // Each of these is a single-mana requirement once colors are relaxed: a
                // monocolored hybrid's colored side ({B} in {2/B}) is 1 mana of any type.
                is ManaSymbol.Colored, is ManaSymbol.Hybrid, is ManaSymbol.Phyrexian,
                is ManaSymbol.Colorless, is ManaSymbol.MonocolorHybrid -> addedGeneric++
                is ManaSymbol.Generic, is ManaSymbol.X -> keptSymbols.add(symbol)
            }
        }
        val existingGeneric = keptSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        val nonGenericKept = keptSymbols.filterNot { it is ManaSymbol.Generic }
        val totalGeneric = existingGeneric + addedGeneric
        val genericList = if (totalGeneric > 0) listOf(ManaSymbol.Generic(totalGeneric)) else emptyList()
        return ManaCost(genericList + nonGenericKept)
    }

    operator fun plus(other: ManaCost): ManaCost {
        val mergedGeneric = this.genericAmount + other.genericAmount
        val nonGeneric = this.symbols.filterNot { it is ManaSymbol.Generic } +
            other.symbols.filterNot { it is ManaSymbol.Generic }
        val genericSymbol = if (mergedGeneric > 0) listOf(ManaSymbol.generic(mergedGeneric)) else emptyList()
        return ManaCost(genericSymbol + nonGeneric)
    }

    /**
     * Repeat this cost [n] times — `{1}{R} * 3` = `{3}{R}{R}{R}`. Used to test whether an
     * activated ability with a fixed mana cost can be paid N times in a row (color-aware),
     * rather than naively dividing total mana by CMC.
     */
    operator fun times(n: Int): ManaCost {
        require(n >= 0) { "Cannot multiply a mana cost by a negative number: $n" }
        if (n == 0) return ZERO
        return (1 until n).fold(this) { acc, _ -> acc + this }
    }

    override fun toString(): String = symbols.joinToString("")

    companion object {
        val ZERO = ManaCost(emptyList())

        fun parse(costString: String): ManaCost {
            if (costString.isBlank()) return ZERO

            val symbols = mutableListOf<ManaSymbol>()
            val regex = Regex("""\{([^}]+)}""")

            regex.findAll(costString).forEach { match ->
                val content = match.groupValues[1]
                val symbol = when {
                    content == "W" -> ManaSymbol.W
                    content == "U" -> ManaSymbol.U
                    content == "B" -> ManaSymbol.B
                    content == "R" -> ManaSymbol.R
                    content == "G" -> ManaSymbol.G
                    content == "C" -> ManaSymbol.C
                    content == "X" -> ManaSymbol.X
                    content.toIntOrNull() != null -> ManaSymbol.generic(content.toInt())
                    // Monocolored hybrid ("twobrid") mana: {2/B}, {2/G}, etc. — pay generic OR
                    // one mana of the color. Must precede the two-color hybrid branch below, which
                    // would otherwise try to read a color from the leading digit and fail.
                    content.contains("/") && content.substringBefore("/").toIntOrNull() != null -> {
                        val generic = content.substringBefore("/").toInt()
                        val color = content.substringAfter("/").firstOrNull()?.let { Color.fromSymbol(it) }
                        if (color != null) {
                            ManaSymbol.MonocolorHybrid(generic, color)
                        } else {
                            throw IllegalArgumentException("Unknown monocolored hybrid mana symbol: {$content}")
                        }
                    }
                    // Hybrid mana: {W/U}, {G/U}, etc.
                    content.contains("/") && !content.contains("P") -> {
                        val parts = content.split("/")
                        if (parts.size == 2) {
                            val color1 = Color.fromSymbol(parts[0][0])
                            val color2 = Color.fromSymbol(parts[1][0])
                            if (color1 != null && color2 != null) {
                                ManaSymbol.Hybrid(color1, color2)
                            } else {
                                throw IllegalArgumentException("Unknown hybrid mana symbol: {$content}")
                            }
                        } else {
                            throw IllegalArgumentException("Unknown mana symbol: {$content}")
                        }
                    }
                    // Phyrexian mana: {W/P}, {G/P}, etc.
                    content.contains("/P") -> {
                        val colorPart = content.substringBefore("/P")
                        val color = Color.fromSymbol(colorPart[0])
                        if (color != null) {
                            ManaSymbol.Phyrexian(color)
                        } else {
                            throw IllegalArgumentException("Unknown Phyrexian mana symbol: {$content}")
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown mana symbol: {$content}")
                }
                symbols.add(symbol)
            }

            return ManaCost(symbols)
        }

        fun of(vararg symbols: ManaSymbol): ManaCost = ManaCost(symbols.toList())
    }
}
