package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silver Raven (AFR #74) — {U} Artifact Creature — Bird 1/1, flying.
 *
 *   When this creature enters, scry 1.
 */
class SilverRavenScenarioTest : ScenarioTestBase() {

    init {
        test("entering scries 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Silver Raven")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Silver Raven").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 1
            game.skipSelection()
            game.resolveStack()

            val raven = game.findPermanent("Silver Raven")!!
            game.state.projectedState.hasKeyword(raven, Keyword.FLYING) shouldBe true
            game.librarySize(1) shouldBe 2
        }
    }
}
