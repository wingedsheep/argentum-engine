package com.wingedsheep.ai.draftsim

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * How reliably a finished deck can cast each of its spells on time, from its manabase — the cost the
 * final deck score charges a deck for every colour beyond its first two.
 *
 * A spell's chance is the hypergeometric chance of holding the coloured sources its pips need among
 * the cards seen by the turn its mana value says it wants to be cast (seven plus one a turn, on the
 * play), each colour taken independently. Its **weight** is that chance against the same spell's on a
 * main colour's [MAIN_SOURCES_PER_17] sources, capped at 1: a two-colour deck's spells lose next to
 * nothing, a splashed six-drop off four sources keeps about three quarters of its value, and a
 * three-colour deck on six sources a colour pays on every spell — so a third colour is worth it only
 * when the cards it adds, or the fixing that carries it, outweigh that.
 *
 * Sources: a land counts once for each colour it fixes ([DraftsimCardOps.fixingOf] — a dual for two,
 * Evolving Wilds for every colour), a nonland fixer half ([DraftsimCardOps.NONLAND_FIXER_WEIGHT]).
 */
internal class DraftsimCastability(private val ops: DraftsimCardOps) {

    /**
     * Coloured sources per colour (WUBRG letters) among [deck]'s lands and nonland fixers. A land that
     * fetches a basic counts toward a colour only up to the deck's basics of it: three Riveteers
     * Overlooks beside one Forest are one Forest's worth of green, not four.
     */
    fun sources(deck: List<ScorerCard>): Map<String, Double> {
        val out = HashMap<String, Double>()
        val fetched = HashMap<String, Int>()
        val basics = HashMap<String, Int>()
        for (card in deck) {
            val land = ops.isLand(card)
            val fix = ops.fixingOf(card).toSet()
            when {
                ops.isBasic(card) -> fix.forEach { basics[it] = (basics[it] ?: 0) + 1 }
                land && card.fetchesBasic -> { fix.forEach { fetched[it] = (fetched[it] ?: 0) + 1 }; continue }
                !land && fix.size < 2 -> continue
            }
            val w = if (land) 1.0 else DraftsimCardOps.NONLAND_FIXER_WEIGHT
            for (c in fix) out[c] = (out[c] ?: 0.0) + w
        }
        for ((c, n) in fetched) out[c] = (out[c] ?: 0.0) + min(n, basics[c] ?: 0)
        return out
    }

    /**
     * [card]'s cast chance in a [deckSize]-card deck of [landCount] lands with [sources], against the
     * same card on main-colour sources; in 0–1. A face-down or split card takes its easiest face.
     */
    fun weight(card: ScorerCard, sources: Map<String, Double>, deckSize: Int, landCount: Int): Double {
        val reference = MAIN_SOURCES_PER_17 * landCount / 17.0
        val seen = min(deckSize, OPENING_HAND + turnFor(card.cmc) - 1)
        return DraftsimMana.faceRequirements(card.manaCost).maxOfOrNull { req ->
            val actual = chance(req, { sources[it] ?: 0.0 }, deckSize, seen)
            val ideal = chance(req, { maxOf(reference, sources[it] ?: 0.0) }, deckSize, seen)
            if (ideal <= 0.0) 1.0 else min(1.0, actual / ideal)
        } ?: 1.0
    }

    private fun chance(req: DraftsimMana.CastRequirement, sourcesOf: (String) -> Double, deckSize: Int, seen: Int): Double {
        var p = 1.0
        for ((color, pips) in req.plainPips) p *= atLeast(pips, sourcesOf(color), deckSize, seen)
        for (group in req.hybridGroups) p *= group.maxOf { atLeast(1, sourcesOf(it), deckSize, seen) }
        return p
    }

    /** P(at least [k] of [sources] among [seen] cards of [deckSize]); fractional sources interpolate. */
    private fun atLeast(k: Int, sources: Double, deckSize: Int, seen: Int): Double {
        val lo = floor(sources).toInt().coerceIn(0, deckSize)
        val hi = ceil(sources).toInt().coerceIn(0, deckSize)
        val pLo = hypergeometricAtLeast(k, lo, deckSize, seen)
        if (hi == lo) return pLo
        return pLo + (sources - lo) * (hypergeometricAtLeast(k, hi, deckSize, seen) - pLo)
    }

    private fun hypergeometricAtLeast(k: Int, successes: Int, population: Int, draws: Int): Double {
        if (k <= 0) return 1.0
        if (successes < k) return 0.0
        // P(X < k) summed term by term: P(X = i) = C(K,i) C(N-K, n-i) / C(N, n).
        var below = 0.0
        for (i in 0 until k) below += exactly(i, successes, population, draws)
        return (1.0 - below).coerceIn(0.0, 1.0)
    }

    private fun exactly(i: Int, successes: Int, population: Int, draws: Int): Double {
        if (i > draws || draws - i > population - successes) return 0.0
        return Math.exp(lnChoose(successes, i) + lnChoose(population - successes, draws - i) - lnChoose(population, draws))
    }

    private fun lnChoose(n: Int, k: Int): Double {
        if (k < 0 || k > n) return Double.NEGATIVE_INFINITY
        var s = 0.0
        for (j in 0 until k) s += Math.log((n - j).toDouble()) - Math.log((j + 1).toDouble())
        return s
    }

    /** The turn a spell of mana value [cmc] wants to be cast: its mana value, from turn 1 to turn 7. */
    private fun turnFor(cmc: Double): Int = ceil(cmc).toInt().coerceIn(1, LATEST_TURN)

    companion object {
        /** Sources a main colour of a limited deck runs per 17 lands — the usual nine of a 9/8 split. */
        const val MAIN_SOURCES_PER_17 = 9.0

        private const val OPENING_HAND = 7

        /** Past this a spell is cast whenever it is drawn with the mana; seven-drops and up wait like it. */
        private const val LATEST_TURN = 7
    }
}
