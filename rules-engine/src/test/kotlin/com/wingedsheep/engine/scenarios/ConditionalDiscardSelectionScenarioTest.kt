package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ConditionalDiscardSelectionScenarioTest : ScenarioTestBase() {

    private val conditionalDiscard = card("Conditional Discard") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "Discard two cards unless you discard a creature card."
        spell {
            effect = Effects.DiscardUnlessMatching(2, GameObjectFilter.Creature)
        }
    }

    init {
        cardRegistry.register(conditionalDiscard)

        fun buildGame() = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Conditional Discard")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(1, "Forest")
            .withCardInHand(1, "Mountain")
            .withLandsOnBattlefield(1, "Island", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("one creature card satisfies discard two unless discard a creature") {
            val game = buildGame()

            game.castSpell(1, "Conditional Discard")
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            val bearsId = game.findCardsInHand(1, "Grizzly Bears").single()
            val forestId = game.findCardsInHand(1, "Forest").single()

            decision.minSelections shouldBe 1
            decision.maxSelections shouldBe 2
            decision.conditionalMinimums.single().matchingOptions shouldContain bearsId

            game.selectCards(listOf(forestId)).error.shouldNotBeNull()
            game.getPendingDecision().shouldNotBeNull()

            game.selectCards(listOf(bearsId)).error shouldBe null
            game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInHand(1, "Forest").size shouldBe 1
        }

        test("two noncreature cards satisfy the normal discard count") {
            val game = buildGame()

            game.castSpell(1, "Conditional Discard")
            game.resolveStack()

            val forestId = game.findCardsInHand(1, "Forest").single()
            val mountainId = game.findCardsInHand(1, "Mountain").single()

            game.selectCards(listOf(forestId, mountainId)).error shouldBe null
            game.findCardsInGraveyard(1, "Forest").size shouldBe 1
            game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
        }
    }
}
