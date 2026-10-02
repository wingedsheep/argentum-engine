package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SpringheartNantukoScenarioTest : ScenarioTestBase() {

    private fun TestGame.bestowOnto(host: EntityId) {
        execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Springheart Nantuko").single(),
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun TestGame.playForest() {
        execute(PlayLand(player1Id, findCardsInHand(1, "Forest").first())).error shouldBe null
    }

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay().error shouldBe null
    }

    private fun bestowedBoard(hostController: Int = 1) = scenario().withPlayers()
        .withCardOnBattlefield(hostController, "Grizzly Bears")
        .withCardInHand(1, "Springheart Nantuko")
        .withCardInHand(1, "Forest")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("bestowed, the enchanted creature gets +1/+1 and paying {1}{G} copies it on landfall") {
            val game = bestowedBoard()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3

            game.playForest()
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            val allBears = game.findPermanents("Grizzly Bears")
            allBears.size shouldBe 2
            val copy = allBears.single { it != bears }
            // The copy has the printed 2/2 — the Aura's +1/+1 is not a copiable value.
            game.state.projectedState.getPower(copy) shouldBe 2
            game.findPermanents("Insect Token").size shouldBe 0
        }

        test("bestowed but declining to pay still creates a 1/1 green Insect") {
            val game = bestowedBoard()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)

            game.playForest()
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanents("Grizzly Bears").size shouldBe 1
            val insect = game.findPermanents("Insect Token").single()
            game.state.projectedState.getPower(insect) shouldBe 1
            game.state.projectedState.getToughness(insect) shouldBe 1
        }

        test("attached to a creature you don't control, no payment is offered and an Insect is created") {
            val game = bestowedBoard(hostController = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)
            game.state.projectedState.getPower(bears) shouldBe 3

            game.playForest()
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Grizzly Bears").size shouldBe 1
            game.findPermanents("Insect Token").size shouldBe 1
        }

        test("as an unattached creature, landfall creates an Insect without offering the payment") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Springheart Nantuko")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.playForest()
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Springheart Nantuko").size shouldBe 1
            game.findPermanents("Insect Token").size shouldBe 1
        }
    }
}
