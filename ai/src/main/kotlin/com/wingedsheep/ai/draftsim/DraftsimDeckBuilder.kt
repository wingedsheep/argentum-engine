package com.wingedsheep.ai.draftsim

import kotlin.math.floor
import kotlin.math.max

/** A physical pool card: [card] + a stable [instanceId] (the identity used in all dedup sets). */
data class DraftsimPoolCard(val card: ScorerCard, val instanceId: String)

/** One built deck. [deckInstanceIds] are exactly the pool cards to move into the deck. */
data class DraftsimBuild(
    val name: String,
    val colors: List<String>,
    val score: Double,
    val manaBaseScore: Double,
    val deckInstanceIds: Set<String>,
    val basicsNeeded: Map<String, Int>,
)

/**
 * Auto-deckbuilding, ported from `SPEC_deckbuild.md`: `dW` ranks archetypes (`kf`), builds each with
 * the greedy `vX`, refines with the hill-climbing `ek`, and sorts by the final `Mm` score. Sealed
 * adds one `SX` "good stuff" build. Bound to one pool's [tables]; reuses [DraftsimCardOps] /
 * [DraftsimScorer] (jm) / [DraftsimDeckScorer] (kf, Mm, tk).
 */
class DraftsimDeckBuilder(private val tables: DraftsimSetTables) {

    private val ops = DraftsimCardOps(tables)
    private val scorer = DraftsimScorer(tables)
    private val deckScorer = DraftsimDeckScorer(tables)

    private companion object {
        const val DECK_NONLAND = 23
        const val DECK_LANDS = 17
        const val CREATURE_FLOOR = 13      // dl
        const val BOMB = 3.9
        // per-CMC-bucket card caps M4; default 2 for missing bucket.
        val BUCKET_CAP = mapOf(0 to 0, 1 to 3, 2 to 8, 3 to 8, 4 to 6, 5 to 4, 6 to 2)
        val COLOR_TO_BASIC = mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")
        // Ten three-color guild shells for the sealed good-stuff build.
        val THREE_COLOR = DraftsimDeckScorerGuilds.THREE_COLOR
    }

    private fun copyCap(card: ScorerCard) = if (ops.isLegendaryCreature(card)) 2 else 4
    private fun castable(card: ScorerCard, colors: List<String>) =
        ops.colorsOf(card).isEmpty() || DraftsimMana.fitsColors(card.manaCost, colors)
    private fun isRemoval(card: ScorerCard) = card.name.lowercase() in tables.removal

    /** Synthetic basic-land pool cards (`I4`). */
    private fun basics(basicsNeeded: Map<String, Int>): List<DraftsimPoolCard> =
        basicsNeeded.flatMap { (color, n) ->
            val name = COLOR_TO_BASIC[color] ?: "Wastes"
            (0 until n).map { DraftsimPoolCard(BasicLand(name, color), "basic-$color-$it") }
        }

    private class BasicLand(override val name: String, color: String) : ScorerCard {
        override val manaCost = ""
        override val typeLine = "Basic Land"
        override val cmc = 0.0
        override val rarity: String? = "common"
        override val priceUsd: Double? = null
        override val colors = emptyList<String>()
        override val colorIdentity = listOf(color)
    }

    // =========================================================================
    // dW — orchestrator (the entry point)
    // =========================================================================

    /**
     * `dW(pool, mode)` → builds best-first. `mode` ∈ "draft" (top 2) / "sealed" (top 3 + good stuff).
     *
     * When [forced] is non-empty the builder switches to **completion mode** (the deckbuild
     * "Complete Deck" button): every forced pool instance is kept and protected from removal/swap,
     * the build colors are taken from those locked cards, and the rest is filled with the normal
     * greedy + refine machinery so the completed deck is scored and mana-based identically.
     */
    fun buildDecks(pool: List<DraftsimPoolCard>, mode: String, forced: Set<String> = emptySet()): List<DraftsimBuild> {
        if (forced.isNotEmpty()) return listOf(completeBuild(pool, forced))
        val nonlandCards = pool.filter { !ops.isLand(it.card) }.map { it.card }
        val archColors = scorer.archColorMap(nonlandCards)
        val ranked = deckScorer.rankArchetypes(pool.map { it.card }, archColors)
            .filterNot { it.name.lowercase().contains("good stuff") }
        val n = if (mode == "draft") 2 else 3
        val builds = ranked.take(n).map { a -> refine(greedyBuild(pool, a.name, a.colors), pool) }
            .toMutableList()
        if (mode == "sealed") goodStuffBuild(pool)?.let { builds += refine(it, pool) }
        return builds.sortedByDescending { it.score }
    }

