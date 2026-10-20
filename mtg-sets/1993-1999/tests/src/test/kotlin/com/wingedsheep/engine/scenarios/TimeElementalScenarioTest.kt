package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Time Elemental — "When this creature attacks or blocks, at end of combat,
 * sacrifice it and it deals 5 damage to you."
 *
 * The interesting part is ordering: the Elemental is sacrificed first and *then* deals the 5 damage,
 * so the damage has to resolve from a source already in the graveyard.
 */
class TimeElementalScenarioTest : ScenarioTestBase() {

    init {
        context("Time Elemental") {

            test("attacking: sacrificed at end of combat and deals 5 damage to its controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Time Elemental")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Time Elemental" to 2)).error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Time Elemental is sacrificed at end of combat") {
                    game.isInGraveyard(1, "Time Elemental") shouldBe true
                }
                withClue("it deals 5 damage to you even though it is already in the graveyard") {
                    game.getLifeTotal(1) shouldBe 15
                }
                game.getLifeTotal(2) shouldBe 20
            }

            test("blocking: sacrificed at end of combat and deals 5 damage to its controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Time Elemental")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Time Elemental" to listOf("Llanowar Elves"))).error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the 0/2 survives the Elves' 1 damage, then is sacrificed at end of combat") {
                    game.isInGraveyard(2, "Time Elemental") shouldBe true
                }
                withClue("the blocker's controller takes the 5 damage") {
                    game.getLifeTotal(2) shouldBe 15
                }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
