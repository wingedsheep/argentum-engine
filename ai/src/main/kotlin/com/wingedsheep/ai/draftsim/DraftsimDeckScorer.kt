package com.wingedsheep.ai.draftsim

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** A scored archetype/colour hypothesis (`kf` entry / `c` output). */
data class DraftsimArchetypeRank(
    val name: String,
    val score: Double,
    val colors: List<String>,
    val cardCount: Int,
    val qualityScore: Double,
    val curveScore: Double,
    val bombScore: Double,
    val synergyScore: Double,
    val removalScore: Double,
    val manaScore: Double,
)

/** Final deck score (`Mm` output): the 0–10 value `dW` sorts on, plus the manabase playability. */
data class DraftsimDeckScore(val score: Double, val manaBaseScore: Double)

/**
 * Layer-1 archetype ranking (`kf`) and the 0–10 final deck score (`Mm`/`Wm`/`G4`), ported from
 * `SPEC_archetype_and_score.md`. Bound to one pool's [tables]; reuses [DraftsimCardOps] for the
 * shared card primitives so the join/classification rules stay single-sourced.
 *
 * Two sub-score producers share the six-sub-score formulas and must agree: `c(colors, arch?)` over a
 * *hypothetical* deck (all pool cards castable in `colors`) for ranking, and `Im` over an *actual*
 * chosen deck for the final score.
 */
class DraftsimDeckScorer(private val tables: DraftsimSetTables) {

    private val ops = DraftsimCardOps(tables)

    private companion object {
        const val BOMB = 3.9
        const val SET_BASELINE = 2.0   // sW: sX is empty in the bundle ⇒ B = 2 always.

        val TWO_COLOR_PAIRS = listOf("WU", "UB", "BR", "RG", "GW", "WB", "UR", "BG", "RW", "GU")
        // The ten three-color "good stuff" shells (cW guild labels), shared with the builder.
        val THREE_COLOR get() = DraftsimDeckScorerGuilds.THREE_COLOR

        // Curve model lW rows: [cmc, low, high, maxBonus, weight].
        val CURVE_ROWS = listOf(
            Curve(1, 1.0, 3.0, 2.0, 0.5), Curve(2, 6.0, 8.0, 2.0, 0.5), Curve(3, 5.0, 7.0, 2.0, 0.5),
            Curve(4, 3.0, 5.0, 2.0, 0.4), Curve(5, 2.0, 3.0, 1.0, 0.4), Curve(6, 1.0, 2.0, 1.0, 0.5),
        )

        // Baked-in population mean / two-sigma (ba.*).
        val BA_QUALITY = Stat(11.54, 4.56)
        val BA_BOMB = Stat(2.61, 4.06)
        val BA_SYNERGY = Stat(5.00, 4.83)
        val BA_MANABASE = Stat(5.42, 2.83)
    }

    private data class Curve(val cmc: Int, val low: Double, val high: Double, val maxBonus: Double, val weight: Double)
    private data class Stat(val mean: Double, val twoSigma: Double)

    // =========================================================================
    // §1 curve model + §2 six sub-scores
    // =========================================================================

    /** `lW(buckets, fill)` — curve fit, 0–10. */
    private fun curveScore(buckets: Map<Int, Int>, fill: Double): Double {
        var n = 0.0
        for (row in CURVE_ROWS) {
            val lo = row.low * fill
            val hi = row.high * fill
            val count = (buckets[row.cmc] ?: 0).toDouble()
            var f = row.maxBonus
            if (count < lo) f -= (lo - count) * row.weight
            else if (count > hi) f -= (count - hi) * row.weight
            n += max(0.0, f)
        }
        return n.coerceIn(0.0, 10.0)
    }

    private data class SubScores(
        val quality: Double, val count: Int, val curveScore: Double, val bombScore: Double,
        val synergyScore: Double, val removalScore: Double,
    )