    // =========================================================================
    // Completion mode — keep the player's locked cards, fill the rest
    // =========================================================================

    /** Build a single deck that keeps every [forced] pool instance, in the locked cards' colors. */
    private fun completeBuild(pool: List<DraftsimPoolCard>, forced: Set<String>): DraftsimBuild {
        val forcedNonland = pool.filter { it.instanceId in forced && !ops.isLand(it.card) }.map { it.card }
        val colors = completionColors(forcedNonland, pool)
        return refine(greedyBuild(pool, "Completion", colors, forced), pool, forced = forced)
    }

    /**
     * The build colors for a completion: **every** color the locked cards strictly require, heaviest-
     * first by colored-pip weight, so [buildManabase] fixes all of them and no locked card is left
     * uncastable (locking a three-color pile yields a three-color manabase). When the locked cards
     * don't pin two colors (mono-color or colorless picks) it's topped up from the pool's overall pip
     * weight so the fill still has a real two-color base to build around.
     *
     * Only **plain** colored pips select colors. Hybrid / Phyrexian pips are flexible — payable from
     * another color or generic mana — so they must not pull their color into the manabase, which would
     * add basics the deck can't use (e.g. a `{2/B}` card in a GU deck must not summon Swamps).
     */
    private fun completionColors(forcedNonland: List<ScorerCard>, pool: List<DraftsimPoolCard>): List<String> {
        fun weigh(cards: List<ScorerCard>): Map<String, Double> {
            val pips = HashMap<String, Double>()
            for (card in cards) {
                DraftsimMana.castRequirement(card.manaCost).plainPips
                    .forEach { (c, v) -> pips[c] = (pips[c] ?: 0.0) + v }
            }
            return pips
        }
        val ordered = weigh(forcedNonland).entries.sortedByDescending { it.value }.map { it.key }.toMutableList()
        if (ordered.size < 2) {
            val poolWeights = weigh(pool.filter { !ops.isLand(it.card) }.map { it.card })
            for (color in poolWeights.entries.sortedByDescending { it.value }.map { it.key }) {
                if (ordered.size >= 2) break
                if (color !in ordered) ordered += color
            }
        }
        return ordered
    }

    // =========================================================================
    // vX — greedy single-archetype build
    // =========================================================================

    private data class Scored(val pc: DraftsimPoolCard, val score: DraftsimCardScore) {
        val total get() = score.total
        val rawRating get() = score.rawRating
    }

    /** Mutable build state threaded through the phases. */
    private inner class BuildState {
        val deck = mutableListOf<Scored>()
        val chosen = HashSet<String>()
        val copies = HashMap<String, Int>()
        val buckets = HashMap<Int, Int>()
        var creatures = 0
        /** Locked (completion) picks — kept unconditionally and never removed/swapped. */
        val forced = HashSet<String>()

        /** Force-include a locked card, bypassing the copy/bucket caps and marking it protected. */
        fun seed(item: Scored) { add(item); forced += item.pc.instanceId }

        fun canAdd(item: Scored): Boolean {
            val card = item.pc.card
            val bucket = ops.cmcBucket(card.cmc)
            return (copies[card.name] ?: 0) < copyCap(card) && (buckets[bucket] ?: 0) < (BUCKET_CAP[bucket] ?: 2)
        }

        fun add(item: Scored) {
            val card = item.pc.card
            deck += item; chosen += item.pc.instanceId
            copies[card.name] = (copies[card.name] ?: 0) + 1
            val bucket = ops.cmcBucket(card.cmc); buckets[bucket] = (buckets[bucket] ?: 0) + 1
            if (ops.isCreature(card)) creatures++
        }

        fun remove(item: Scored) {
            val card = item.pc.card
            deck -= item; chosen -= item.pc.instanceId
            copies[card.name] = (copies[card.name] ?: 1) - 1
            val bucket = ops.cmcBucket(card.cmc); buckets[bucket] = (buckets[bucket] ?: 1) - 1
            if (ops.isCreature(card)) creatures--
        }
    }

