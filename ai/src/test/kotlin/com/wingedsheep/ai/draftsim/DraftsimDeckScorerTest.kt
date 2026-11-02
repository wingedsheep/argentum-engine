package com.wingedsheep.ai.draftsim

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/** Full control over every scorer field — lets the manabase Monte-Carlo see real land color identities. */
private data class TestCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String,
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
    override val rewardsColors: Boolean = false,
) : ScorerCard

/**
 * Stage 4: archetype ranking (`kf`) and the final deck score (`Mm`/`Wm`/`Gm`/`G4`). Pins the
 * precisely-specified pieces — kf ordering, the 0–10 score envelope, and the seeded manabase
 * Monte-Carlo (determinism + that fixable beats unfixable).
 */
class DraftsimDeckScorerTest : FunSpec({

    fun spell(name: String, cost: String, color: String) =
        TestCard(name, cost, "Creature", colors = listOf(color))

    fun land(name: String, color: String) =
        TestCard(name, "", "Basic Land", colorIdentity = listOf(color))

    test("kf ranks the dominant color pair on top (untagged path)") {
        // A realistic pool spanning all colors (so no pair is empty), but the WU cards are the only
        // ones rated above the baseline — WU should accrue the most quality and rank first.
        val wuRatings = (1..12).associate { "wu$it" to 3.0 }
        val fillerRatings = listOf("B", "R", "G").flatMap { c -> (1..6).map { "$c$it" to 2.0 } }.toMap()
        val scorer = DraftsimDeckScorer(DraftsimSetTables(wuRatings + fillerRatings, emptySet(), emptyMap()))
        val pool = (1..12).map { spell("wu$it", if (it % 2 == 0) "{1}{W}" else "{1}{U}", if (it % 2 == 0) "W" else "U") } +
            listOf("B", "R", "G").flatMap { c -> (1..6).map { spell("$c$it", "{1}{$c}", c) } }

        val ranked = scorer.rankArchetypes(pool, emptyMap())
        ranked.first().name shouldBe "WU"
    }

    test("a deck score and manabase score both fall in 0–10") {
        val ratings = (1..23).associate { "r$it" to 2.8 }
        val scorer = DraftsimDeckScorer(DraftsimSetTables(ratings, emptySet(), emptyMap()))
        val deck = (1..23).map { spell("r$it", "{1}{R}", "R") } + (1..17).map { land("Mountain$it", "R") }

        val result = scorer.scoreDeck(deck)
        result.score shouldBeGreaterThanOrEqual 0.0
        result.score shouldBeLessThanOrEqual 10.0
        result.manaBaseScore shouldBeGreaterThanOrEqual 0.0
        result.manaBaseScore shouldBeLessThanOrEqual 10.0
    }

    test("manabase Monte-Carlo is deterministic for a fixed deck") {
        val scorer = DraftsimDeckScorer(DraftsimSetTables(emptyMap(), emptySet(), emptyMap()))
        val deck = (1..23).map { spell("r$it", "{1}{R}", "R") } + (1..17).map { land("Mountain$it", "R") }
        scorer.manabasePlayability(deck) shouldBe scorer.manabasePlayability(deck)
    }

    test("a mono-color manabase is more castable than an unfixable two-color one") {
        val scorer = DraftsimDeckScorer(DraftsimSetTables(emptyMap(), emptySet(), emptyMap()))
        val mono = (1..23).map { spell("r$it", "{R}", "R") } + (1..17).map { land("Mountain$it", "R") }
        // Same lands (all Mountains) but half the spells need blue ⇒ those hands can't be cast.
        val splitColors = (1..23).map { i ->
            if (i % 2 == 0) spell("u$i", "{U}", "U") else spell("r$i", "{R}", "R")
        } + (1..17).map { land("Mountain$it", "R") }

        scorer.manabasePlayability(mono) shouldBeGreaterThan scorer.manabasePlayability(splitColors)
    }

    test("bombs use the raw rating, so an unrated card is never a bomb") {
        // 'mythic' rarity but no map rating ⇒ rating??B = 2 < 3.9 ⇒ contributes no bombScore.
        val scorer = DraftsimDeckScorer(DraftsimSetTables(emptyMap(), emptySet(), emptyMap()))
        val pool = (1..6).map { TestCard("x$it", "{1}{R}", "Creature", colors = listOf("R"), rarity = "mythic") }
        val ranked = scorer.rankArchetypes(pool, emptyMap())
        ranked.all { it.bombScore == 0.0 } shouldBe true
    }

    // The final score's quality term saturates on any real deck, so colour choices are weighed by
    // castable power: the spells' ratings, each scaled by how reliably the manabase casts it on time.
    fun deck(perColor: Map<String, Int>, basics: Map<String, Int>, rewards: Int = 0): List<TestCard> {
        var n = 0
        val spells = perColor.flatMap { (c, k) -> (1..k).map { TestCard("s${n++}", "{1}{$c}", "Creature", colors = listOf(c)) } }
            .mapIndexed { i, card -> if (i < rewards) card.copy(rewardsColors = true) else card }
        return spells + basics.flatMap { (c, k) -> (1..k).map { land("basic$c$it", c) } }
    }
    val flat = DraftsimSetTables((0 until 23).associate { "s$it" to 3.0 }, emptySet(), emptyMap())

    test("castable power charges a third colour on basics for every spell it makes less reliable") {
        val scorer = DraftsimDeckScorer(flat)
        val twoColour = scorer.scoreDeck(deck(mapOf("W" to 12, "G" to 11), mapOf("W" to 9, "G" to 8)))
        val threeColour = scorer.scoreDeck(deck(mapOf("W" to 8, "G" to 8, "U" to 7), mapOf("W" to 6, "G" to 6, "U" to 5)))
        twoColour.castablePower shouldBeGreaterThan threeColour.castablePower
    }

    test("a colour-count payoff is worth more in a three-colour deck") {
        val scorer = DraftsimDeckScorer(flat)
        val colours = mapOf("W" to 8, "G" to 8, "U" to 7)
        val basics = mapOf("W" to 6, "G" to 6, "U" to 5)
        scorer.scoreDeck(deck(colours, basics, rewards = 6)).castablePower shouldBeGreaterThan
            scorer.scoreDeck(deck(colours, basics)).castablePower
    }
})
