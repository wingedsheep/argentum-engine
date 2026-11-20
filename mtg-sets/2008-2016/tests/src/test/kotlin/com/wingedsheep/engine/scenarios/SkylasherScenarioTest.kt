package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skylasher (DGM #49) — {1}{G} 2/2 Creature — Insect.
 *
 *   Flash
 *   This spell can't be countered.
 *   Reach, protection from blue
 */
class SkylasherScenarioTest : ScenarioTestBase() {
    init {
        test("Cancel can't counter it, and it lands with flash, reach and protection from blue") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Skylasher")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(2, "Cancel")
                .withLandsOnBattlefield(2, "Island", 3)
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .withPriorityPlayer(1)
                .build()

            val cast = game.castSpell(1, "Skylasher")
            withClue("flash lets it be cast on the opponent's end step: ${cast.error}") { cast.error shouldBe null }
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Cancel", "Skylasher").error shouldBe null
            game.resolveStack()

            withClue("Skylasher resolved despite Cancel") { game.isOnBattlefield("Skylasher") shouldBe true }
            game.isInGraveyard(2, "Cancel") shouldBe true

            val skylasher = game.findPermanent("Skylasher")!!
            game.state.projectedState.hasKeyword(skylasher, Keyword.REACH) shouldBe true
        }
    }
}
