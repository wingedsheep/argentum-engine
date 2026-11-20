package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mindstatic (DGM #14) — {3}{U} Instant.
 *
 *   Counter target spell unless its controller pays {6}.
 */
class MindstaticScenarioTest : ScenarioTestBase() {
    init {
        test("counters a spell whose controller can't pay {6}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mindstatic")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2) // exactly {1}{G}, nothing left to pay
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.passPriority()

            game.castSpellTargetingStackSpell(1, "Mindstatic", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is countered") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Mindstatic") shouldBe true
            }
        }
    }
}
