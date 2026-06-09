package com.wingedsheep.ai.draftsim

import kotlin.math.max
import kotlin.math.min

/**
 * The Draftsim per-card scorer, ported from `index-CgvY9PKD.pretty.js` (`SPEC_scoring.md`).
 *
 * Two entry points share the helpers below:
 *  - [scoreFallback] / [scoreBoosterFallback] — the bundle's `aX`/`oX`, the **default path** for
 *    every set without archetype data (all but FDN/SOS/SOSSPG/TMT). Color-bias only.
 *  - [score] / [scoreBooster] — the bundle's `jm`/`xs`, the archetype-aware path (added in the
 *    main-scorer stage of the port).
 *
 * A scorer is bound to one pool's [tables] (ratings/removal/archetypes). It holds no mutable state;
 * `archColors` and the per-call flags are passed in. Bundle symbols are noted per function.
 */
class DraftsimScorer(private val tables: DraftsimSetTables) {

    private val ops = DraftsimCardOps(tables)

    // ----- constants (SPEC_scoring.md §2) -----
    private companion object {
        const val QJ = 2.0          // quality baseline subtracted in color-weight accumulation (aW)
        const val PO = 3.5          // "meaningful color presence" threshold (po)
        const val JU = 2.0          // fallback on-color flat bonus
        const val ZJ = 0.9
        const val OS = PO / ZJ      // 3.888…, fallback color-spec divisor
        const val TX = 2.0          // fallback color-spec tuning (tX)
        const val EX = 0.8          // (eX)
        const val RX = 0.3          // (rX)
        const val NX = 17           // pool size above which openColors opens both top colors
        const val AC = 4            // "early pick" cutoff: nonland pool < ac → early branch
        const val G2 = 17           // late-pool threshold
        const val IX = 3            // real-archetype total threshold
        const val LX = 15           // "early-ish / colors flexible" threshold
        const val BOMB = 3.9        // bomb / splashable-bomb rating cutoff

        // rarity quality bonus (jm) cX
        val RARITY_QUALITY = mapOf("mythic" to 0.15, "rare" to 0.1, "uncommon" to 0.0, "common" to 0.0)
        // curve target per CMC bucket (iW)
        val CURVE_TARGET = mapOf(0 to 0.0, 1 to 1.5, 2 to 6.0, 3 to 6.0, 4 to 4.0, 5 to 2.0, 6 to 1.0)
    }

    // ----- per-card helpers delegate to the shared ops (SPEC_scoring.md §3) -----
    private fun rating(name: String) = ops.rating(name)
    private fun ratingOrDefault(card: ScorerCard) = ops.ratingOrDefault(card)
    private fun ratingFallback(card: ScorerCard) = ops.ratingFallback(card)
    private fun isLand(card: ScorerCard) = ops.isLand(card)
    private fun isBasic(card: ScorerCard) = ops.isBasic(card)
    private fun isPermanent(card: ScorerCard) = ops.isPermanent(card)
    private fun isLegendaryCreature(card: ScorerCard) = ops.isLegendaryCreature(card)
    private fun cmcBucket(x: Double) = ops.cmcBucket(x)
    private fun colorsOf(card: ScorerCard) = ops.colorsOf(card)
    private fun colorVector(colors: List<String>) = ops.colorVector(colors)

    // ----- color-weight & open-color helpers (SPEC_scoring.md §4) -----

    /** `colorWeights(cards)` (`aW`): per-color accumulated quality `max(0, ratingFallback - QJ)`. */
    private fun colorWeights(cards: List<ScorerCard>): DoubleArray {
        val w = DoubleArray(5)
        for (card in cards) {
            val q = max(0.0, ratingFallback(card) - QJ)
            if (q > 0) for (c in colorsOf(card)) DraftsimMana.COLORS.indexOf(c).let { if (it >= 0) w[it] += q }
        }
        return w
    }

    private data class OpenColors(val inColor: IntArray, val topColors: IntArray, val numPlayerColors: Int)

    /** `openColors(weights, poolSize)` (`oW`). */
    private fun openColors(weights: DoubleArray, poolSize: Int): OpenColors {
        var r = 0
        for (i in 1 until 5) if (weights[i] > weights[r]) r = i
        var n = if (r == 0) 1 else 0
        for (i in 0 until 5) if (i != r && weights[i] > weights[n]) n = i

        val inColor = IntArray(5)
        if (weights[r] > PO) inColor[r] = 1
        if (weights[n] > PO) inColor[n] = 1
        if (poolSize > NX) { inColor[r] = 1; inColor[n] = 1 }

        val numPlayerColors = weights.count { it > PO }
        return OpenColors(inColor, intArrayOf(r, n), numPlayerColors)
    }

    // ----- the fallback scorer aX / oX (SPEC_scoring.md §5) -----

