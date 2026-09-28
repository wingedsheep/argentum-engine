package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ImperialEdictScenarioTest : ScenarioTestBase() {
    init {
        context("Imperial Edict") {
            test("the targeted opponent chooses which creature is destroyed") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Imperial Edict")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(1, "Imperial Edict", 2).error shouldBe null
                game.resolveStack()
                val d = game.getPendingDecision() as SelectCardsDecision
                withClue("the opponent decides") { d.playerId shouldBe game.player2Id }
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!))
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }
        }
    }
}
