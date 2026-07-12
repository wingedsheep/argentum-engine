package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Syphon Essence (VOW #84) — {2}{U} Instant.
 *
 *   Counter target creature or planeswalker spell. Create a Blood token.
 *
 * Exercises Effects.CounterSpell() then Effects.CreateBlood(1): the targeted creature spell is
 * countered (goes to its owner's graveyard) and the caster gets a Blood token.
 */
class SyphonEssenceScenarioTest : ScenarioTestBase() {

    init {
        context("Syphon Essence — counter a creature spell and make a Blood token") {

            test("counters the targeted creature spell and creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Syphon Essence")
                    .withLandsOnBattlefield(1, "Island", 3)
                    // Player 2 (active player) casts a creature spell to be countered.
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Syphon Essence", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is countered (in player 2's graveyard, not on the battlefield)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("A Blood token is created for the caster") {
                    game.findPermanent("Blood").shouldNotBeNull()
                }
            }
        }
    }
}
