package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bishop of Binding (RIX #2) — "When this creature enters, exile target creature an opponent
 * controls until this creature leaves the battlefield. Whenever this creature attacks, target
 * Vampire gets +X/+X until end of turn, where X is the power of the exiled card."
 *
 * X reads the power of the card in the Bishop's linked-exile pile.
 */
class BishopOfBindingScenarioTest : ScenarioTestBase() {

    init {
        test("attack pumps a Vampire by the exiled card's power; the card returns when the Bishop leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bishop of Binding")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Bishop of Binding").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(wurm)).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Craw Wurm") shouldBe true

            val bishop = game.findPermanent("Bishop of Binding")!!
            // The Bishop was just cast: drop its summoning sickness so it can attack this turn.
            game.state = game.state.updateEntity(bishop) { it.without<SummoningSicknessComponent>() }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bishop of Binding" to 2)).error shouldBe null
            game.selectTargets(listOf(bishop)).error shouldBe null
            game.resolveStack()

            // Craw Wurm is 6/4: the 1/1 Bishop becomes 7/7.
            game.state.projectedState.getPower(bishop) shouldBe 7
            game.state.projectedState.getToughness(bishop) shouldBe 7

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.castSpell(1, "Doom Blade", bishop).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Bishop of Binding") shouldBe false
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