    private fun greedyBuild(
        pool: List<DraftsimPoolCard>, archName: String, archColors: List<String>, forced: Set<String> = emptySet(),
    ): DraftsimBuild {
        val nonlandPC = pool.filter { !ops.isLand(it.card) }
        val poolLands = pool.filter { ops.isLand(it.card) && !ops.isBasic(it.card) }
        val forcedMap = mapOf(archName to archColors)
        val nonlandCards = nonlandPC.map { it.card }

        val scored = nonlandPC.map { pc ->
            Scored(pc, scorer.score(pc.card, nonlandCards, forcedMap, removalFlag = true, forcedArch = archName))
        }.sortedWith(compareByDescending<Scored> { it.total }.thenByDescending { it.rawRating })

        val removalCards = scored.filter { isRemoval(it.pc.card) && castable(it.pc.card, archColors) }
        val otherCards = scored.filter { !isRemoval(it.pc.card) && castable(it.pc.card, archColors) }

        val st = BuildState()

        // Completion: seed the locked cards first so the phases fill around them (and never drop them).
        if (forced.isNotEmpty()) for (item in scored) if (item.pc.instanceId in forced) st.seed(item)

        // Phase 1 — removal first (cap 6). Seeded (locked) removal is already in the deck: skip it so
        // it isn't double-added, but count it against the cap so completion doesn't over-stack removal.
        var removalTaken = st.deck.count { isRemoval(it.pc.card) }
        for (item in removalCards) {
            if (removalTaken >= 6 || st.deck.size >= DECK_NONLAND) break
            if (item.pc.instanceId in st.chosen) continue
            if (st.canAdd(item)) { st.add(item); removalTaken++ }
        }
        // Phase 2 — interleave removal vs. best other until 9 removal or 23 cards.
        run {
            val remaining = removalCards.filter { it.pc.instanceId !in st.chosen }
            var wi = 0; var gi = 0
            while (removalTaken < 9 && st.deck.size < DECK_NONLAND) {
                while (wi < remaining.size && !st.canAdd(remaining[wi])) wi++
                while (gi < otherCards.size && (otherCards[gi].pc.instanceId in st.chosen || !st.canAdd(otherCards[gi]))) gi++
                val f = remaining.getOrNull(wi)
                val t = otherCards.getOrNull(gi)
                if (f == null) break
                if (t == null || f.total >= t.total) { st.add(f); removalTaken++; wi++ } else break
            }
        }
        // Phase 3 — fill by pure score to 23.
        for (item in scored) {
            if (st.deck.size >= DECK_NONLAND) break
            if (item.pc.instanceId in st.chosen || !castable(item.pc.card, archColors)) continue
            if (st.canAdd(item)) st.add(item)
        }
        // Phase 4 — relax bucket caps.
        for (item in scored) {
            if (st.deck.size >= DECK_NONLAND) break
            if (item.pc.instanceId in st.chosen || !castable(item.pc.card, archColors)) continue
            if ((st.copies[item.pc.card.name] ?: 0) < copyCap(item.pc.card)) st.add(item)
        }
        // Phase 5 — relax castability + bucket caps.
        for (item in scored) {
            if (st.deck.size >= DECK_NONLAND) break
            if (item.pc.instanceId in st.chosen) continue
            if ((st.copies[item.pc.card.name] ?: 0) < copyCap(item.pc.card)) st.add(item)
        }

        creatureFloor(st, scored)
        val splashColor = splashPass(st, scored, poolLands, archColors)

        val deckColors = if (splashColor != null) archColors + splashColor else archColors
        val chosenCards = st.deck.map { it.pc }
        val mana = buildManabase(chosenCards, poolLands, deckColors)
        val includedIds = (chosenCards.map { it.instanceId } + mana.usefulPoolLands.map { it.instanceId }).toSet()
        val fullDeck = chosenCards.map { it.card } + mana.usefulPoolLands.map { it.card } + basics(mana.basicsNeeded).map { it.card }
        val final = deckScorer.scoreDeck(fullDeck)
        return DraftsimBuild(archName, deckColors, final.score, final.manaBaseScore, includedIds, mana.basicsNeeded)
    }

    /** §1.6 creature floor: swap weakest non-creature non-removal cards for best available creatures. */
    private fun creatureFloor(st: BuildState, scored: List<Scored>) {
        if (st.creatures >= CREATURE_FLOOR) return
        val candidates = scored.filter {
            ops.isCreature(it.pc.card) && it.pc.instanceId !in st.chosen &&
                (st.copies[it.pc.card.name] ?: 0) < copyCap(it.pc.card)
        }
        val victims = st.deck.filter { !ops.isCreature(it.pc.card) && !isRemoval(it.pc.card) && it.pc.instanceId !in st.forced }.sortedBy { it.total }
        val k = minOf(CREATURE_FLOOR - st.creatures, victims.size, candidates.size)
        for (x in 0 until k) { st.remove(victims[x]); st.add(candidates[x]) }
    }