    /**
     * `aX(card, pool, ratings)` — color-bias scoring with no archetype data. The common path: only 4
     * sets ship archetype columns, so most sets score here. Higher = better pick.
     */
    fun scoreFallback(card: ScorerCard, pool: List<ScorerCard>): DraftsimCardScore {
        val acc = Acc()
        val rawRating = ratingFallback(card)
        acc.total += rawRating
        acc.emit("Rating ${fmt(rawRating)}")

        val weights = colorWeights(pool)
        val open = openColors(weights, pool.size)
        val committedCount = open.inColor.sum()          // u — # locked colors (0/1/2)
        val cardVec = colorVector(colorsOf(card))
        val f = cardVec.sum()                             // card's color count

        // b = # of the card's colors that are off the committed set; g = on-color flag.
        var offCount = 0
        var onColor: Boolean
        if (f > 0) {
            onColor = true
            for (i in 0 until 5) if (cardVec[i] == 1 && open.inColor[i] == 0) { offCount++; onColor = false }
        } else {
            onColor = committedCount == 2
        }

        var h = 0.0
        when {
            committedCount == 2 -> {
                if (onColor) {
                    h = JU
                    acc.total += h; acc.emit("+${fmt(JU)} on-color")
                } else {
                    h = -max(0.0, offCount - 1.0)
                    acc.total += h
                    acc.emit(if (h == 0.0) "Splashable off-color" else "${fmt(h)} off-color ($offCount pips)")
                }
            }
            f == 0 -> {
                if (open.numPlayerColors > 1) {
                    h = min(PO / OS, weights.max() / OS)
                    acc.total += h; acc.emit("+${fmt(h)} colorless follows top color")
                }
            }
            f == 1 -> {
                val idx = cardVec.indexOfFirst { it == 1 }
                h = min(PO / OS, weights[idx] / OS)
                if (open.numPlayerColors == 1) h /= TX
                if (open.numPlayerColors == 1 && idx == open.topColors[1] && weights[idx] > 0) {
                    h = max(EX * PO / OS, h)
                }
                acc.total += h; acc.emit("+${fmt(h)} on-color spec")
            }
            f in 2..3 -> {
                var on = 0.0
                var off = 0.0
                for (i in 0 until 5) {
                    val capped = min(weights[i], PO)
                    if (cardVec[i] == 1) on += capped else off += capped
                }
                h = (on - off) / OS - RX
                acc.total += h; acc.emit("${fmt(h)} multicolor spec")
            }
        }

        return DraftsimCardScore(
            total = round2(rawRating + h),
            rawRating = rawRating,
            reasons = acc.reasons,
            reasonPoints = acc.points,
        )
    }

    /**
     * `oX(booster, picks, ratings)` — score every booster card with [scoreFallback], then attach a
     * one-line summary to the argmax. Returns name → score for the whole pack.
     */
    fun scoreBoosterFallback(booster: List<ScorerCard>, picks: List<ScorerCard>): Map<String, DraftsimCardScore> {
        val scores = booster.associate { it.name to scoreFallback(it, picks) }
        val pick = argmax(booster, scores) ?: return scores
        val pickScore = scores.getValue(pick.name)

        val weights = colorWeights(picks)
        val open = openColors(weights, picks.size)
        val committedColors = (0 until 5).filter { open.inColor[it] == 1 }.map { DraftsimMana.COLORS[it] }
        val pickColors = colorsOf(pick)
        val pickOnColor = pickColors.isEmpty() || pickColors.all { it in committedColors }

        val summary = if (open.inColor.sum() == 2 && pickOnColor) {
            "Best on-color card available (deck: ${committedColors.joinToString("")}, rating ${fmt(pickScore.rawRating)})."
        } else {
            "Highest-rated card in pack (rating ${fmt(pickScore.rawRating)}); colors still open."
        }
        return scores + (pick.name to pickScore.copy(summary = summary))
    }

    /** `Wo` — argmax by total, tie-broken by rawRating. */
    fun argmax(cards: List<ScorerCard>, scores: Map<String, DraftsimCardScore>): ScorerCard? {
        var best: ScorerCard? = null
        var bestTotal = Double.NEGATIVE_INFINITY
        var bestRaw = Double.NEGATIVE_INFINITY
        for (card in cards) {
            val s = scores[card.name]
            val total = s?.total ?: 0.0
            val raw = s?.rawRating ?: 0.0
            if (total > bestTotal || (total == bestTotal && raw > bestRaw)) {
                bestTotal = total; bestRaw = raw; best = card
            }
        }
        return best
    }

    // =========================================================================
    // Deck-color & archetype helpers (SPEC_scoring.md §4)
    // =========================================================================

    private fun idx(color: String) = ops.idx(color)
    private fun archRecord(name: String) = ops.archRecord(name)
    private fun deckCardWeight(card: ScorerCard) = ops.deckCardWeight(card)
    private fun isBombCard(card: ScorerCard) = ops.isBombCard(card)

