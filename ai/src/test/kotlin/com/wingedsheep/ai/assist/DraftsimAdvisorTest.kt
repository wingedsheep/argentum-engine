package com.wingedsheep.ai.assist

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Stage 6: the Draftsim engines wired as advisors, driven end-to-end with real LTR cards through the
 * same SPI the server calls. Verifies the engine is registered, scores a pack, and builds a deck.
 */
class DraftsimAdvisorTest : FunSpec({

    val ltr = MtgSetCatalog.all.first { it.code == "LTR" }

    fun CardDefinition.toSummary() = CardSummary(
        name = name,
        manaCost = if (manaCost.symbols.isEmpty()) null else manaCost.toString(),
        typeLine = typeLine.toString(),
        rarity = metadata.rarity.name,
        power = creatureStats?.basePower,
        toughness = creatureStats?.baseToughness,
    )

    test("Draftsim is registered in the catalog under both surfaces") {
        AdvisorCatalog.draftAdvisor("draftsim") shouldBe DraftsimDraftAdvisor
        AdvisorCatalog.deckBuildAdvisor("draftsim") shouldBe DraftsimDeckBuildAdvisor
    }

    test("draft advisor scores an LTR pack and recommends the best pick(s)") {
        val pack = ltr.cards.filterNot { it.typeLine.isBasicLand }.take(15).map { it.toSummary() }
        val advice = DraftsimDraftAdvisor.suggestPick(
            DraftPickAdviceRequest(pack = pack, picksRequired = 2, setCodes = listOf("LTR")),
        )

        advice.advisorId shouldBe "draftsim"
        advice.scores shouldHaveSize pack.size
        advice.recommended shouldHaveSize 2
        advice.scores.forEach {
            (it.score in 0..100) shouldBe true
            it.reason.isNotBlank() shouldBe true
        }
    }

    test("draft advisor still works for a set with no Draftsim table (rarity fallback)") {
        val pack = ltr.cards.filterNot { it.typeLine.isBasicLand }.take(8).map { it.toSummary() }
        val advice = DraftsimDraftAdvisor.suggestPick(
            DraftPickAdviceRequest(pack = pack, setCodes = listOf("ZZZ")),  // no table
        )
        advice.scores shouldHaveSize pack.size
        advice.recommended shouldHaveSize 1
    }

    test("deckbuild advisor builds a ~40-card deck from an LTR pool") {
        // A pool large enough to support a build: all booster-legal LTR spells (one copy each).
        val pool = ltr.cards.filter { it.metadata.let { m -> m.rarity != null } && !it.typeLine.isBasicLand }
        val result = DraftsimDeckBuildAdvisor.buildDeck(
            DeckBuildRequest(pool = pool, targetSize = 40, setCodes = listOf("LTR")),
        )

        result.advisorId shouldBe "draftsim"
        // A fresh Auto-build surfaces several alternative decks (up to 3), ordered best-first.
        (result.builds.size in 2..3) shouldBe true
        result.recommended shouldBe 0
        result.builds.zipWithNext().all { (a, b) -> (a.score ?: 0.0) >= (b.score ?: 0.0) } shouldBe true
        val best = result.builds.first()
        best.deckList.values.sum() shouldBeGreaterThan 30
        // A real archetype/colour label is attached.
        (best.archetype != null) shouldBe true
        (best.score != null) shouldBe true
        best.colors.isNotEmpty() shouldBe true
    }

    test("deckbuild advisor completes a partial deck without dropping the locked cards") {
        val pool = ltr.cards.filter { it.metadata.let { m -> m.rarity != null } && !it.typeLine.isBasicLand }
        // Lock a handful of real nonland spells from the pool; "Complete Deck" must keep every one.
        val locked = pool.filterNot { it.typeLine.isLand }.take(6).associate { it.name to 1 }

        val result = DraftsimDeckBuildAdvisor.buildDeck(
            DeckBuildRequest(pool = pool, locked = locked, targetSize = 40, setCodes = listOf("LTR")),
        )

        result.advisorId shouldBe "draftsim"
        // Completion mode yields exactly one deck (no archetype alternatives to choose from).
        val build = result.builds.single()
        for ((name, count) in locked) {
            (build.deckList[name] ?: 0) shouldBe count
        }
        build.deckList.values.sum() shouldBeGreaterThan 30
    }
})
