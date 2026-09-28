package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Volcanic Spite (MOM #170): 3 damage to target creature/planeswalker/battle, then you may put a
 * card from hand on the bottom of your library; if you do, draw a card.
 */
class VolcanicSpiteScenarioTest : ScenarioTestBase() {
    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Volcanic Spite")
        .withCardInHand(1, "Forest")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("deals 3 damage, then tucking a hand card draws a card") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Volcanic Spite", bears).error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(decision.options.take(1))
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            // Forest tucked (-1), one card drawn (+1).
            game.handSize(1) shouldBe 1
        }

        test("declining the tuck draws nothing") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Volcanic Spite", bears).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList())
            game.handSize(1) shouldBe 1 // Forest only
            game.isInGraveyard(1, "Volcanic Spite") shouldBe true
        }
    }
}
