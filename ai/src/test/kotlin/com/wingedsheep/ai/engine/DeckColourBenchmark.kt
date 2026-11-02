package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimMana
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.draftsim.withFallbackRatings
import com.wingedsheep.ai.draftsim.withSynergyAxes
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import kotlin.random.Random

/**
 * How many colours the AI's limited builds ([buildLimitedDeck]) run, over seeded pools of one set:
 * eight-seat pods drafted by the AI's own pick scorer (the one [EngineAiPlayerController] drafts
 * with), and six-booster sealed pools. No games are played — this reads the decks only.
 *
 * Two counts per deck:
 *  - **identity**: every colour on a nonland card, hybrids counting for both halves — the count the
 *    win-rate split by colours is taken over;
 *  - **main + splash**: a colour more than [SPLASH_CARDS] cards strictly need (plain pips) is a main
 *    colour; one that only a few need is a splash.
 *
 * ```
 * just deck-colours 10 FRA
 * ```
 *
 * Disabled unless `-DdeckColours=true`, so a normal `:ai:test` run never pays for it.
 */
class DeckColourBenchmark : FunSpec({

    val enabled = System.getProperty("deckColours") == "true"
    val pods = System.getProperty("deckColourPods")?.toIntOrNull() ?: 10
    val setCode = System.getProperty("deckColourSet") ?: "FRA"
    val seed = System.getProperty("deckColourSeed")?.toLongOrNull() ?: 20261102L

    test("colour counts of the AI's limited builds").config(enabled = enabled) {
        val set = MtgSetCatalog.requireByCode(setCode)
        val byName = set.cards.associateBy { it.name }
        val rng = Random(seed)

        val draftPools = (0 until pods).flatMap { draftPod(set.cards, setCode, rng) { set.boosterStrategy.generate(set.cards, it) } }
        val sealedPools = (0 until pods * 8).map { (0 until 6).flatMap { set.boosterStrategy.generate(set.cards, rng) } }

        for ((label, pools) in listOf("draft" to draftPools, "sealed" to sealedPools)) {
            val decks = pools.map { buildLimitedDeck(it, listOf(setCode)) }
            val shapes = decks.map { shapeOf(it, byName) }
            println("=== DECK COLOURS: $setCode $label, ${decks.size} decks (seed $seed) ===")
            println("  identity colours: " + histogram(shapes.map { it.identity }))
            println("  main colours:     " + histogram(shapes.map { it.main }))
            println("  with a splash:    ${pct(shapes.count { it.splash > 0 }, shapes.size)}")
            println("  main+splash:      " + histogram(shapes.map { it.main + it.splash }))
            println("  hard splash card: ${pct(shapes.count { it.hardSplash }, shapes.size)}  (a splash card with two pips of it, or costing 3 or less)")
            println("  inverted basics:  ${pct(shapes.count { it.inverted }, shapes.size)}  (more basics of a colour than of one more cards need)")
        }
    }
}) {
    private data class Shape(val identity: Int, val main: Int, val splash: Int, val hardSplash: Boolean, val inverted: Boolean)

    private companion object {
        const val SPLASH_CARDS = 3
        const val POD = 8
        const val PICKS_PER_PACK = 14
        val BASICS = mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")

        /** Eight seats, three packs, passing left-right-left; each seat picks like the engine AI. */
        fun draftPod(
            cards: List<CardDefinition>, setCode: String, rng: Random, booster: (Random) -> List<CardDefinition>,
        ): List<List<CardDefinition>> {
            val tables = DraftsimData.tablesFor(listOf(setCode)).withFallbackRatings(cards).withSynergyAxes(cards)
            val scorer = DraftsimScorer(tables)
            val picks = List(POD) { mutableListOf<CardDefinition>() }
            for (round in 0 until 3) {
                var packs = List(POD) { booster(rng).toMutableList() }
                repeat(PICKS_PER_PACK) {
                    for (seat in 0 until POD) {
                        val pack = packs[seat]
                        if (pack.isEmpty()) continue
                        val scores = scorer.scoreBoosterAuto(pack.map { it.toScorerCard() }, picks[seat].map { it.toScorerCard() }, POD)
                        val pick = pack.maxBy { scores[it.name]?.total ?: 0.0 }
                        pack.remove(pick)
                        picks[seat] += pick
                    }
                    packs = if (round == 1) List(POD) { packs[(it + 1) % POD] } else List(POD) { packs[(it + POD - 1) % POD] }
                }
            }
            return picks
        }

        fun shapeOf(deck: Map<String, Int>, byName: Map<String, CardDefinition>): Shape {
            val nonland = deck.flatMap { (name, n) -> List(n) { byName[name] } }.filterNotNull()
                .filter { !it.typeLine.isLand }.map { it.toScorerCard() }
            val identity = nonland.flatMap { it.colors.ifEmpty { DraftsimMana.colorsInCost(it.manaCost) } }.toSet().size
            val needing = DraftsimMana.COLORS.associateWith { c ->
                nonland.count { (DraftsimMana.castRequirement(it.manaCost).plainPips[c] ?: 0) > 0 }
            }
            val splashColours = needing.filterValues { it in 1..SPLASH_CARDS }.keys
            val hardSplash = nonland.any { card ->
                val pips = DraftsimMana.castRequirement(card.manaCost).plainPips
                splashColours.any { c -> (pips[c] ?: 0) >= 2 || ((pips[c] ?: 0) == 1 && card.cmc <= 3.0) }
            }
            val basics = BASICS.mapValues { (_, name) -> deck[name] ?: 0 }
            val inverted = needing.entries.any { (a, na) ->
                needing.entries.any { (b, nb) -> nb > na + SPLASH_CARDS && basics.getValue(a) > basics.getValue(b) }
            }
            return Shape(
                identity, needing.values.count { it > SPLASH_CARDS }, splashColours.size, hardSplash, inverted,
            )
        }

        fun histogram(values: List<Int>): String =
            values.groupingBy { it }.eachCount().toSortedMap().entries
                .joinToString("  ") { (k, n) -> "$k: ${pct(n, values.size)}" }

        fun pct(n: Int, of: Int) = "%d (%.0f%%)".format(n, 100.0 * n / of.coerceAtLeast(1))
    }
}
