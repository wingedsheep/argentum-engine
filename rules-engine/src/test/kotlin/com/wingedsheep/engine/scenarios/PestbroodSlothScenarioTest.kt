package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pestbrood Sloth (Secrets of Strixhaven #157).
 *
 * Pestbrood Sloth ({3}{G}, 4/4, Plant Sloth):
 *   Reach
 *   When this creature dies, create two 1/1 black and green Pest creature tokens with
 *   "Whenever this token attacks, you gain 1 life."
 *
 * Exercises the dies trigger creating two Pest tokens, and the tokens' own attack life-gain.
 * The Sloth (green, nonblack) is killed with Doom Blade (a test removal card).
 */
class PestbroodSlothScenarioTest : ScenarioTestBase() {

    private fun life(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<LifeTotalComponent>()?.life ?: 0

    init {
        context("Pestbrood Sloth — dies makes two Pests") {

            test("dying creates two Pest tokens that each gain life on attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Pestbrood Sloth")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.state.activePlayerId!!
                val startLife = life(game, player1)

                val sloth = game.findPermanent("Pestbrood Sloth")!!
                val cast = game.castSpell(1, "Doom Blade", targetId = sloth)
                withClue("Doom Blade should target the Sloth: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Sloth is destroyed") {
                    game.findPermanent("Pestbrood Sloth") shouldBe null
                }
                val pests = game.findPermanents("Pest Token")
                withClue("Two Pest tokens should have been created, got ${pests.size}") {
                    pests.size shouldBe 2
                }

                // Attack with one Pest; its attack trigger gains 1 life.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Pest Token" to 2))
                game.resolveStack()

                withClue("Attacking with a Pest gains its controller 1 life") {
                    life(game, player1) shouldBe startLife + 1
                }
            }
        }
    }
}
