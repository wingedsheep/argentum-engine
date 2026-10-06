package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class IllusionaryMaskScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Illusionary Mask")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(1, "Prodigal Sorcerer")
            .withCardInHand(1, "Craw Wurm")
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun activate(game: TestGame, x: Int) = game.execute(ActivateAbility(
            game.player1Id, game.findPermanent("Illusionary Mask")!!,
            cardRegistry.getCard("Illusionary Mask")!!.script.activatedAbilities.single().id,
            xValue = x,
        ))

        test("{G}{G} spent on X offers only the creature it could pay, cast face down for free") {
            val game = board()
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()

            activate(game, 2).error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() as SelectCardsDecision).options shouldBe listOf(bears)
            game.selectCards(listOf(bears))
            game.answerYesNo(true)
            game.resolveStack()

            game.state.getEntity(bears)!!.has<FaceDownComponent>() shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 2
            game.isInHand(1, "Prodigal Sorcerer") shouldBe true
        }

        test("declaring it as an attacker taps it, which turns it face up instead") {
            val game = board()
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            activate(game, 2)
            game.resolveStack()
            game.selectCards(listOf(bears))
            game.answerYesNo(true)
            game.resolveStack()

            game.state = game.state.updateEntity(bears) { it.without<SummoningSicknessComponent>() }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.getEntity(bears)!!.has<FaceDownComponent>() shouldBe true

            game.execute(com.wingedsheep.engine.core.DeclareAttackers(
                game.player1Id, mapOf(bears to game.player2Id)
            )).error shouldBe null

            game.state.getEntity(bears)!!.has<FaceDownComponent>() shouldBe false
        }
    }
}