    /** `deckColors(nonland)` (`wa`): colors sorted by accumulated weight, descending. */
    private fun deckColors(nonland: List<ScorerCard>): List<String> {
        val weight = accumulateDeckColorWeights(nonland, skipBombOffTop2 = false)
        return (0 until 5).filter { weight[it] > 0 }.sortedByDescending { weight[it] }.map { DraftsimMana.COLORS[it] }
    }

    /** Shared pass-1 (mono) + pass-2 (multicolor touching top-2) color-weight accumulation. */
    private fun accumulateDeckColorWeights(nonland: List<ScorerCard>, skipBombOffTop2: Boolean): DoubleArray {
        val weight = DoubleArray(5)
        for (card in nonland) {
            val cols = colorsOf(card)
            if (cols.size == 1) weight[idx(cols[0])] += deckCardWeight(card)
        }
        val top2 = (0 until 5).sortedByDescending { weight[it] }.take(2).map { DraftsimMana.COLORS[it] }
        for (card in nonland) {
            val cols = colorsOf(card)
            if (cols.size >= 2 && cols.any { it in top2 }) {
                val onlyTop2 = DraftsimMana.fitsColors(card.manaCost, top2) || (skipBombOffTop2 && isBombCard(card))
                val targets = if (onlyTop2) cols.filter { it in top2 } else cols
                for (c in targets) weight[idx(c)] += deckCardWeight(card)
            }
        }
        return weight
    }

    /**
     * `deckArchetypeColors(nonland)` (`s_`): the deck's color identity as a display string
     * (`"WU"`, `"WU splash B"`, `"WUB"`). Feeds only [DraftsimCardScore.deckContext] (not the pick
     * total), so this is a faithful-but-display-only port of the bundle's splash heuristics.
     */
    private fun deckArchetypeColors(nonland: List<ScorerCard>): DraftsimDeckContext {
        val weight = accumulateDeckColorWeights(nonland, skipBombOffTop2 = true)
        val order = (0 until 5).filter { weight[it] > 0 }.sortedByDescending { weight[it] }.map { DraftsimMana.COLORS[it] }
        if (order.isEmpty()) return DraftsimDeckContext()
        if (order.size == 1) return DraftsimDeckContext(order[0], null)

        val c1 = order[0]; val c2 = order[1]
        val base = c1 + c2
        val w1 = weight[idx(c1)]
        val third = order.getOrNull(2)

        // support(splash) = Σ weight of cards touching the splash color.
        fun support(color: String): Double =
            nonland.filter { color in colorsOf(it) }.sumOf { deckCardWeight(it) }
        fun hasBombOf(color: String): Boolean = nonland.any { color in colorsOf(it) && isBombCard(it) }

        if (third != null) {
            val b = support(third)
            if (b >= max(6.0, w1 * 0.4)) return DraftsimDeckContext(primary = "$c1$c2$third", secondary = base)
            if ((b >= 3.5 || hasBombOf(third)) && nonland.size >= 19)
                return DraftsimDeckContext(primary = "$base splash $third")
        }
        // Optional splashes: a 3rd/4th color with enough support / weight / a bomb.
        val splashes = order.drop(2).filter { support(it) >= 3.5 || weight[idx(it)] >= w1 / 2 || hasBombOf(it) }
        val secondary = if (splashes.isEmpty()) null else base + " splash " + splashes.joinToString("")
        return DraftsimDeckContext(primary = base, secondary = secondary)
    }

    /** An archetype's accumulated draft value (the bundle's `ZU` entry / forced placeholder). */
    private data class ArchScore(val name: String, val enablers: Double, val payoffs: Double, val total: Double)

    /** `archetypeScores(nonland, arch, archColors)` (`ZU`), sorted by total desc. */
    private fun archetypeScores(nonland: List<ScorerCard>, archColors: Map<String, List<String>>?): List<ArchScore> {
        if (archColors.isNullOrEmpty()) return emptyList()
        val colorWeight = DoubleArray(5)
        val enablers = HashMap<String, Double>()
        val payoffs = HashMap<String, Double>()
        val untaggedColored = mutableListOf<Pair<List<String>, Double>>()
        for (card in nonland) {
            val q = rating(card.name) ?: 2.5
            for (c in colorsOf(card)) colorWeight[idx(c)] += q
            val rec = archRecord(card.name)
            if (rec != null && rec.archetypes.isNotEmpty()) {
                for (tag in rec.archetypes) when (tag.role) {
                    "enabler" -> enablers[tag.archetype] = (enablers[tag.archetype] ?: 0.0) + 1
                    "payoff" -> payoffs[tag.archetype] = (payoffs[tag.archetype] ?: 0.0) + 1
                }
            } else if (colorsOf(card).isNotEmpty()) {
                untaggedColored += colorsOf(card) to q
            }
        }
        val top2 = (0 until 5).sortedByDescending { colorWeight[it] }.take(2).map { DraftsimMana.COLORS[it] }
        return archColors.map { (name, f) ->
            var colorBoost = 0.0
            for ((cols, quality) in untaggedColored) {
                val overlap = cols.count { it in f }
                if (overlap > 0) colorBoost += (overlap.toDouble() / cols.size) * ((quality - 1.5) / 4).coerceIn(0.25, 0.75)
            }
            if (top2.size >= 2 && nonland.size >= 3) {
                val g = f.take(2).count { it in top2 }
                val hh = min(2.0, 0.5 + (nonland.size - 3) * 0.3)
                colorBoost += if (g == 2) hh else if (g == 1) hh * 0.3 else 0.0
            }
            val e = enablers[name] ?: 0.0
            val p = payoffs[name] ?: 0.0
            ArchScore(name, e, p, e + p + colorBoost)
        }.sortedByDescending { it.total }
    }