    /** The six sub-scores over a nonland card set, given pre-counted enabler/payoff totals. */
    private fun subScores(cards: List<ScorerCard>, enablers: Int, payoffs: Int): SubScores {
        val b = SET_BASELINE
        var quality = 0.0
        var bombs = 0
        var removalN = 0
        val buckets = HashMap<Int, Int>()
        for (card in cards) {
            quality += max(0.0, (ops.rating(card.name) ?: b) - b)
            if ((ops.rating(card.name) ?: b) >= BOMB) bombs++          // Wt ?? B, NOT ratingOrDefault
            if (card.name.lowercase() in tables.removal) removalN++
            val bucket = ops.cmcBucket(card.cmc)
            if (bucket >= 1) buckets[bucket] = (buckets[bucket] ?: 0) + 1
        }
        val count = cards.size
        val fill = min(count / 23.0, 1.0)
        val expected = 6 * fill
        val removalScore = when {
            fill < 0.15 -> if (removalN > 0) 2.0 else 0.0
            removalN >= expected * 1.15 -> 6.0
            removalN >= expected * 0.8 -> 4.0
            removalN >= expected * 0.5 -> 2.0
            else -> 0.0
        }
        return SubScores(quality, count, curveScore(buckets, fill), bombs * 1.5, min(enablers, payoffs).toDouble(), removalScore)
    }

    /** Mana penalty for 3+ color hypotheses (kf only). */
    private fun manaPenalty(colors: List<String>, poolLands: List<ScorerCard>): Double {
        if (colors.size < 3) return 0.0
        val colorSet = colors.toSet()
        val fixers = poolLands.count { land ->
            val fix = ops.archRecord(land.name)?.fixing?.takeIf { it.isNotEmpty() }
                ?: ops.colorsOf(land).ifEmpty { land.colorIdentity }
            fix.size >= 4 || fix.count { it in colorSet } >= 2
        }
        return when {
            colors.size == 3 && fixers < 3 -> -5.0
            colors.size == 4 && fixers < 6 -> -10.0
            colors.size >= 5 && fixers < 7 -> -15.0
            else -> 0.0
        }
    }

    // =========================================================================
    // §3 kf — archetype ranking
    // =========================================================================

    /** kf's `c(colors, archName?)`: score a hypothetical deck = pool nonland castable in `colors`. */
    private fun scoreHypothesis(pool: List<ScorerCard>, colors: List<String>, archName: String?): DraftsimArchetypeRank {
        val s = pool.filter { !ops.isLand(it) && DraftsimMana.fitsColors(it.manaCost, colors) }
        var enablers = 0
        var payoffs = 0
        if (archName != null) for (card in s) {
            for (tag in ops.archRecord(card.name)?.archetypes.orEmpty()) if (tag.archetype == archName) {
                when (tag.role) { "enabler" -> enablers++; "payoff" -> payoffs++ }
            }
        }
        val sub = subScores(s, enablers, payoffs)
        val mana = manaPenalty(colors, pool.filter { ops.isLand(it) && !ops.isBasic(it) })
        val qualityScore = sub.quality / 3.0
        val score = round1((qualityScore + sub.curveScore + sub.bombScore + sub.synergyScore + sub.removalScore + mana))
        return DraftsimArchetypeRank(
            name = archName ?: colors.joinToString(""), score = score, colors = colors, cardCount = s.size,
            qualityScore = round1(qualityScore), curveScore = sub.curveScore, bombScore = sub.bombScore,
            synergyScore = sub.synergyScore, removalScore = sub.removalScore, manaScore = mana,
        )
    }

    /** `kf(pool, archColors)` → archetype descriptors sorted by score desc. */
    fun rankArchetypes(pool: List<ScorerCard>, archColors: Map<String, List<String>>): List<DraftsimArchetypeRank> {
        val nonland = pool.filter { !ops.isLand(it) }
        val goodStuff = rankShells(pool).take(2).map { it.copy(name = "${it.name} good stuff") }

        // The plain pairs are always in the running. A set's named archetypes only cover the pairs
        // its pool happens to tag, and an archetype's colours are inferred from those tags — so with
        // only named hypotheses a pool can be left with no good two-colour option to build at all.
        val pairs = TWO_COLOR_PAIRS.map { scoreHypothesis(pool, it.map { c -> c.toString() }, null).copy(name = it) }

        if (tables.archetypes.isNotEmpty()) {
            val archNames = nonland.flatMap { ops.archRecord(it.name)?.archetypes.orEmpty().map { t -> t.archetype } }.toSet()
            val named = archNames.map { name ->
                val cols = archColors[name] ?: colorsOnAtLeastTwo(nonland, name)
                scoreHypothesis(pool, cols, name).copy(name = name, colors = cols)
            }
            return (named + pairs + goodStuff).sortedByDescending { it.score }
        }
        return (pairs + goodStuff).sortedByDescending { it.score }
    }

