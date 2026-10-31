package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Deathless Knight: "When you gain life for the first time each turn, return this card from your
 * graveyard to your hand." A graveyard-zone trigger on the per-player first-life-gain flag.
 */
class DeathlessKnightScenarioTest : ScenarioTestBase() {
    init {
        fun board(knightInGraveyard: Boolean) = scenario().withPlayers("Owner", "Opponent").apply {
            if (knightInGraveyard) withCardInGraveyard(1, "Deathless Knight")
            else withCardOnBattlefield(1, "Deathless Knight")
        }
            .withCardsInHand(1, "Sacred Nectar", 2)
            .withCardInHand(2, "Sacred Nectar")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withLandsOnBattlefield(2, "Plains", 2)
            .withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        test("first life gain of the turn returns the Knight from graveyard to hand") {
            val game = board(knightInGraveyard = true)
            game.castSpell(1, "Sacred Nectar").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 24
            game.isInHand(1, "Deathless Knight") shouldBe true
            game.isInGraveyard(1, "Deathless Knight") shouldBe false
        }

        test("Knight reaching the graveyard after you already gained life this turn does not return") {
            val game = board(knightInGraveyard = false)
            game.castSpell(1, "Sacred Nectar").error shouldBe null
            game.resolveStack()
            val knight = game.findPermanent("Deathless Knight")!!
            game.state = zones.moveToZone(game.state, knight, Zone.GRAVEYARD).state
            game.castSpell(1, "Sacred Nectar").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 28
            game.isInGraveyard(1, "Deathless Knight") shouldBe true
            game.isInHand(1, "Deathless Knight") shouldBe false
        }

        test("an opponent gaining life does not return the Knight") {
            val game = board(knightInGraveyard = true)
            game.state = game.state.copy(activePlayerId = game.player2Id, priorityPlayerId = game.player2Id)
            game.castSpell(2, "Sacred Nectar").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 24
            game.isInGraveyard(1, "Deathless Knight") shouldBe true
        }
    }
}