    /** §1.7 splash pass: add one off-color bomb the pool can fix, or swap it for the weakest card. */
    private fun splashPass(st: BuildState, scored: List<Scored>, poolLands: List<DraftsimPoolCard>, archColors: List<String>): String? {
        val d = archColors.toSet()
        var splashColor: String? = null
        fun poolFixes(color: String) = poolLands.any { land ->
            val fix = ops.archRecord(land.card.name)?.fixing?.takeIf { it.isNotEmpty() }
                ?: ops.colorsOf(land.card).ifEmpty { land.card.colorIdentity }
            color in fix
        }
        val eligible = scored.filter {
            val card = it.pc.card
            it.pc.instanceId !in st.chosen && ops.ratingOrDefault(card) >= BOMB && !castable(card, archColors) &&
                !ops.isLand(card) && ops.archRecord(card.name)?.splashable == true &&
                ops.colorsOf(card).count { c -> c !in d } == 1 &&
                poolFixes(ops.colorsOf(card).first { c -> c !in d })
        }.sortedByDescending { ops.ratingOrDefault(it.pc.card) }

        for (item in eligible) {
            val k = ops.colorsOf(item.pc.card).first { it !in d }
            if (splashColor != null && k != splashColor) continue
            if (st.deck.size < DECK_NONLAND) { st.add(item); splashColor = k } else {
                val weakest = st.deck.filter { !isRemoval(it.pc.card) && it.pc.instanceId !in st.forced }.minByOrNull { it.total } ?: break
                if (item.rawRating > weakest.rawRating) { st.remove(weakest); st.add(item); splashColor = k } else break
            }
        }
        return splashColor
    }

    // =========================================================================
    // Tm — manabase builder
    // =========================================================================

    private data class Manabase(
        val usefulPoolLands: List<DraftsimPoolCard>, val basicsNeeded: Map<String, Int>,
    )

    private fun buildManabase(deckCards: List<DraftsimPoolCard>, poolLands: List<DraftsimPoolCard>, forcedColors: List<String>): Manabase {
        // §3.1 allowed set.
        val a: Set<String> = if (forcedColors.isNotEmpty()) forcedColors.toSet() else {
            val pips = HashMap<String, Double>()
            for (pc in deckCards) DraftsimMana.pipCounts(pc.card.manaCost).forEach { (c, v) -> pips[c] = (pips[c] ?: 0.0) + v }
            pips.keys.ifEmpty { deckCards.flatMap { ops.colorsOf(it.card) }.toSet() }.toSet()
        }
        // §3.2 pip demand + emergent colors.
        val demand = HashMap<String, Double>()
        for (pc in deckCards) DraftsimMana.pipCounts(pc.card.manaCost, a).forEach { (c, v) -> demand[c] = (demand[c] ?: 0.0) + v }
        val deckColorSet = HashSet(a)
        if (forcedColors.isEmpty()) demand.filterValues { it >= 2 }.keys.forEach { deckColorSet += it }
        // §3.3 useful pool lands (cap 17).
        val usefulLands = poolLands.filter { land ->
            val fix = ops.archRecord(land.card.name)?.fixing?.takeIf { it.isNotEmpty() }
                ?: ops.colorsOf(land.card).ifEmpty { land.card.colorIdentity }
            fix.isNotEmpty() && (fix.size >= 4 || fix.all { it in deckColorSet })
        }.take(DECK_LANDS)
        var basicsCount = max(0, DECK_LANDS - usefulLands.size)

        // §3.4 allocate basics proportional to pip demand.
        val basics = allocateBasics(demand, basicsCount)
        // §3.5 splash floor for demanded colors beyond the top 2.
        splashFloor(basics, demand, deckCards, usefulLands)
        // §3.6 trim to 17 total.
        trimLands(basics, usefulLands.size)

        return Manabase(usefulLands, basics.filterValues { it > 0 })
    }

