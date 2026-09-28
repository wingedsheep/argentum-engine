package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class CoercionScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("caster chooses which card the opponent discards") {
            val game = base().withCardInHand(1, "Coercion")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Forest").withCardInHand(2, "Grizzly Bears").build()
            game.castSpellTargetingPlayer(1, "Coercion", 2).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.playerId shouldBe game.player1Id
            val bear = game.state.getHand(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            game.selectCards(listOf(bear)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInHand(2, "Forest") shouldBe true
        }

        test("empty hand resolves without a card choice") {
            val game = base().withCardInHand(1, "Coercion")
                .withLandsOnBattlefield(1, "Swamp", 3).build()
            game.castSpellTargetingPlayer(1, "Coercion", 2).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Coercion") shouldBe true
        }
    }
}
