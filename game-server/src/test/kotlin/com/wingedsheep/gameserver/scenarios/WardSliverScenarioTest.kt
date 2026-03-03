package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ward Sliver.
 *
 * Card reference:
 * - Ward Sliver ({4}{W}): Creature — Sliver 2/2
 *   "As this creature enters, choose a color.
 *    All Slivers have protection from the chosen color."
 */
class WardSliverScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseColor(color: Color) {
        val decision = getPendingDecision()!!
        submitDecision(ColorChosenResponse(decision.id, color))
    }

    init {
        context("Ward Sliver - choose color, all Slivers gain protection") {

            test("all Slivers have protection from the chosen color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ward Sliver")
                    .withCardOnBattlefield(1, "Plated Sliver") // Sliver 1/1
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Ward Sliver")
                withClue("Ward Sliver should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Should have pending color decision") {
                    game.hasPendingDecision() shouldBe true
                }

                game.chooseColor(Color.RED)

                withClue("Ward Sliver should be on battlefield") {
                    game.isOnBattlefield("Ward Sliver") shouldBe true
                }

                val projected = stateProjector.project(game.state)

                val wardSliver = game.findPermanent("Ward Sliver")!!
                withClue("Ward Sliver itself should have protection from red") {
                    projected.hasKeyword(wardSliver, "PROTECTION_FROM_RED") shouldBe true
                }

                val platedSliver = game.findPermanent("Plated Sliver")!!
                withClue("Plated Sliver should have protection from red") {
                    projected.hasKeyword(platedSliver, "PROTECTION_FROM_RED") shouldBe true
                }
            }

            test("non-Sliver creatures do not gain protection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ward Sliver")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier 2/2
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ward Sliver")
                game.resolveStack()
                game.chooseColor(Color.BLACK)

                val glorySeekerEntity = game.findPermanent("Glory Seeker")!!
                val projected = stateProjector.project(game.state)

                withClue("Glory Seeker (non-Sliver) should not have protection from black") {
                    projected.hasKeyword(glorySeekerEntity, "PROTECTION_FROM_BLACK") shouldBe false
                }
            }

            test("opponent's Slivers also gain protection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ward Sliver")
                    .withCardOnBattlefield(2, "Hunter Sliver") // Sliver 1/1
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ward Sliver")
                game.resolveStack()
                game.chooseColor(Color.GREEN)

                val hunterSliver = game.findPermanent("Hunter Sliver")!!
                val projected = stateProjector.project(game.state)

                withClue("Opponent's Hunter Sliver should have protection from green") {
                    projected.hasKeyword(hunterSliver, "PROTECTION_FROM_GREEN") shouldBe true
                }
            }
        }
    }
}
