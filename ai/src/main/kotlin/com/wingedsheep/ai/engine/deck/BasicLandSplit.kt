package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition

/**
 * How a generated deck divides its basic lands between its colours. Shared by the Commander and
 * constructed generators, which both build a mostly-basic manabase for a fixed set of colours.
 */
internal object BasicLandSplit {

    val BASIC_FOR: Map<Color, String> = mapOf(
        Color.WHITE to "Plains",
        Color.BLUE to "Island",
        Color.BLACK to "Swamp",
        Color.RED to "Mountain",
        Color.GREEN to "Forest",
    )

    /**
     * Split [count] basic lands across [colours], weighted by the coloured pips [cards] ask for (a
     * card listed twice counts twice).
     *
     * Every colour gets a floor of [minColourShare] of the basics before the pips divide the rest:
     * a pip count is a noisy read of a spell suite, and a colour with one double-pip card still has
     * to be castable (2026-10-31: a three-colour Commander deck ran 10 of its 38 basics in the
     * colour of its double-pip six-drops). Commander's CR 903.5d is why only [colours] appear.
     *
     * [floors] then raises a colour to the sources its cards need, counting [fixedSources] (the
     * non-basic lands that already make it), by taking basics from a colour with sources to spare:
     * the pip share alone left a constructed deck's four one-pip black three-drops on five Swamps.
     */
    fun split(
        cards: List<CardDefinition>,
        colours: Set<Color>,
        count: Int,
        minColourShare: Double,
        floors: Map<Color, Int> = emptyMap(),
        fixedSources: (Color) -> Int = { 0 },
    ): Map<String, Int> {
        if (count <= 0 || colours.isEmpty()) return emptyMap()
        val ordered = colours.sortedBy { it.ordinal }
        val pips = ordered.associateWith { color -> cards.sumOf { it.manaCost.colorCount[color] ?: 0 } }
        val totalPips = pips.values.sum()

        val floor = minOf((count * minColourShare / ordered.size).toInt().coerceAtLeast(1), count / ordered.size)
        val shares = ordered.associateWith { floor }.toMutableMap()
        val rest = count - floor * ordered.size
        // Largest-remainder split of what's left, so the counts sum to exactly `count`.
        val exact = ordered.associateWith { color ->
            if (totalPips == 0) rest.toDouble() / ordered.size else rest * pips.getValue(color).toDouble() / totalPips
        }
        exact.forEach { (color, share) -> shares[color] = shares.getValue(color) + share.toInt() }
        val leftover = count - shares.values.sum()
        ordered.sortedByDescending { exact.getValue(it) - exact.getValue(it).toInt() }
            .take(leftover)
            .forEach { shares[it] = shares.getValue(it) + 1 }

        // Raise each colour towards its source floor, hungriest first, off a colour with sources
        // to spare above its own — the same rule [com.wingedsheep.ai.engine.PipDemand.allocate]
        // applies to limited decks.
        fun surplus(color: Color) = shares.getValue(color) + fixedSources(color) - (floors[color] ?: 0)
        for (color in ordered.sortedBy { surplus(it) }) {
            while (surplus(color) < 0) {
                val donor = ordered.filter { it != color && shares.getValue(it) > 0 && surplus(it) > 0 }
                    .maxByOrNull { surplus(it) } ?: break
                shares[donor] = shares.getValue(donor) - 1
                shares[color] = shares.getValue(color) + 1
            }
        }

        return ordered.associate { BASIC_FOR.getValue(it) to shares.getValue(it) }.filterValues { it > 0 }
    }
}
