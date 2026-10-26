package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Coat of Arms (EXO #131) — {5} Artifact.
 *
 * "Each creature gets +1/+1 for each other creature on the battlefield that shares at least one
 *  creature type with it."
 *
 * The amount is evaluated per affected creature: each Goblin counts the *other* Goblins on both
 * sides of the table, while a creature sharing no type with anything gets nothing.
 */
class CoatOfArmsScenarioTest : ScenarioTestBase() {

    init {
        test("each creature gets +1/+1 per other creature, on either side, sharing a creature type with it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Coat of Arms")
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Goblin Guide")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            withClue("each Goblin Guide (2/1) counts the two other Goblins → +2/+2") {
                game.findAllPermanents("Goblin Guide").forEach { goblin ->
                    projected.getPower(goblin) shouldBe 4
                    projected.getToughness(goblin) shouldBe 3
                }
            }
            withClue("creatures sharing no type with any other creature are unchanged") {
                val courser = game.findPermanent("Centaur Courser")!!
                projected.getPower(courser) shouldBe 3
                projected.getToughness(courser) shouldBe 3
                val lions = game.findPermanent("Savannah Lions")!!
                projected.getPower(lions) shouldBe 1
                projected.getToughness(lions) shouldBe 1
            }
        }
    }
}
