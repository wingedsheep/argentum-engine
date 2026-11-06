package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Battlefield Scavenger (AKH #118).
 *
 *   You may exert this creature as it attacks.
 *   Whenever you exert a creature, you may discard a card. If you do, draw a card.
 */
class BattlefieldScavengerScenarioTest : ScenarioTestBase() {

    init {
        context("Battlefield Scavenger") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Battlefield Scavenger")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting it lets you discard a card to draw a card") {
                val game = game()
                val scavenger = game.findPermanent("Battlefield Scavenger")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Battlefield Scavenger" to 2)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(scavenger))
                game.state.getEntity(scavenger)!!.has<ExertedComponent>() shouldBe true

                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.answerYesNo(true).error shouldBe null
                if (game.hasPendingDecision()) game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the discarded card is in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("the draw replaced it") {
                    game.isInHand(1, "Island") shouldBe true
                    game.handSize(1) shouldBe 1
                }
            }

            test("attacking without exerting puts no trigger on the stack") {
                val game = game()
                val scavenger = game.findPermanent("Battlefield Scavenger")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Battlefield Scavenger" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(scavenger)!!.has<ExertedComponent>() shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
