package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MortuaryMireScenarioTest : ScenarioTestBase() {
    private fun mireGame() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Mortuary Mire")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(1, "Swamp")
        .withCardInGraveyard(2, "Savannah Lions")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enters tapped and returns the chosen own creature on top after accepting") {
            val game = mireGame()
            val mire = game.findCardsInHand(1, "Mortuary Mire").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.execute(PlayLand(game.player1Id, mire)).error shouldBe null
            game.state.getEntity(mire)!!.has<TappedComponent>() shouldBe true
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(bears, giant)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>().context.targetIds shouldBe listOf(bears)
            game.answerYesNo(true).error shouldBe null
            game.state.getLibrary(game.player1Id).first() shouldBe bears
            game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
            game.findCardsInGraveyard(1, "Hill Giant").single() shouldBe giant
        }

        test("declining leaves the targeted creature in the graveyard") {
            val game = mireGame()
            val mire = game.findCardsInHand(1, "Mortuary Mire").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val library = game.state.getLibrary(game.player1Id)
            game.execute(PlayLand(game.player1Id, mire)).error shouldBe null
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.findCardsInGraveyard(1, "Grizzly Bears").single() shouldBe bears
            game.state.getLibrary(game.player1Id) shouldBe library
            game.hasPendingDecision() shouldBe false
        }

        test("an empty graveyard does not prevent playing the land") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Mortuary Mire")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mire = game.findCardsInHand(1, "Mortuary Mire").single()
            game.execute(PlayLand(game.player1Id, mire)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(mire)!!.has<TappedComponent>() shouldBe true
            game.hasPendingDecision() shouldBe false
        }
    }
}
