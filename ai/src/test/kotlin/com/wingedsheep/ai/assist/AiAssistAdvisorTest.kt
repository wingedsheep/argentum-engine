package com.wingedsheep.ai.assist

import com.wingedsheep.ai.engine.LimitedPickScorer
import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine-level coverage for the AI-assist heuristic engines: the shared pick scorer, the draft
 * advisor's recommendation, and the deckbuild advisor's fresh-build vs. partial-completion modes.
 */
class AiAssistAdvisorTest : FunSpec({

    fun summary(
        name: String,
        rarity: String,
        manaCost: String,
        power: Int? = null,
        toughness: Int? = null,
        oracle: String? = null,
        typeLine: String = "Creature",
    ) = CardSummary(
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        rarity = rarity,
        power = power,
        toughness = toughness,
        oracleText = oracle,
    )

    // ----- LimitedPickScorer -----

    test("a rare removal bomb outscores a vanilla common") {
        val bomb = summary("Dragon", "RARE", "{3}{R}{R}", 5, 5, "Flying. When this enters, destroy target creature.")
        val vanilla = summary("Bear", "COMMON", "{1}{G}", 2, 2)

        val bombScore = LimitedPickScorer.score(bomb, emptyMap(), emptyList())
        val vanillaScore = LimitedPickScorer.score(vanilla, emptyMap(), emptyList())

        bombScore shouldBeGreaterThan vanillaScore
    }

    test("off-color cards are penalized once colors are committed") {
        // Player is committed to green (8+ green picks made).
        val greenPicks = (1..8).map { summary("Green Pick $it", "COMMON", "{1}{G}", 2, 2) }
        val committed = LimitedPickScorer.inferColors(greenPicks)

        val blue = summary("Blue Drake", "COMMON", "{2}{U}", 2, 2)
        val onColorScore = LimitedPickScorer.score(blue, emptyMap(), emptyList())
        val offColorScore = LimitedPickScorer.score(blue, committed, greenPicks)

        offColorScore shouldBeLessThan onColorScore
    }

    test("reason flags off-color only once the off-color penalty applies (>=8 picks)") {
        val greenPicks = (1..8).map { summary("Green Pick $it", "COMMON", "{1}{G}", 2, 2) }
        val committed = LimitedPickScorer.inferColors(greenPicks)
        val blue = summary("Blue Drake", "COMMON", "{2}{U}", 2, 2)

        // Past the exploratory window: score() penalizes, so reason() should say off-color.
        LimitedPickScorer.reason(blue, committed, greenPicks) shouldBe "Off-color for your current picks"

        // Early picks (< 8): score() does not penalize, so reason() must not claim off-color.
        val earlyPicks = greenPicks.take(3)
        LimitedPickScorer.reason(blue, committed, earlyPicks) shouldNotBe "Off-color for your current picks"
    }

    // ----- HeuristicDraftAdvisor -----

    test("draft advisor scores every card and recommends the strongest picks") {
        val pack = listOf(
            summary("Bomb", "MYTHIC", "{2}{R}{R}", 4, 4, "Flying. When this enters, destroy target creature."),
            summary("Solid", "UNCOMMON", "{1}{R}", 2, 2),
            summary("Filler", "COMMON", "{2}{R}", 1, 3),
            summary("Weak", "COMMON", "{5}{R}", 1, 1),
        )

        val advice = HeuristicDraftAdvisor.suggestPick(
            DraftPickAdviceRequest(pack = pack, picksRequired = 2)
        )

        advice.advisorId shouldBe "heuristic"
        advice.scores shouldHaveSize 4
        advice.recommended shouldHaveSize 2
        // Scores normalize into the 0–100 display band.
        advice.scores.forEach {
            (it.score in 0..100) shouldBe true
            it.reason.isNotBlank() shouldBe true
        }
        // The bomb must be one of the two recommended cards.
        advice.recommended shouldContain "Bomb"
        // Recommendations are the top of the score-sorted list.
        advice.recommended shouldContainExactlyInAnyOrder advice.scores.take(2).map { it.cardName }
    }

    // ----- HeuristicDeckBuildAdvisor / buildHeuristicSealedDeck -----

    fun creature(name: String, manaCost: String, rarity: Rarity = Rarity.COMMON) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity),
    )

    // A two-color pool (green-heavy) deep enough to build a 40-card deck.
    fun pool(): List<CardDefinition> =
        (1..14).map { creature("Green $it", "{1}{G}") } +
            (1..10).map { creature("Red $it", "{2}{R}") } +
            (1..4).map { creature("Blue $it", "{3}{U}") }

    val basics = listOf(
        CardDefinition.basicLand("Plains", Subtype.PLAINS),
        CardDefinition.basicLand("Island", Subtype.ISLAND),
        CardDefinition.basicLand("Swamp", Subtype.SWAMP),
        CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN),
        CardDefinition.basicLand("Forest", Subtype.FOREST),
    )

    test("fresh build produces a full 40-card deck with lands") {
        val deck = buildHeuristicSealedDeck(pool())

        deck.values.sum() shouldBe 40
        val basicLands = deck.filterKeys { it in setOf("Plains", "Island", "Swamp", "Mountain", "Forest") }
        basicLands.values.sum() shouldBeGreaterThan 0
    }

    test("deckbuild advisor builds a 40-card deck and scores it") {
        val result = HeuristicDeckBuildAdvisor.buildDeck(
            DeckBuildRequest(pool = pool(), availableBasics = basics, targetSize = 40)
        )

        result.advisorId shouldBe "heuristic"
        val build = result.builds.single()
        build.deckList.values.sum() shouldBe 40
        (build.score ?: 0.0) shouldBeGreaterThan 0.0
    }

    test("completion mode keeps locked cards and fills to the target size") {
        val locked = mapOf("Green 1" to 3, "Green 2" to 2)

        val result = HeuristicDeckBuildAdvisor.buildDeck(
            DeckBuildRequest(pool = pool(), availableBasics = basics, locked = locked, targetSize = 40)
        )

        // Every locked card is preserved at (at least) its locked count.
        val deckList = result.builds.single().deckList
        deckList["Green 1"]!! shouldBeGreaterThan 2
        deckList["Green 2"]!! shouldBeGreaterThan 1
        deckList.values.sum() shouldBe 40
    }

    test("completion respects pool copy limits — never exceeds available copies") {
        // Pool has exactly one copy of this card; locking it must not let the builder add more.
        val singleton = listOf(creature("Unique Card", "{1}{G}")) + (1..20).map { creature("Bulk $it", "{2}{R}") }
        val result = buildHeuristicSealedDeck(
            pool = singleton,
            locked = mapOf("Unique Card" to 1),
            targetSize = 40,
            resolveCard = { name -> singleton.firstOrNull { it.name == name } },
        )

        result["Unique Card"] shouldBe 1
    }
})