    /**
     * Every three-colour shell scored as a hypothesis, best first and named for its guild. The score
     * carries the 3+ colour mana penalty, so a shell the pool can't fix reads [DraftsimArchetypeRank.manaScore] < 0.
     */
    fun rankShells(pool: List<ScorerCard>): List<DraftsimArchetypeRank> = THREE_COLOR
        .map { (combo, guild) -> scoreHypothesis(pool, combo.map { it.toString() }, null).copy(name = guild) }
        .sortedByDescending { it.score }

    /** `C2`: colors appearing on ≥2 of an archetype's tagged cards. */
    private fun colorsOnAtLeastTwo(nonland: List<ScorerCard>, archName: String): List<String> {
        val perColor = HashMap<String, Int>()
        for (card in nonland) if (ops.archRecord(card.name)?.archetypes?.any { it.archetype == archName } == true)
            for (c in ops.colorsOf(card)) perColor[c] = (perColor[c] ?: 0) + 1
        return perColor.filterValues { it >= 2 }.keys.toList()
    }

    // =========================================================================
    // §4–§5 final deck score Mm → Wm
    // =========================================================================

    /** `Im` over the chosen deck's nonland cards (archetype-agnostic enabler/payoff counts). */
    private fun realDeckSubScores(deckCards: List<ScorerCard>): SubScores =
        subScoresAgnostic(deckCards.filter { !ops.isLand(it) })

    /** `Mm(deckCards)` → final 0–10 score + manabase playability. */
    fun scoreDeck(deckCards: List<ScorerCard>): DraftsimDeckScore {
        val o = realDeckSubScores(deckCards)
        val removalInDeck = deckCards.filter { !ops.isLand(it) && it.name.lowercase() in tables.removal }
        val avgRating = if (removalInDeck.isEmpty()) 0.0
            else removalInDeck.sumOf { ops.rating(it.name) ?: 2.0 } / removalInDeck.size
        val removalScore = gm(removalInDeck.size, avgRating)
        val manaBaseScore = manabasePlayability(deckCards)
        val score = wm(o.quality, o.curveScore, o.bombScore, o.synergyScore, removalScore, manaBaseScore, o.count)
        return DraftsimDeckScore(score, manaBaseScore)
    }

    /**
     * `tk(nonlandSet)` — the final deck score with the manabase **fixed at its population mean**
     * (the refiner [DraftsimDeckBuilder] hill-climbs on the nonland set without recomputing the
     * manabase every iteration).
     */
    fun scoreNonlandSet(nonland: List<ScorerCard>): Double {
        val o = subScoresAgnostic(nonland)
        val removal = nonland.filter { it.name.lowercase() in tables.removal }
        val avg = if (removal.isEmpty()) 0.0 else removal.sumOf { ops.rating(it.name) ?: 2.0 } / removal.size
        val removalScore = gm(removal.size, avg)
        return wm(o.quality, o.curveScore, o.bombScore, o.synergyScore, removalScore, BA_MANABASE.mean, o.count)
    }

    private fun subScoresAgnostic(nonland: List<ScorerCard>): SubScores {
        var enablers = 0
        var payoffs = 0
        for (card in nonland) for (tag in ops.archRecord(card.name)?.archetypes.orEmpty()) {
            when (tag.role) { "enabler" -> enablers++; "payoff" -> payoffs++ }
        }
        return subScores(nonland, enablers, payoffs)
    }

    /** `Wm` — normalize each sub-score to 0–10 and average. */
    private fun wm(quality: Double, curve: Double, bomb: Double, synergy: Double, removal: Double, mana: Double, cardCount: Int): Double {
        val l = min(cardCount / 23.0, 1.0)
        val qN = mu(quality, BA_QUALITY.mean * 0.7 * l, BA_QUALITY.twoSigma * 0.5 * max(l, 0.3))
        val cN = round1(curve.coerceIn(0.0, 10.0))
        val bN = mu(bomb, BA_BOMB.mean * l, BA_BOMB.twoSigma)
        val sN = mu(synergy, BA_SYNERGY.mean * l, BA_SYNERGY.twoSigma * max(l, 0.3))
        val rN = round1(removal.coerceIn(0.0, 10.0))
        val mN = mu(mana, BA_MANABASE.mean, BA_MANABASE.twoSigma)
        return round1((qN + cN + bN + sN + rN + mN) / 6.0)
    }

