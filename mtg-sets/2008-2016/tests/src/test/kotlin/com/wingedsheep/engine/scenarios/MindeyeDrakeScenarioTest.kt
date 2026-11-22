package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Mindeye Drake (GTC) — flying; when it dies, target player mills five cards. */
class MindeyeDrakeScenarioTest : ScenarioTestBase() {
    init {
        test("dying makes the targeted player mill five") {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mindeye Drake")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
            repeat(7) { builder.withCardInLibrary(2, "Forest") }
            val game = builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Murder", game.findPermanent("Mindeye Drake")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Mindeye Drake") shouldBe true
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.findCardsInGraveyard(2, "Forest").size shouldBe 5
        }
    }
}
