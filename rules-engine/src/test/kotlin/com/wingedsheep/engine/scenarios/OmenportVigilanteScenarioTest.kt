package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Omenport Vigilante (OTJ #21) — {1}{W} Creature — Human Mercenary 2/2.
 *
 *   "This creature has double strike as long as you've committed a crime this turn."
 *
 * The crime-conditional keyword is a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility]
 * granting double strike to itself gated on `YouCommittedCrimeThisTurn`. Verifies the keyword is
 * absent before a crime and present once the controller has committed one this turn.
 */
class OmenportVigilanteScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Omenport Vigilante") {

            test("has no double strike until you've committed a crime this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Omenport Vigilante")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vigilante = game.findPermanent("Omenport Vigilante")!!

                // No crime yet -> no double strike.
                var projected = stateProjector.project(game.state)
                projected.hasKeyword(vigilante, Keyword.DOUBLE_STRIKE) shouldBe false

                // Mark that the controller committed a crime this turn.
                game.state = game.state.copy(
                    playersWhoCommittedCrimeThisTurn = setOf(game.player1Id)
                )

                projected = stateProjector.project(game.state)
                projected.hasKeyword(vigilante, Keyword.DOUBLE_STRIKE) shouldBe true
            }

            test("an opponent's crime does not grant you double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Omenport Vigilante")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vigilante = game.findPermanent("Omenport Vigilante")!!

                // Only the opponent committed a crime.
                game.state = game.state.copy(
                    playersWhoCommittedCrimeThisTurn = setOf(game.player2Id)
                )

                val projected = stateProjector.project(game.state)
                projected.hasKeyword(vigilante, Keyword.DOUBLE_STRIKE) shouldBe false
            }
        }
    }
}
