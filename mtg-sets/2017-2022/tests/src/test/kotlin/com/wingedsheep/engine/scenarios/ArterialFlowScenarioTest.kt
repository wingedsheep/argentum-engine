package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Arterial Flow (RIX #62) — "Each opponent discards two cards. If you control a Vampire, each
 * opponent loses 2 life and you gain 2 life."
 */
class ArterialFlowScenarioTest : ScenarioTestBase() {

    private fun build(withVampire: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Arterial Flow")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInHand(2, "Grizzly Bears")
            .withCardInHand(2, "Shock")
            .withCardInHand(2, "Lightning Bolt")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withVampire) builder.withCardOnBattlefield(1, "Vampire Interloper")
        return builder.build()
    }

    private fun TestGame.castAndDiscardTwo() {
        castSpell(1, "Arterial Flow").error shouldBe null
        resolveStack()
        val discards = findCardsInHand(2, "Grizzly Bears") + findCardsInHand(2, "Shock")
        selectCards(discards).error shouldBe null
        resolveStack()
    }

    init {
        test("opponent discards two cards and, with a Vampire, the drain happens") {
            val game = build(withVampire = true)
            game.castAndDiscardTwo()

            game.handSize(2) shouldBe 1
            game.isInHand(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("without a Vampire only the discard happens") {
            val game = build(withVampire = false)
            game.castAndDiscardTwo()

            game.handSize(2) shouldBe 1
            game.getLifeTotal(2) shouldBe 20
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
