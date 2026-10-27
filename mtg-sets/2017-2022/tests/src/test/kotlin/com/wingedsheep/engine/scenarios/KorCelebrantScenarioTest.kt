package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kor Celebrant (ZNR #22) — {2}{W} Creature — Kor Cleric, 1/4.
 *
 *   Whenever this creature or another creature you control enters, you gain 1 life.
 */
class KorCelebrantScenarioTest : ScenarioTestBase() {

    init {
        context("Kor Celebrant's enters trigger") {

            test("triggers on its own arrival and on another creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kor Celebrant")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kor Celebrant").error shouldBe null
                game.resolveStack()
                withClue("the Celebrant's own arrival gains 1 life") {
                    game.getLifeTotal(1) shouldBe 21
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("another creature you control entering gains 1 more") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("an opponent's creature entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kor Celebrant")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