    /** `archetypeColorsFor(nonland, archName, archColors)` (`QU`). */
    private fun archetypeColorsFor(nonland: List<ScorerCard>, archName: String, archColors: Map<String, List<String>>?): List<String> {
        val perColor = HashMap<String, Int>()
        for (card in nonland) {
            if (archRecord(card.name)?.archetypes?.any { it.archetype == archName } == true)
                for (c in colorsOf(card)) perColor[c] = (perColor[c] ?: 0) + 1
        }
        val c2 = perColor.filterValues { it >= 2 }.keys.toList()
        return if (c2.size < 2) (archColors?.get(archName) ?: c2) else c2
    }

    /** `hasBombSeparation(scores)` (`mX`). */
    private fun hasBombSeparation(scores: List<ArchScore>): Boolean =
        scores.size >= 2 && scores[0].total >= 4 && scores[0].total - scores[1].total >= 2

    /** `splashBombs(nonland, deckCols)` (`fX`): off-color ≥BOMB cards worth splashing. */
    private fun splashBombs(nonland: List<ScorerCard>, deckCols: List<String>): List<ScorerCard> {
        val onColor = nonland.count { colorsOf(it).isEmpty() || DraftsimMana.fitsColors(it.manaCost, deckCols) }
        if (nonland.isNotEmpty() && onColor < 12) return emptyList()
        return nonland.filter {
            !DraftsimMana.fitsColors(it.manaCost, deckCols) &&
                colorsOf(it).all { c -> c !in deckCols } &&
                ratingOrDefault(it) >= BOMB
        }
    }

    /** `Bm(nonland, arch)`: archetype → colors on ≥3 of its cards (plus colors ≥30% as frequent). */
    fun archColorMap(nonland: List<ScorerCard>): Map<String, List<String>> {
        if (tables.archetypes.isEmpty()) return emptyMap()
        val perArch = HashMap<String, HashMap<String, Int>>()
        for (card in nonland) {
            val rec = archRecord(card.name) ?: continue
            for (tag in rec.archetypes) {
                val counts = perArch.getOrPut(tag.archetype) { HashMap() }
                for (c in colorsOf(card)) counts[c] = (counts[c] ?: 0) + 1
            }
        }
        return perArch.mapValues { (_, counts) ->
            val top = counts.values.maxOrNull() ?: 0
            counts.filterValues { it >= 3 || it >= 0.3 * top }.keys.toList()
        }.filterValues { it.isNotEmpty() }
    }

    private fun fixersFor(color: String, allowed: Collection<String>, pool: List<ScorerCard>): Int {
        var count = 0
        for (card in pool) {
            if (isLand(card) && !isBasic(card)) {
                val rec = archRecord(card.name)
                val fix = rec?.fixing?.takeIf { it.isNotEmpty() } ?: colorsOf(card).ifEmpty { card.colorIdentity }
                if (fix.size >= 4) count++
                else if (fix.contains(color) && fix.any { it in allowed }) count++
            } else if (archRecord(card.name)?.fixing?.contains(color) == true) count++
        }
        return count
    }

    private data class Penalty(val penalty: Double, val offPips: Int, val fixing: Int, val offColors: List<String>)

    /** `colorPenalty(card, allowed, weight, pool, arch)` (`kn`). */
    private fun colorPenalty(card: ScorerCard, allowed: Collection<String>, weight: Double, pool: List<ScorerCard>): Penalty {
        val op = DraftsimMana.offPips(card.manaCost, allowed)
        if (op == 0) return Penalty(0.0, 0, 0, emptyList())
        val offCols = DraftsimMana.offColors(card.manaCost, allowed)
        val fixing = offCols.minOfOrNull { fixersFor(it, allowed, pool) } ?: 0
        val c = if (op == 1) 0.4 else 1.0
        val d = max(0.0, 1 - fixing / 3.0)
        return Penalty(weight * c * d, op, fixing, offCols.toList())
    }

    // =========================================================================
    // The main scorer jm (SPEC_scoring.md §6)
    // =========================================================================

