package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class PilferingHawkScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Pilfering Hawk")!!.activatedAbilities.single().id

    private fun board(land: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Pilfering Hawk")
        .withLandsOnBattlefield(1, land, 1)
        .withCardInHand(1, "Mountain")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateHawk() = execute(
        ActivateAbility(player1Id, findPermanent("Pilfering Hawk")!!, abilityId)
    )

    init {
        test("snow mana pays activation and newly drawn card can be discarded") {
            val game = board("Snow-Covered Island")
            val originalHand = game.state.getHand(game.player1Id).toSet()
            game.activateHawk().error shouldBe null
            game.state.getEntity(game.findPermanent("Pilfering Hawk")!!)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.findPermanent("Snow-Covered Island")!!)!!.has<TappedComponent>() shouldBe true
            game.resolveStack()

            game.state.getHand(game.player1Id).size shouldBe 2
            val drawn = game.state.getHand(game.player1Id).single { it !in originalHand }
            val decision = game.getPendingDecision() as SelectCardsDecision
            decision.options.contains(drawn) shouldBe true
            game.selectCards(listOf(drawn)).error shouldBe null
            game.state.getGraveyard(game.player1Id).contains(drawn) shouldBe true
            game.state.getHand(game.player1Id).map { game.state.getEntity(it)!!.get<CardComponent>()!!.name } shouldBe listOf("Mountain")
        }

        test("ordinary mana cannot pay snow cost and failed activation leaves Hawk untapped") {
            val game = board("Island")
            game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                .none { it.isAffordable } shouldBe true
            (game.activateHawk().error != null) shouldBe true
            game.state.getEntity(game.findPermanent("Pilfering Hawk")!!)!!.has<TappedComponent>() shouldBe false
            game.state.getHand(game.player1Id).size shouldBe 1
        }
    }
}
