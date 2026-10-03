package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Eternity Snare. */
class EternitySnareScenarioTest : ScenarioTestBase() {

    init {
        context("Eternity Snare — draw on entry, lock the enchanted creature") {
            test("entering draws a card and the enchanted creature doesn't untap") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eternity Snare")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Eternity Snare", bears).error shouldBe null
                game.resolveStack()

                withClue("the Aura is on the battlefield") {
                    game.isOnBattlefield("Eternity Snare") shouldBe true
                }
                withClue("the Snare left the hand and the ETB trigger drew a card back") {
                    game.handSize(1) shouldBe handBefore
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the enchanted creature carries DOESNT_UNTAP") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }
            }
        }
    }
}
