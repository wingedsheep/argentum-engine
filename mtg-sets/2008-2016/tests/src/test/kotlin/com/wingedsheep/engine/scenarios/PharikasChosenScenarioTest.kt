package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Pharika's Chosen (Journey into Nyx #78) — {B} Creature — Snake 1/1, Deathtouch.
 */
class PharikasChosenScenarioTest : ScenarioTestBase() {
    init {
        test("its 1 damage destroys the 6/4 that blocks it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pharika's Chosen")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Pharika's Chosen" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Craw Wurm" to listOf("Pharika's Chosen"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isInGraveyard(1, "Pharika's Chosen") shouldBe true
        }
    }
}