    /**
     * `jm(card, pool, ratings, removal, arch, archColors, removalFlag, forcedArch)` — the full,
     * archetype-aware scorer. Used directly for the 4 tagged sets at draft time, and for **every**
     * set during deckbuild (where [forcedArch]/[removalFlag] are set and, for untagged sets, the
     * archetype-aware path collapses to the color-only path).
     *
     * The land-fixing and splash sub-branches inside §6.4 are ported from the spec's prose summary
     * (the bundle's verbose `jm`), so those deltas are best-effort; the base/anchor/removal/curve/
     * tail branches are exact.
     */
    fun score(
        card: ScorerCard,
        pool: List<ScorerCard>,
        archColors: Map<String, List<String>>? = null,
        removalFlag: Boolean = false,
        forcedArch: String? = null,
    ): DraftsimCardScore {
        val acc = Acc()
        val nonland = pool.filter { !isLand(it) }
        val h = nonland.size
        val hasArch = tables.archetypes.isNotEmpty()
        val g = rating(card.name)

        // ----- §6.1 early exits & base -----
        if (isBasic(card)) {
            return DraftsimCardScore(-1.0, 0.0, listOf("Basic land: always available, never draft"), listOf(-1.0))
        }
        val v: Double
        if (isLand(card)) {
            v = if (g != null && g >= BOMB) g else 2.0
            acc.total += v; acc.emit("Land")
        } else {
            v = g ?: 2.5
            acc.total += v
            acc.emit(if (g != null) "Card quality: ${fmt(g)}/5" else "Card quality: unrated")
            val rb = RARITY_QUALITY[card.rarity] ?: 0.0
            if (rb > 0) { acc.total += rb; acc.emit("Rarity bonus (${card.rarity})") }
        }

        val poolBombs = nonland.filter { isBombCard(it) }
        val topBomb = poolBombs.maxByOrNull { ratingOrDefault(it) }

        // ----- §6.2 bomb anchoring -----
        if (hasArch && !isLand(card) && topBomb != null) {
            val e = if (ratingOrDefault(topBomb) >= 4.3) 0.3 else 0.15
            val bombArchNames = archRecord(topBomb.name)?.archetypes?.map { it.archetype }?.toSet() ?: emptySet()
            val ktFromArch = bombArchNames.flatMap { archColors?.get(it) ?: emptyList() }.toSet()
            val kt = ktFromArch.ifEmpty { colorsOf(topBomb).toSet() }
            val cardColors = colorsOf(card)
            if (cardColors.isNotEmpty() && cardColors.all { it in kt }) {
                acc.total += e; acc.emit("Bomb anchor: on-color")
            } else {
                val shared = (archRecord(card.name)?.archetypes?.map { it.archetype }?.toSet() ?: emptySet())
                    .intersect(bombArchNames)
                if (shared.isNotEmpty()) { acc.total += e; acc.emit("Bomb anchor: on-archetype (${shared.first()})") }
            }
        }

        // ----- §6.3 EARLY branch (h < ac) -----
        if (h < AC) {
            scoreEarlyBranch(card, nonland, v, h, acc, archColors, topBomb)
            val ctx = earlyDeckContext(nonland, archColors, topBomb)
            return finalize(acc, v, ctx, floorAtZero = true)
        }

        // ----- §6.4 MAIN branch setup -----
        val isAnyBomb = poolBombs.isNotEmpty()
        val s = if (isAnyBomb) 0.25 else 0.05
        var r = if (h <= 6) s else min(1.0, s + (h - 6) / 28.0)
        val u: List<ArchScore> = when {
            forcedArch != null -> listOf(ArchScore(forcedArch, 10.0, 10.0, 20.0))
            hasArch -> archetypeScores(nonland, archColors)
            else -> emptyList()
        }
        val x = u.getOrNull(0)
        val bombSep = hasBombSeparation(u)
        val late = h >= G2 && (x?.total ?: 0.0) >= IX
        val flexible = h < LX
        val committedT = !flexible && (u.getOrNull(0)?.total ?: 0.0) >= 2
        val committed = bombSep || late || committedT
        val k = if (x != null && (committed || x.total >= 2)) archetypeColorsFor(nonland, x.name, archColors) else emptyList()
        if (committed && (k.isNotEmpty() || h >= 5)) r = max(r, 0.5)
        val w = if (k.isNotEmpty()) k else if (h >= 3) deckColors(nonland).take(2) else emptyList()

        val d = hasArch && (u.isNotEmpty() || h < G2)
        val splashCols: List<String> =
            if (!d && w.size >= 2) poolBombs.flatMap { b -> colorsOf(b).filter { it !in w } }.distinct() else emptyList()
        val secondary: ArchScore? = if (committed && flexible && u.size > 1) {
            val p1 = archetypeColorsFor(nonland, u[1].name, archColors)
            if (p1.isNotEmpty() && p1 != k) u[1] else null
        } else null

        if (d) scoreArchetypeAware(card, pool, nonland, v, h, r, acc, u, committed, w, archColors)
        else scoreColorOnly(card, pool, nonland, v, h, r, acc, w, splashCols)

        // ----- §6.5 removal -----
        scoreRemoval(card, nonland, h, r, w, acc, removalFlag)
        // ----- §6.6 curve -----
        scoreCurve(card, nonland, h, r, w, acc)
        // ----- §6.7 tail -----
        scoreTail(card, pool, v, acc)

        val ctx = if (hasArch && (u.getOrNull(0)?.total ?: 0.0) > 0)
            DraftsimDeckContext(u[0].name, secondary?.name)
        else deckArchetypeColors(nonland)
        return finalize(acc, v, ctx, floorAtZero = true)
    }

