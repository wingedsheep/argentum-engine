package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Magebane Lizard (OTJ #134) — {1}{R} Lizard, 1/4.
 *
 *   "Whenever a player casts a noncreature spell, this creature deals damage to that player equal
 *    to the number of noncreature spells they've cast this turn."
 *
 * Verifies the trigger fires when the active player casts a noncreature spell and the damage
 * scales with the running noncreature-spell count for that turn (1 for the first, 2 for the
 * second), and that the controller of the Lizard who casts a creature spell takes no damage.
 */
class MagebaneLizardScenarioTest : ScenarioTestBase() {

    init {
        context("Magebane Lizard") {

            test("first noncreature spell deals 1, second deals 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Magebane Lizard") // opponent controls the Lizard
                    .withCardsInHand(1, "Lightning Bolt", 2)     // two noncreature spells
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .withLifeTotal(1, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First noncreature spell — Lizard's trigger deals 1 (the only noncreature
                // spell cast this turn). Target the opponent with the Bolt itself.
                val firstBolt = game.findCardsInHand(1, "Lightning Bolt").first()
                val cast1 = game.execute(
                    com.wingedsheep.engine.core.CastSpell(
                        playerId = game.player1Id,
                        cardId = firstBolt,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)
                        ),
                    )
                )
                withClue("Casting the first Lightning Bolt should succeed: ${cast1.error}") {
                    cast1.error shouldBe null
                }
                game.resolveStack() // Lizard trigger + Bolt resolve

                withClue("Caster takes 1 from the Lizard (1st noncreature spell this turn)") {
                    game.getLifeTotal(1) shouldBe 19
                }

                // Second noncreature spell this turn — Lizard now deals 2.
                val secondBolt = game.findCardsInHand(1, "Lightning Bolt").first()
                val cast2 = game.execute(
                    com.wingedsheep.engine.core.CastSpell(
                        playerId = game.player1Id,
                        cardId = secondBolt,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)
                        ),
                    )
                )
                withClue("Casting the second Lightning Bolt should succeed: ${cast2.error}") {
                    cast2.error shouldBe null
                }
                game.resolveStack()

                withClue("Caster takes 2 more from the Lizard (2nd noncreature spell this turn)") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }
        }
    }
}