    private fun allocateBasics(demand: Map<String, Double>, count: Int): MutableMap<String, Int> {
        val out = HashMap<String, Int>()
        val entries = demand.filterValues { it > 0 }.entries.sortedByDescending { it.value }
        if (entries.isEmpty() || count <= 0) return out
        if (entries.size == 1) { out[entries[0].key] = count; return out }
        val total = entries.sumOf { it.value }
        var assigned = 0
        val fractional = mutableListOf<Pair<String, Double>>()
        for ((color, d) in entries) {
            val exact = d / total * count
            val whole = floor(exact).toInt()
            out[color] = whole; assigned += whole
            fractional += color to (exact - whole)
        }
        var remainder = count - assigned
        for ((color, _) in fractional.sortedByDescending { it.second }) {
            if (remainder <= 0) break
            out[color] = (out[color] ?: 0) + 1; remainder--
        }
        return out
    }

    private fun splashFloor(basics: MutableMap<String, Int>, demand: Map<String, Double>, deckCards: List<DraftsimPoolCard>, usefulLands: List<DraftsimPoolCard>) {
        val demanded = demand.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }
        if (demanded.size <= 2) return
        for (color in demanded.drop(2)) {
            val maxReq = deckCards.maxOfOrNull { DraftsimMana.pipCounts(it.card.manaCost).getOrDefault(color, 0.0) } ?: 0.0
            if (maxReq <= 0) continue
            val target = maxReq + 3
            val landFix = usefulLands.count { land ->
                val fix = ops.archRecord(land.card.name)?.fixing?.takeIf { it.isNotEmpty() }
                    ?: ops.colorsOf(land.card).ifEmpty { land.card.colorIdentity }
                color in fix
            }
            val have = landFix + (basics[color] ?: 0)
            var shortfall = (target - have).toInt()
            while (shortfall > 0) {
                val donor = basics.entries.filter { it.key != color && it.value > 0 }.maxByOrNull { it.value } ?: break
                basics[donor.key] = donor.value - 1
                basics[color] = (basics[color] ?: 0) + 1
                shortfall--
            }
        }
    }

    private fun trimLands(basics: MutableMap<String, Int>, usefulCount: Int) {
        var excess = usefulCount + basics.values.sum() - DECK_LANDS
        while (excess > 0) {
            val biggest = basics.entries.filter { it.value > 0 }.maxByOrNull { it.value } ?: break
            basics[biggest.key] = biggest.value - 1
            excess--
        }
    }

    // =========================================================================
    // ek — post-build refinement (hill climbing)
    // =========================================================================

    private fun refine(build: DraftsimBuild, pool: List<DraftsimPoolCard>, iterations: Int = 3, forced: Set<String> = emptySet()): DraftsimBuild {
        val chosen = build.deckInstanceIds.toHashSet()
        val deckNonland = pool.filter { it.instanceId in chosen && !ops.isLand(it.card) }.toMutableList()
        var best = deckScorer.scoreNonlandSet(deckNonland.map { it.card })

        repeat(iterations) {
            val candidates = pool.filter {
                it.instanceId !in chosen && !ops.isLand(it.card) &&
                    (ops.colorsOf(it.card).isEmpty() || DraftsimMana.fitsColors(it.card.manaCost, build.colors))
            }
            if (candidates.isEmpty()) return@repeat
            val worstFirst = deckNonland.filter { it.instanceId !in forced }.sortedBy { ops.ratingOrDefault(it.card) }
            val bestFirst = candidates.sortedByDescending { ops.ratingOrDefault(it.card) }
            var improved = false
            outer@ for (out in worstFirst) {
                for (inn in bestFirst) {
                    if (ops.ratingOrDefault(inn.card) < ops.ratingOrDefault(out.card) - 0.5) break
                    val trial = deckNonland.filter { it != out } + inn
                    if (deckScorer.scoreNonlandSet(trial.map { it.card }) > best + 0.01) {
                        chosen -= out.instanceId; chosen += inn.instanceId
                        deckNonland -= out; deckNonland += inn
                        best = deckScorer.scoreNonlandSet(deckNonland.map { it.card })
                        improved = true
                        break@outer
                    }
                }
            }
            if (!improved) return@repeat
        }

        val poolLands = pool.filter { ops.isLand(it.card) && !ops.isBasic(it.card) }
        val mana = buildManabase(deckNonland, poolLands, build.colors)
        val deckIds = (deckNonland.map { it.instanceId } + mana.usefulPoolLands.map { it.instanceId }).toSet()
        val fullDeck = deckNonland.map { it.card } + mana.usefulPoolLands.map { it.card } + basics(mana.basicsNeeded).map { it.card }
        val final = deckScorer.scoreDeck(fullDeck)
        return build.copy(score = final.score, manaBaseScore = final.manaBaseScore, deckInstanceIds = deckIds, basicsNeeded = mana.basicsNeeded)
    }

    // =========================================================================
    // SX — sealed "good stuff" splash build
    // =========================================================================

    private fun goodStuffBuild(pool: List<DraftsimPoolCard>): DraftsimBuild? {
        val nonlandPC = pool.filter { !ops.isLand(it.card) }
        val poolLands = pool.filter { ops.isLand(it.card) && !ops.isBasic(it.card) }

        // 1. best 3-color shell by top-16 raw ratings.
        var bestCombo: Pair<String, String>? = null
        var bestTop: List<DraftsimPoolCard> = emptyList()
        var bestSum = Double.NEGATIVE_INFINITY
        for (combo in THREE_COLOR) {
            val colors = combo.first.map { it.toString() }
            val top = nonlandPC.filter { castable(it.card, colors) }
                .sortedByDescending { ops.ratingOrDefault(it.card) }.take(16)
            val sum = top.sumOf { ops.ratingOrDefault(it.card) }
            if (sum > bestSum) { bestSum = sum; bestCombo = combo; bestTop = top }
        }
        val combo = bestCombo ?: return null
        if (bestTop.size < 10) return null
        val colors = combo.first.map { it.toString() }
        val name = "${combo.second} Good Stuff"

        // 2. score all nonland as if forced into this shell.
        val forcedMap = mapOf(name to colors)
        val nonlandCards = nonlandPC.map { it.card }
        val scored = nonlandPC.map { pc ->
            Scored(pc, scorer.score(pc.card, nonlandCards, forcedMap, removalFlag = true, forcedArch = name))
        }.sortedWith(compareByDescending<Scored> { it.total }.thenByDescending { it.rawRating })

        // 3. greedy fill 23.
        val st = BuildState()
        val topIds = bestTop.map { it.instanceId }.toHashSet()
        var topTaken = 0
        for (item in scored) {
            if (topTaken >= 15 || st.deck.size >= DECK_NONLAND) break
            if (item.pc.instanceId in topIds && item.pc.instanceId !in st.chosen && castable(item.pc.card, colors) && st.canAdd(item)) {
                st.add(item); topTaken++
            }
        }
        var removalTaken = 0
        for (item in scored) {
            if (removalTaken >= 7 || st.deck.size >= DECK_NONLAND) break
            if (isRemoval(item.pc.card) && item.pc.instanceId !in st.chosen && castable(item.pc.card, colors) && st.canAdd(item)) {
                st.add(item); removalTaken++
            }
        }
        for (item in scored) { if (st.deck.size >= DECK_NONLAND) break; if (item.pc.instanceId !in st.chosen && castable(item.pc.card, colors) && st.canAdd(item)) st.add(item) }
        for (item in scored) { if (st.deck.size >= DECK_NONLAND) break; if (item.pc.instanceId !in st.chosen && castable(item.pc.card, colors) && (st.copies[item.pc.card.name] ?: 0) < copyCap(item.pc.card)) st.add(item) }
        for (item in scored) { if (st.deck.size >= DECK_NONLAND) break; if (item.pc.instanceId !in st.chosen && (st.copies[item.pc.card.name] ?: 0) < copyCap(item.pc.card)) st.add(item) }

        creatureFloor(st, scored)

        val chosenCards = st.deck.map { it.pc }
        val mana = buildManabase(chosenCards, poolLands, colors)
        val includedIds = (chosenCards.map { it.instanceId } + mana.usefulPoolLands.map { it.instanceId }).toSet()
        val fullDeck = chosenCards.map { it.card } + mana.usefulPoolLands.map { it.card } + basics(mana.basicsNeeded).map { it.card }
        val final = deckScorer.scoreDeck(fullDeck)
        return DraftsimBuild(name, colors, final.score, final.manaBaseScore, includedIds, mana.basicsNeeded)
    }
}

/** The ten three-color guild shells, shared with the deck scorer's good-stuff ranking. */
internal object DraftsimDeckScorerGuilds {
    val THREE_COLOR = listOf(
        "WUB" to "Esper", "WUR" to "Jeskai", "WUG" to "Bant", "WBR" to "Mardu", "WBG" to "Abzan",
        "WRG" to "Naya", "UBR" to "Grixis", "UBG" to "Sultai", "URG" to "Temur", "BRG" to "Jund",
    )
}