    /** `xs(booster, picks, …)` — score every booster card with [score] (6-arg jm: no forced arch). */
    fun scoreBooster(booster: List<ScorerCard>, picks: List<ScorerCard>): Map<String, DraftsimCardScore> {
        val archColors = archColorMap(picks.filter { !isLand(it) })
        return booster.associate { it.name to score(it, picks, archColors) }
    }

    /**
     * `xs` entry: route to the archetype-aware [scoreBooster] when the set ships archetype data, else
     * to the color-only [scoreBoosterFallback] (the common path). Mirrors the bundle's `xs` dispatch.
     */
    fun scoreBoosterAuto(booster: List<ScorerCard>, picks: List<ScorerCard>): Map<String, DraftsimCardScore> =
        if (tables.archetypes.isEmpty()) scoreBoosterFallback(booster, picks) else scoreBooster(booster, picks)

    /** The reason with the largest marginal point contribution (the hint layer's dominant reason). */
    fun dominantReason(score: DraftsimCardScore): String? {
        if (score.reasons.isEmpty()) return null
        val i = score.reasonPoints.indices.maxByOrNull { score.reasonPoints[it] } ?: 0
        return score.reasons.getOrNull(i)
    }

    // ----- §6.3 helpers -----

    private fun scoreEarlyBranch(
        card: ScorerCard, nonland: List<ScorerCard>, v: Double, h: Int, acc: Acc,
        archColors: Map<String, List<String>>?, topBomb: ScorerCard?,
    ) {
        val poolColors = nonland.flatMap { colorsOf(it) }.toSet()
        val cardColors = colorsOf(card)
        if (topBomb != null && !isLand(card)) {
            if (poolColors.size >= 3 && cardColors.isNotEmpty() && cardColors.none { it in poolColors } && v < 4.4) {
                acc.total -= 0.5; acc.emit("4th color; already drafting ${poolColors.size} colors")
            }
        }
        if (isLand(card)) {
            val rec = archRecord(card.name)
            val fix = rec?.fixing?.takeIf { it.isNotEmpty() } ?: colorsOf(card).ifEmpty { card.colorIdentity }
            val bombColors = topBomb?.let { colorsOf(it) } ?: emptyList()
            if (topBomb != null && bombColors.size >= 2) {
                when {
                    bombColors.all { it in fix } -> { acc.total += 2.5; acc.emit("Fixing (${fix.joinToString("")}): supports bomb") }
                    bombColors.any { it in fix } -> { acc.total += 0.5; acc.emit("Fixing (${fix.joinToString("")}): partial bomb support") }
                }
            } else if (fix.isNotEmpty() && fix.size < 4 && fix.none { it in poolColors }) {
                val delta = max(0.0, 1 - h * 0.15)
                acc.total -= delta; acc.emit("Off-color land (${fix.joinToString("")})")
            }
        }
    }

    private fun earlyDeckContext(
        nonland: List<ScorerCard>, archColors: Map<String, List<String>>?, topBomb: ScorerCard?,
    ): DraftsimDeckContext {
        val arch = if (tables.archetypes.isNotEmpty()) archetypeScores(nonland, archColors) else emptyList()
        if (arch.isNotEmpty() && arch[0].total > 0) return DraftsimDeckContext(arch[0].name, null)
        val bombArch = topBomb?.let { archRecord(it.name)?.archetypes?.firstOrNull()?.archetype }
        if (bombArch != null) return DraftsimDeckContext(bombArch, null)
        return deckArchetypeColors(nonland)
    }

    // ----- §6.4 (A) archetype-aware spell/land scoring (tagged sets) -----

