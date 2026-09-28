package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class DeceptionScenarioTest : ScenarioTestBase() {
    init {
        context("Deception") {
            test("target opponent discards two cards") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Deception")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(1, "Deception", 2).error shouldBe null
                game.resolveStack()
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 5) {
                    val d = game.getPendingDecision() as com.wingedsheep.engine.core.SelectCardsDecision
                    game.selectCards(d.options.take(d.minSelections))
                }
                withClue("two discarded") {
                    game.handSize(2) shouldBe 1
                    game.graveyardSize(2) shouldBe 2
                }
            }
        }
    }
}
