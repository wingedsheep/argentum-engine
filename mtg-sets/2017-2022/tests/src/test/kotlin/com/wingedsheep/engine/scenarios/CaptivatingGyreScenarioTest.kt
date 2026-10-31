package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Captivating Gyre (M20 #51) — {4}{U}{U} Sorcery.
 *
 *   Return up to three target creatures to their owners' hands.
 *
 * Each chosen target goes to its own owner's hand; an untargeted creature stays put.
 */
class CaptivatingGyreScenarioTest : ScenarioTestBase() {

    init {
        context("Captivating Gyre") {
            test("returns three targeted creatures to their owners' hands, leaving the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Captivating Gyre")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.state.getBattlefield(game.player1Id)
                    .single { game.findAllPermanents("Grizzly Bears").contains(it) }
                val giants = game.findAllPermanents("Hill Giant")
                val gyre = game.findCardsInHand(1, "Captivating Gyre").single()

                val targets = listOf(myBears, giants[0], giants[1]).map { ChosenTarget.Permanent(it) }
                val result = game.execute(CastSpell(game.player1Id, gyre, targets))
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("each target returns to its owner's hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInHand(2, "Hill Giant").size shouldBe 2
                }
                withClue("the untargeted creature stays on the battlefield") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 1
                    game.findAllPermanents("Hill Giant").size shouldBe 0
                }
            }
        }
    }
}
