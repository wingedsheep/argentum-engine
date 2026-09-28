package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class WeiAssassinsScenarioTest : ScenarioTestBase() {
    init {
        context("Wei Assassins") {
            test("on entering, the opponent chooses one of their creatures and it is destroyed") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Wei Assassins")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Wei Assassins").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision() && game.getPendingDecision() !is SelectCardsDecision) {
                    game.selectTargets(listOf(game.player2Id))
                }
                val d = game.getPendingDecision() as SelectCardsDecision
                withClue("the opponent decides") { d.playerId shouldBe game.player2Id }
                game.selectCards(listOf(game.findPermanent("Hill Giant")!!))
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Wei Assassins") shouldBe true
            }
        }
    }
}
