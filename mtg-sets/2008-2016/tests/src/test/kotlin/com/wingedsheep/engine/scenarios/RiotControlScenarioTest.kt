package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Riot Control (DGM) — "You gain 1 life for each creature your opponents control. Prevent all
 * damage that would be dealt to you this turn."
 */
class RiotControlScenarioTest : ScenarioTestBase() {

    init {
        context("Riot Control") {
            test("gains 1 life per opposing creature only, then prevents damage to you this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Riot Control")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Bog Imp")
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Riot Control").error shouldBe null
                game.resolveStack()

                withClue("three opposing creatures; your own Grizzly Bears doesn't count") {
                    game.getLifeTotal(1) shouldBe 13
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("damage that would be dealt to you this turn is prevented") {
                    game.getLifeTotal(1) shouldBe 13
                }
            }

            test("with no opposing creatures you gain no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Riot Control")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Riot Control").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 10
            }
        }
    }
}
