package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skull Raid — target opponent discards two cards; if fewer than two were discarded this way,
 * you draw cards equal to the difference.
 */
class SkullRaidScenarioTest : ScenarioTestBase() {

    private fun cast(opponentHand: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Skull Raid")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponentHand.forEach { builder.withCardInHand(2, it) }
        val game = builder.build()
        game.castSpellTargetingPlayer(1, "Skull Raid", 2).error shouldBe null
        game.resolveStack()
        var guard = 0
        while (game.hasPendingDecision() && guard++ < 5) {
            val d = game.getPendingDecision() as SelectCardsDecision
            game.selectCards(d.options.take(d.minSelections))
        }
        return game
    }

    init {
        context("Skull Raid") {
            test("opponent with three cards discards two and you draw nothing") {
                val game = cast(listOf("Hill Giant", "Ornithopter", "Grizzly Bears"))
                withClue("opponent discarded two") {
                    game.handSize(2) shouldBe 1
                    game.graveyardSize(2) shouldBe 2
                }
                withClue("no difference to draw") { game.handSize(1) shouldBe 0 }
            }

            test("opponent with one card discards it and you draw one") {
                val game = cast(listOf("Hill Giant"))
                game.handSize(2) shouldBe 0
                game.graveyardSize(2) shouldBe 1
                withClue("drew the difference of one") { game.handSize(1) shouldBe 1 }
            }

            test("opponent with an empty hand discards nothing and you draw two") {
                val game = cast(emptyList())
                game.graveyardSize(2) shouldBe 0
                withClue("drew the difference of two") { game.handSize(1) shouldBe 2 }
            }
        }
    }
}
