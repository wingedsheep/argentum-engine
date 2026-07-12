package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Blood Petal Celebrant (VOW #146) — {1}{R} Creature — Vampire, 2/1.
 *
 *   This creature has first strike as long as it's attacking.
 *   When this creature dies, create a Blood token.
 *
 * Exercises the conditional first strike (only while attacking, not while sitting on the
 * battlefield) and the dies trigger creating a Blood token.
 */
class BloodPetalCelebrantScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Blood Petal Celebrant") {

            test("has no first strike while not attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blood Petal Celebrant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val celebrant = game.findPermanent("Blood Petal Celebrant")!!

                withClue("no first strike while not attacking") {
                    projector.getProjectedKeywords(game.state, celebrant).contains(Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("gains first strike while attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blood Petal Celebrant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val celebrant = game.findPermanent("Blood Petal Celebrant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Blood Petal Celebrant" to 2)).error shouldBe null

                withClue("first strike is granted while attacking") {
                    projector.getProjectedKeywords(game.state, celebrant).contains(Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("creates a Blood token when it dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blood Petal Celebrant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Blood Petal Celebrant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Hill Giant" to listOf("Blood Petal Celebrant")))
                withClue("blocking should succeed: ${block.error}") { block.error shouldBe null }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("Blood Petal Celebrant died to the 3/3 blocker") {
                    game.isOnBattlefield("Blood Petal Celebrant") shouldBe false
                    game.isInGraveyard(1, "Blood Petal Celebrant") shouldBe true
                }
                withClue("a Blood token was created on death") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