    private fun scoreArchetypeAware(
        card: ScorerCard, pool: List<ScorerCard>, nonland: List<ScorerCard>, v: Double, h: Int, r: Double,
        acc: Acc, u: List<ArchScore>, committed: Boolean, w: List<String>, archColors: Map<String, List<String>>?,
    ) {
        val rec = archRecord(card.name)
        val cardArchNames = rec?.archetypes?.map { it.archetype }?.toSet() ?: emptySet()
        val committedArch = (if (committed) u.take(1) else u.take(2)).map { it.name }.toSet()
        val offWeight = if (committed) 3.5 else 2.5
        val castable = w.isEmpty() || colorsOf(card).isEmpty() || DraftsimMana.fitsColors(card.manaCost, w)

        if (isLand(card)) {
            val fix = rec?.fixing?.takeIf { it.isNotEmpty() } ?: colorsOf(card).ifEmpty { card.colorIdentity }
            when {
                fix.size >= 4 -> { acc.total += 0.3 * r; acc.emit("Universal fixing") }
                fix.isNotEmpty() && fix.all { it in w } && w.isNotEmpty() -> { acc.total += 1.5 * r + 1; acc.emit("On-color fixing") }
                fix.isNotEmpty() && fix.any { it in w } -> { acc.total += 0.3 * r; acc.emit("Partial fixing") }
                fix.isNotEmpty() && fix.none { it in w } && w.isNotEmpty() -> { acc.total -= 3 * r; acc.emit("Wrong colors") }
                else -> { acc.total += 0.2 * r; acc.emit("Land") }
            }
            return
        }

        val onArch = cardArchNames.intersect(committedArch)
        if (onArch.isNotEmpty()) {
            val topArch = u.getOrNull(0)?.total ?: 1.0
            val cardArchTotal = u.firstOrNull { it.name in onArch }?.total ?: topArch
            val base = if (committed) 2.0 else 1.5
            val weight = if (committed) base else base * (cardArchTotal / max(topArch, 0.0001))
            acc.total += weight * r; acc.emit("On-archetype (${onArch.first()})")
            if (committed && (card.rarity == "rare" || card.rarity == "mythic")) {
                val bonus = if (card.rarity == "mythic") 0.6 else 0.3
                acc.total += bonus; acc.emit("Won't see again")
            }
        } else if (cardArchNames.isEmpty()) {
            if (u.isNotEmpty() && castable) { acc.total += 1 * r; acc.emit("No archetype; on-color") }
            else if (!castable) {
                val pen = colorPenalty(card, w, offWeight, pool)
                if (pen.penalty > 0) { acc.total -= pen.penalty * r; acc.emit("Off-color") }
            } else { acc.total += 0.3 * r; acc.emit("No archetype; flexible") }
        } else {
            // tagged but off the committed archetypes
            if (!castable) {
                if (isBombCard(card)) {
                    val pen = colorPenalty(card, w, offWeight, pool)
                    val net = 1 * r - (if (pen.offPips <= 1) 0.0 else pen.penalty * r)
                    acc.total += net; acc.emit("Splashable bomb")
                } else {
                    val pen = colorPenalty(card, w, offWeight, pool)
                    if (pen.penalty > 0) { acc.total -= pen.penalty * r; acc.emit("Off-color") }
                }
            } else { acc.total += 0.2 * r; acc.emit("Open to archetype signals") }
        }
    }

    // ----- §6.4 (B) color-only scoring (untagged sets / arch produced nothing) -----

    private fun scoreColorOnly(
        card: ScorerCard, pool: List<ScorerCard>, nonland: List<ScorerCard>, v: Double, h: Int, r: Double,
        acc: Acc, w: List<String>, splashCols: List<String>,
    ) {
        val cardColors = colorsOf(card)
        if (isLand(card)) {
            val allowed = (w + splashCols).distinct()
            val fix = archRecord(card.name)?.fixing?.takeIf { it.isNotEmpty() } ?: colorsOf(card).ifEmpty { card.colorIdentity }
            when {
                fix.isEmpty() -> { acc.total += 0.3 * r; acc.emit("Colorless land") }
                fix.size >= 4 -> { acc.total += 2 * r; acc.emit("Universal fixing") }
                fix.all { it in allowed } && allowed.isNotEmpty() -> { acc.total += 2 * r; acc.emit("On-color fixing") }
                fix.any { it in allowed } -> { acc.total += 0.3 * r; acc.emit("Partial fixing") }
                else -> { acc.total -= 1.5 * r; acc.emit("Wrong colors") }
            }
            return
        }
        if (cardColors.isEmpty()) { acc.total += 0.5 * r; acc.emit("Colorless; fits any deck"); return }

        val onColor = w.isNotEmpty() && (DraftsimMana.fitsColors(card.manaCost, w) || cardColors.all { it in w })
        when {
            onColor -> { acc.total += 2 * r; acc.emit("On-color") }
            cardColors.any { it in w } && w.isNotEmpty() -> { acc.total += 0.5 * r; acc.emit("Partially on-color") }
            cardColors.any { it in splashCols } -> { acc.total += 1 * r; acc.emit("Supports a splash bomb") }
            isBombCard(card) -> {
                val pen = colorPenalty(card, w, if (h >= LX) 3.5 else 2.5, pool)
                val net = 1 * r - (if (pen.offPips <= 1) 0.0 else pen.penalty * r)
                acc.total += net; acc.emit("Off-color bomb (splash)")
            }
            w.size < 2 -> { acc.total += 0.3 * r; acc.emit("Colors still open") }
            else -> {
                val pen = colorPenalty(card, w, if (h >= LX) 3.5 else 2.5, pool)
                if (pen.penalty > 0) { acc.total -= pen.penalty * r; acc.emit("Off-color") }
            }
        }
    }

