package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Ashiok's Reaper. */
class AshioksReaperScenarioTest : ScenarioTestBase() {

    init {
        context("Ashiok's Reaper — your enchantments dying draw cards") {
            test("an enchantment you control hitting the graveyard draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ashiok's Reaper", summoningSickness = false)
                    .withCardOnBattlefield(1, "Castle")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Disenchant", game.findPermanent("Castle")!!).error shouldBe null
                game.resolveStack()

                withClue("Disenchant left hand (-1) and the Reaper's trigger drew a card (+1)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("an opponent's enchantment dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ashiok's Reaper", summoningSickness = false)
                    .withCardOnBattlefield(2, "Castle")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Disenchant", game.findPermanent("Castle")!!).error shouldBe null
                game.resolveStack()

                withClue("the enchantment was not yours, so no draw — only Disenchant left hand") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }
    }
}
