package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Reach for the Sky. */
class ReachForTheSkyScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Reach for the Sky") {
            test("grants +3/+2 and reach to the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Reach for the Sky")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Reach for the Sky", creature)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant should be 6/5 (3/3 +3/+2) and have reach") {
                    projector.getProjectedPower(game.state, creature) shouldBe 6
                    projector.getProjectedToughness(game.state, creature) shouldBe 5
                    projector.hasProjectedKeyword(game.state, creature, Keyword.REACH) shouldBe true
                }
            }

            test("draws a card when put into a graveyard from the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Reach for the Sky")
                    .withCardInHand(2, "Disenchant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Reach for the Sky", creature)
                game.resolveStack()
                val aura = game.findPermanent("Reach for the Sky")!!

                val handBefore = game.handSize(1)

                // Hand priority to the opponent so they may respond on player 1's turn.
                game.passPriority()

                // Opponent destroys the Aura → it goes to the graveyard → draw trigger fires.
                val disenchant = game.castSpell(2, "Disenchant", aura)
                withClue("Disenchant cast should succeed: ${disenchant.error}") {
                    disenchant.error shouldBe null
                }
                game.resolveStack()

                withClue("Reach for the Sky should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Reach for the Sky") shouldBe true
                }
                withClue("Controller should have drawn a card from the put-into-graveyard trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