    // ----- §6.5 removal -----

    private fun scoreRemoval(
        card: ScorerCard, nonland: List<ScorerCard>, h: Int, r: Double, w: List<String>, acc: Acc, removalFlag: Boolean,
    ) {
        if (card.name.lowercase() !in tables.removal) return
        val castable = w.isEmpty() || colorsOf(card).isEmpty() || DraftsimMana.fitsColors(card.manaCost, w) ||
            colorsOf(card).any { it in w }
        if (h >= 3 && w.isNotEmpty() && !castable) { acc.emit("Off-color removal; not playing it"); return }

        val onColorRemoval = nonland.count {
            it.name.lowercase() in tables.removal &&
                (colorsOf(it).isEmpty() || w.isEmpty() || DraftsimMana.fitsColors(it.manaCost, w))
        }
        val target = 6
        val ramp = min(h / 23.0, 1.0)
        val shortfall = target * ramp - onColorRemoval
        if (removalFlag) {
            if (onColorRemoval < target) { acc.total += 1 * r; acc.emit("Removal ($onColorRemoval/6)") }
            else acc.emit("Removal (have $onColorRemoval; enough)")
        } else {
            when {
                onColorRemoval >= 9 -> { acc.total -= 1 * r; acc.emit("Too much removal") }
                shortfall > 2 -> { acc.total += 2 * r * max(min(shortfall / 6, 1.0) * ramp, 0.3); acc.emit("Need removal") }
                shortfall > 0 -> { acc.total += 1 * r * ramp; acc.emit("Removal helps") }
                else -> acc.emit("Removal (enough)")
            }
        }
    }

    // ----- §6.6 curve -----

    private fun scoreCurve(card: ScorerCard, nonland: List<ScorerCard>, h: Int, r: Double, w: List<String>, acc: Acc) {
        if (isLand(card) || !isPermanent(card) || w.isEmpty()) return
        if (colorsOf(card).isNotEmpty() && !DraftsimMana.fitsColors(card.manaCost, w)) return
        val bucket = cmcBucket(card.cmc)
        val ramp = min(h / 23.0, 1.0)
        val target = (CURVE_TARGET[bucket] ?: 0.0) * ramp
        val have = nonland.count {
            !isLand(it) && cmcBucket(it.cmc) == bucket &&
                (colorsOf(it).isEmpty() || DraftsimMana.fitsColors(it.manaCost, w))
        }
        val deficit = target - have
        if (deficit > 0.5) {
            val bonus = min(1.5, min(deficit / max(CURVE_TARGET[bucket] ?: 1.0, 1.0), 1.0) * ramp * 3.5) * r
            acc.total += bonus; acc.emit("Fills curve (${bucket}-drop)")
        } else if (have > target + 2.5) {
            acc.total -= 0.4 * r; acc.emit("Overcrowded curve")
        }
    }

    // ----- §6.7 tail -----

    private fun scoreTail(card: ScorerCard, pool: List<ScorerCard>, v: Double, acc: Acc) {
        val price = card.priceUsd
        if (v < 2 && price != null && price >= 5) {
            val bonus = when { price >= 20 -> 0.6; price >= 10 -> 0.4; else -> 0.2 }
            acc.total += bonus; acc.emit("Hate draft ($${fmt(price)})")
        }
        if (isLegendaryCreature(card)) {
            val copies = pool.count { it.name == card.name }
            when {
                copies >= 3 -> { acc.total -= 6; acc.emit("4th+ copy of a legend; unplayable") }
                copies >= 2 -> { acc.total -= 4; acc.emit("3rd copy; deck caps at 2") }
            }
        }
    }

    private fun finalize(acc: Acc, rawRating: Double, ctx: DraftsimDeckContext, floorAtZero: Boolean = false): DraftsimCardScore {
        val total = if (floorAtZero) round2(max(0.0, acc.total)) else round2(acc.total)
        return DraftsimCardScore(total, rawRating, acc.reasons, acc.points, ctx)
    }

    // ----- internal scoring scaffolding -----

    /** Running-total accumulator that records each reason's rounded marginal point delta. */
    private class Acc {
        var total = 0.0
        private var prev = 0.0
        val reasons = mutableListOf<String>()
        val points = mutableListOf<Double>()
        fun emit(reason: String) {
            reasons += reason
            points += round2(total - prev)
            prev = total
        }
    }
}

/** Round to 2 decimals the same way the bundle does (`Math.round(x*100)/100`). */
internal fun round2(x: Double): Double = Math.round(x * 100.0) / 100.0

/** Compact number formatting for reason strings (no trailing-zero noise). */
private fun fmt(x: Double): String {
    val r = round2(x)
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}
