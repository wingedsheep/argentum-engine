package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vraan, Executioner Thane (ONE #114) — "Whenever one or more other creatures you control die,
 * each opponent loses 2 life and you gain 2 life. This ability triggers only once each turn."
 */
class VraanExecutionerThaneScenarioTest : ScenarioTestBase() {

    init {
        context("Vraan, Executioner Thane") {
            test("drains once for a creature dying, and only once per turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraan, Executioner Thane")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekers = game.findPermanents("Glory Seeker")
                val cast1 = game.castSpell(1, "Shock", seekers[0])
                withClue("First Shock should cast: ${cast1.error}") { cast1.error shouldBe null }
                game.resolveStack()

                withClue("Opponent loses 2, you gain 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }

                val cast2 = game.castSpell(1, "Shock", seekers[1])
                withClue("Second Shock should cast: ${cast2.error}") { cast2.error shouldBe null }
                game.resolveStack()

                withClue("Second death the same turn does not trigger again") {
                    game.findPermanents("Glory Seeker").size shouldBe 0
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("opponent's creatures dying do not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraan, Executioner Thane")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Glory Seeker")!!)
                game.resolveStack()

                withClue("No life change") {
                    game.findPermanents("Glory Seeker").size shouldBe 0
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("Vraan dying alone does not trigger its own ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraan, Executioner Thane")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Vraan, Executioner Thane")!!)
                game.resolveStack()

                withClue("No life change") {
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
