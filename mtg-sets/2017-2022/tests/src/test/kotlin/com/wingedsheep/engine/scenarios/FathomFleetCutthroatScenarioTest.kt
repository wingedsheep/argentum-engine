package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fathom Fleet Cutthroat (XLN #107) — {3}{B} 3/3 Creature — Human Pirate.
 *
 *   When this creature enters, destroy target creature an opponent controls that was dealt damage this turn.
 */
class FathomFleetCutthroatScenarioTest : ScenarioTestBase() {

    init {
        context("Fathom Fleet Cutthroat") {
            test("can only target an opponent's creature that was dealt damage this turn, and destroys it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Fathom Fleet Cutthroat")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Shock", giant).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe true

                game.castSpell(1, "Fathom Fleet Cutthroat").error shouldBe null
                game.resolveStack()
                val decision = game.state.pendingDecision
                if (decision is ChooseTargetsDecision) {
                    decision.legalTargets.values.flatten().toSet() shouldBe setOf(giant)
                    game.selectTargets(listOf(giant)).error shouldBe null
                }
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
