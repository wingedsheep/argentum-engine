package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wretched Anurid.
 *
 * Card reference:
 * - Wretched Anurid ({1}{B}): Creature — Zombie Frog Beast 3/3
 *   "Whenever another creature enters the battlefield, you lose 1 life."
 */
class WretchedAnuridScenarioTest : ScenarioTestBase() {

    init {
        context("Wretched Anurid triggers on creature ETB") {

            test("triggers when a normal creature enters the battlefield") {
                val game = scenario()
                    .withPlayers("Anurid Player", "Opponent")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(1, "Barkhide Mauler")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Barkhide Mauler")
                game.resolveStack()

                withClue("Should lose 1 life from Wretched Anurid trigger") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("triggers when a face-down morph creature enters the battlefield") {
                val game = scenario()
                    .withPlayers("Anurid Player", "Opponent")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(1, "Crude Rampart")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast morph creature face-down
                val hand = game.state.getHand(game.player1Id)
                val morphCardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Crude Rampart"
                }!!
                val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
                withClue("Cast morph should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Should lose 1 life from Wretched Anurid trigger (face-down creature is still a creature)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("triggers when opponent's creature enters the battlefield") {
                val game = scenario()
                    .withPlayers("Anurid Player", "Opponent")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(2, "Barkhide Mauler")
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Barkhide Mauler")
                game.resolveStack()

                withClue("Should lose 1 life even when opponent's creature enters") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
