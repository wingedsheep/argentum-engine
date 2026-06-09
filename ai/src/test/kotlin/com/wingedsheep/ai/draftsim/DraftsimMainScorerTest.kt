package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Stage 3: the archetype-aware scorer `jm`. The land-fixing/splash sub-branches are best-effort from
 * the spec prose, so these tests pin the precisely-specified behavior: the basic-land sentinel, base
 * quality, removal pacing, the curve filler, the legend-copy cap, and that a real tagged set (TMT)
 * detects an archetype and rewards on-archetype cards.
 */
class DraftsimMainScorerTest : FunSpec({

    fun summary(name: String, cost: String, rarity: String = "common", type: String = "Creature") =
        CardSummary(name = name, manaCost = cost, typeLine = type, rarity = rarity).toScorerCard()

    test("a basic land returns the never-draft sentinel") {
        val s = DraftsimScorer(DraftsimSetTables(emptyMap(), emptySet(), emptyMap()))
        val forest = CardSummary(name = "Forest", manaCost = null, typeLine = "Basic Land — Forest").toScorerCard()
        val score = s.score(forest, emptyList())
        score.total shouldBe -1.0
        score.reasons.first() shouldBe "Basic land: always available, never draft"
    }

    test("base quality reflects the map rating plus rarity bonus") {
        val s = DraftsimScorer(DraftsimSetTables(mapOf("angel" to 3.0), emptySet(), emptyMap()))
        val score = s.score(summary("Angel", "{3}{W}", rarity = "mythic"), emptyList())
        // h<4 ⇒ early branch; base is rating 3.0 + mythic rarity bonus 0.15.
        score.rawRating shouldBe 3.0
        score.total shouldBe 3.15
    }

    test("removal is rewarded over an identical non-removal card (deckbuild removalFlag path)") {
        val ratings = mapOf("zap" to 2.5, "bear" to 2.5) +
            (1..6).associate { "red$it" to 2.5 }
        val s = DraftsimScorer(DraftsimSetTables(ratings, removal = setOf("zap"), archetypes = emptyMap()))
        val pool = (1..6).map { summary("red$it", "{1}{R}") }
        val archColors = mapOf("R" to listOf("R"))

        val zap = s.score(summary("Zap", "{R}", type = "Instant"), pool, archColors, removalFlag = true, forcedArch = "R")
        val bear = s.score(summary("Bear", "{1}{R}"), pool, archColors, removalFlag = true, forcedArch = "R")

        zap.total shouldBeGreaterThan bear.total
        zap.reasons.any { it.contains("Removal") } shouldBe true
    }

    test("a card that fills an empty curve slot beats one that overcrowds it") {
        // Pool: six 3-drops on-color ⇒ the 2-slot is empty and the 3-slot is overcrowded.
        val ratings = (1..6).associate { "three$it" to 2.5 } + mapOf("two" to 2.5, "anotherthree" to 2.5)
        val s = DraftsimScorer(DraftsimSetTables(ratings, emptySet(), emptyMap()))
        val pool = (1..6).map { summary("three$it", "{2}{G}") }
        val archColors = mapOf("G" to listOf("G"))

        val twoDrop = s.score(summary("Two", "{1}{G}"), pool, archColors, forcedArch = "G")
        val threeDrop = s.score(summary("AnotherThree", "{2}{G}"), pool, archColors, forcedArch = "G")

        twoDrop.reasons.any { it.contains("Fills curve") } shouldBe true
        twoDrop.total shouldBeGreaterThan threeDrop.total
    }

    test("a third copy of a legend is penalized") {
        val ratings = (1..5).associate { "filler$it" to 2.5 } + mapOf("hero" to 2.5)
        val s = DraftsimScorer(DraftsimSetTables(ratings, emptySet(), emptyMap()))
        val pool = (1..5).map { summary("filler$it", "{1}{W}") } +
            listOf(summary("Hero", "{1}{W}", type = "Legendary Creature — Human"),
                   summary("Hero", "{1}{W}", type = "Legendary Creature — Human"))
        val hero = s.score(summary("Hero", "{1}{W}", type = "Legendary Creature — Human"), pool,
            mapOf("W" to listOf("W")), forcedArch = "W")
        hero.reasons.any { it.contains("3rd copy") } shouldBe true
    }

    test("a tagged set (TMT) detects an archetype direction in deckContext") {
        val tmt = DraftsimData.tablesFor(listOf("TMT"))
        val s = DraftsimScorer(tmt)
        // Build a pool of real TMT cards that carry archetype tags.
        val tmtSet = MtgSetCatalog.all.first { it.code == "TMT" }
        val tagged = tmtSet.cards
            .filter { tmt.archetypes.containsKey(DraftsimData.nameKey(it.name)) && !it.typeLine.isLand }
            .take(8)
            .map { it.toScorerCard() }
        // Assert up front so a data regression that drops TMT's archetype tags fails here loudly
        // rather than silently skipping the real assertion below (vacuous green).
        tagged.size shouldBeGreaterThanOrEqual 5

        val archColors = s.archColorMap(tagged)
        val score = s.score(tagged.first(), tagged, archColors)
        // With archetype data present, the deck context names an archetype (not just colors).
        (score.deckContext.primary != null) shouldBe true
    }
})