    /** `Mu(value, mean, twoSigma)` — z-score onto a 0–10 scale centered at 6.8. */
    private fun mu(value: Double, mean: Double, twoSigma: Double): Double {
        if (twoSigma == 0.0) return 6.8
        val n = 6.8 + ((value - mean) / twoSigma) * 3.1
        return round1(n.coerceIn(0.0, 10.0))
    }

    /** `Gm(count, avgRating)` — final-score removal sub-score (quantity × quality). */
    private fun gm(count: Int, avgRating: Double): Double {
        if (count == 0) return 0.0
        val quantity = when { count >= 7 -> 10; count == 6 -> 9; count == 5 -> 7; count == 4 -> 5; count == 3 -> 3; count == 2 -> 2; count == 1 -> 1; else -> 0 }
        val quality = when {
            avgRating >= 3.7 -> 10; avgRating >= 3.5 -> 9; avgRating >= 3.3 -> 8; avgRating >= 3.0 -> 7
            avgRating >= 2.6 -> 6; avgRating >= 2.3 -> 5; avgRating >= 2.0 -> 4; avgRating >= 1.7 -> 3
            avgRating >= 1.5 -> 2; avgRating >= 1.0 -> 1; else -> 0
        }
        return round1((quantity + quality) / 2.0)
    }

    // =========================================================================
    // §6 manabase playability G4 (seeded Monte Carlo)
    // =========================================================================

    /** `G4(cards, arch)` → 0–10 castability over 20 seeded opening hands. Deterministic per deck. */
    fun manabasePlayability(cards: List<ScorerCard>): Double {
        if (cards.size < 15) return 0.0
        var seed = 0
        for (card in cards) for (ch in card.name) seed = (seed shl 5) - seed + ch.code
        val rng = Mulberry32(seed)

        val fixMap: Map<String, List<String>> = cards
            .filter { !ops.isLand(it) }
            .mapNotNull { c -> ops.archRecord(c.name)?.fixing?.takeIf { it.isNotEmpty() }?.let { c.name to it } }
            .toMap()

        var valid = 0
        var castable = 0
        repeat(20) {
            val shuffled = cards.toMutableList()
            for (v in shuffled.indices.reversed()) {
                if (v == 0) break
                val j = floor(rng.next() * (v + 1)).toInt()
                val tmp = shuffled[v]; shuffled[v] = shuffled[j]; shuffled[j] = tmp
            }
            val hand = shuffled.take(7)
            val lands = hand.filter { ops.isLand(it) }
            if (lands.size < 2) return@repeat
            valid++

            val available = HashMap<String, Int>()
            for (land in lands) for (c in land.colorIdentity) available[c] = (available[c] ?: 0) + 1
            for (c in hand) fixMap[c.name]?.let { for (col in it) available[col] = (available[col] ?: 0) + 1 }

            if (hand.filter { !ops.isLand(it) }.all { payable(it, available) }) castable++
        }
        return if (valid == 0) 0.0 else Math.round(castable.toDouble() / valid * 100.0) / 10.0
    }

    private fun payable(card: ScorerCard, available: Map<String, Int>): Boolean {
        val pool = HashMap(available)
        val req = DraftsimMana.castRequirement(card.manaCost)
        for ((color, n) in req.plainPips) {
            val have = pool[color] ?: 0
            if (have < n) return false
            pool[color] = have - n
        }
        for (group in req.hybridGroups) {
            val best = group.maxByOrNull { pool[it] ?: 0 } ?: continue
            val have = pool[best] ?: 0
            if (have <= 0) return false
            pool[best] = have - 1
        }
        return true
    }

    /** mulberry32 PRNG (`hX`); 32-bit `imul`/`ushr` must match or the seeded sequence diverges. */
    private class Mulberry32(seed: Int) {
        private var state = seed
        fun next(): Double {
            state += 0x6D2B79F5
            var t = (state xor (state ushr 15)) * (state or 1)
            t = (t + ((t xor (t ushr 7)) * (t or 61))) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }
    }
}

private fun round1(x: Double): Double = Math.round(x * 10.0) / 10.0
