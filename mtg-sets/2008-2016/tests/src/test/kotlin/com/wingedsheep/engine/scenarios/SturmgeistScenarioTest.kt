package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sturmgeist (ISD #82) — {3}{U}{U} * / * Spirit.
 *
 *   Flying
 *   Sturmgeist's power and toughness are each equal to the number of cards in your hand.
 *   Whenever Sturmgeist deals combat damage to a player, draw a card.
 */
class SturmgeistScenarioTest : ScenarioTestBase() {

    init {
        test("P/T tracks your hand size, and combat damage to a player draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sturmgeist")
                .withCardInHand(1, "Island")
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val geist = game.findPermanent("Sturmgeist")!!
            withClue("two cards in your hand; the opponent's hand doesn't count") {
                game.state.projectedState.getPower(geist) shouldBe 2
                game.state.projectedState.getToughness(geist) shouldBe 2
            }
            game.state.projectedState.hasKeyword(geist, Keyword.FLYING) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sturmgeist" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe 3
            game.isInHand(1, "Plains") shouldBe true
            game.state.projectedState.getPower(geist) shouldBe 3
        }
    }
}
