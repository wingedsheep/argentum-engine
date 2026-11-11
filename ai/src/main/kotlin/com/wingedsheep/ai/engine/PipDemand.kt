package com.wingedsheep.ai.engine

import kotlin.math.ceil
import kotlin.math.floor

/**
 * How a limited deck's colored mana demand turns into basic lands — shared by the heuristic builder
 * and the Draftsim autobuilder's manabase.
 *
 * Splitting basics by raw pip count treats `{G}` on a six-drop like `{G}{G}{G}` on a four-drop, and
 * put a deck with Squirrel Girl ({1}{G}{G}{G}) on 3 Forests, or Countersculpt ({U}{U}) on 6 Islands
 * (2026-10-15 games c71b39d3, 32b8e508). Two corrections:
 *
 *  - **[weight]**: a card's demand for a color grows faster than its pip count — two of a color
 *    need far more sources than one, three more again — and early drops weigh more, because a
 *    two-drop stuck in hand on turn two costs more than a six-drop stuck until turn seven.
 *  - **[sourcesNeeded]**: the most demanding card in a color sets a *floor* on that color's sources
 *    (a 17-land deck wants about 6 for a single pip, 8 for a double, 10 for a triple, in the spirit
 *    of the usual limited source tables), which [allocate] meets by moving basics off a color with
 *    room above its own floor.
 */
internal object PipDemand {

    /** The 40-card limited land count the [sourcesNeeded] table is written for. */
    private const val REFERENCE_LANDS = 17.0

    /** Basic-land demand of one card holding [pips] pips of a color, at mana value [manaValue]. */
    fun weight(pips: Double, manaValue: Double): Double {
        if (pips <= 0.0) return 0.0
        val intensity = pips + when {
            pips >= 3.0 -> 2.5
            pips >= 2.0 -> 1.0
            else -> 0.0
        }
        val early = when {
            manaValue <= 2.0 -> 1.5
            manaValue <= 3.0 -> 1.25
            else -> 1.0
        }
        return intensity * early
    }

    /**
     * The fewest colorless lands (Wastes, a colorless utility land) a pool needs before its `{C}`
     * spells are worth a slot. A `{C}` pip is paid only with colorless mana (CR 107.4c), and no
     * basic a Limited deck adds makes any — Wastes is played only as opened or drafted.
     */
    const val MIN_COLORLESS_SOURCES = 3

    /** Colorless sources a deck plays beyond its hungriest `{C}` card's pips: the three a splash colour gets. */
    const val COLORLESS_EXTRA_SOURCES = 3

    /**
     * The colorless sources a deck wants when its hungriest card holds [pips] `{C}` pips: those pips
     * plus [COLORLESS_EXTRA_SOURCES], as a splash colour gets; 0 when no card has one.
     */
    fun colorlessSourcesWanted(pips: Int): Int = if (pips > 0) pips + COLORLESS_EXTRA_SOURCES else 0

    /** Sources of one color a deck of [landCount] lands wants for a card with [pips] pips of it. */
    fun sourcesNeeded(pips: Double, landCount: Int): Int {
        val base = when {
            pips >= 3.0 -> 10.0
            pips >= 2.0 -> 8.0
            pips > 0.0 -> 6.0
            else -> return 0
        }
        return ceil(base * landCount / REFERENCE_LANDS).toInt()
    }

    /**
     * Split [count] basics across [demand]'s colors in proportion to it (largest remainder), then
     * raise each color — most demanded first — towards its [floors] entry, counting [fixedSources]
     * (non-basic lands that already make it). A donor never drops below its own floor, so when the
     * floors can't all be met the split stays as close to proportional as they allow; callers that
     * care check [shortfall].
     */
    fun <K> allocate(
        demand: Map<K, Double>,
        count: Int,
        floors: Map<K, Int> = emptyMap(),
        fixedSources: (K) -> Int = { 0 },
    ): MutableMap<K, Int> {
        val out = LinkedHashMap<K, Int>()
        val entries = demand.filterValues { it > 0 }.entries.sortedByDescending { it.value }
        if (entries.isEmpty() || count <= 0) return out
        val total = entries.sumOf { it.value }
        var assigned = 0
        val fractional = mutableListOf<Pair<K, Double>>()
        for ((color, d) in entries) {
            val exact = d / total * count
            val whole = floor(exact).toInt()
            out[color] = whole
            assigned += whole
            fractional += color to (exact - whole)
        }
        var remainder = count - assigned
        for ((color, _) in fractional.sortedByDescending { it.second }) {
            if (remainder <= 0) break
            out[color] = (out[color] ?: 0) + 1
            remainder--
        }

        fun surplus(color: K) = (out[color] ?: 0) + fixedSources(color) - (floors[color] ?: 0)
        for ((color, _) in entries) {
            while (surplus(color) < 0) {
                val donor = out.keys.filter { it != color && (out[it] ?: 0) > 0 && surplus(it) > 0 }
                    .maxByOrNull { surplus(it) } ?: break
                out[donor] = out.getValue(donor) - 1
                out[color] = (out[color] ?: 0) + 1
            }
        }
        return out
    }

    /** Sources [allocation] leaves short of [floors], summed over colors (0 = every floor met). */
    fun <K> shortfall(allocation: Map<K, Int>, floors: Map<K, Int>, fixedSources: (K) -> Int = { 0 }): Int =
        floors.entries.sumOf { (color, floor) -> (floor - (allocation[color] ?: 0) - fixedSources(color)).coerceAtLeast(0) }
}
