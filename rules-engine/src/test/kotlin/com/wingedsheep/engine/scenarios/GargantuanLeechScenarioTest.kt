package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gargantuan Leech's self-cost-reduction and lifelink.
 *
 * Oracle: "This spell costs {1} less to cast for each Cave you control and each Cave card in
 * your graveyard.\nLifelink"
 *
 * Base cost is {7}{B} (7 generic + 1 black). The generic portion is reduced by:
 *  - the number of Cave permanents controlled (projected state via PermanentsYouControlMatching)
 *  - the number of Cave cards in the graveyard (base state via CardsInGraveyardMatchingFilter)
 *
 * Two separate ModifySpellCost static abilities accumulate additively in CostCalculator.
 * Cost floor: generic cannot drop below 0 (the {B} pip is never reduced).
 *
 * Lifelink: controller gains life equal to damage dealt by this creature.
 */
class GargantuanLeechScenarioTest : ScenarioTestBase() {

    init {
        context("Gargantuan Leech — cost reduction") {

            test("no Caves anywhere → full {7}{B} cost (7 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("with no Caves the generic component stays at 7") {
                    cost.genericAmount shouldBe 7
                }
            }

            test("two Caves controlled, none in graveyard → generic reduced by 2 (5 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                    .withCardOnBattlefield(1, "Captivating Cave")
                    .withCardOnBattlefield(1, "Promising Vein")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("two controlled Caves reduce generic from 7 to 5") {
                    cost.genericAmount shouldBe 5
                }
            }

            test("two Cave cards in graveyard, none on battlefield → generic reduced by 2 (5 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                    .withCardInGraveyard(1, "Captivating Cave")
                    .withCardInGraveyard(1, "Promising Vein")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("two Cave cards in graveyard reduce generic from 7 to 5") {
                    cost.genericAmount shouldBe 5
                }
            }

            test("two Caves controlled + one Cave in graveyard → generic reduced by 3 (4 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                    .withCardOnBattlefield(1, "Captivating Cave")
                    .withCardOnBattlefield(1, "Promising Vein")
                    .withCardInGraveyard(1, "Hidden Courtyard")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("two controlled + one graveyard Cave reduce generic from 7 to 4") {
                    cost.genericAmount shouldBe 4
                }
            }

            test("opponent's Caves do not reduce cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                    .withCardOnBattlefield(2, "Captivating Cave")   // opponent's Cave — must NOT count
                    .withCardOnBattlefield(2, "Promising Vein")     // opponent's Cave — must NOT count
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("opponent's Caves provide no discount") {
                    cost.genericAmount shouldBe 7
                }
            }

            test("reduction is capped — cannot reduce below 0 generic") {
                // Put 4 Caves on the battlefield and 4 Cave cards in the graveyard (8 total,
                // more than the 7 generic pips).
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gargantuan Leech")
                repeat(4) {
                    builder.withCardOnBattlefield(1, "Captivating Cave")
                    builder.withCardInGraveyard(1, "Promising Vein")
                }
                val game = builder.build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Gargantuan Leech"),
                    game.player1Id,
                )

                withClue("generic cost floors at 0 even when discount overshoots") {
                    cost.genericAmount shouldBe 0
                }
            }
        }

        context("Gargantuan Leech — lifelink") {

            test("deals 5 combat damage to opponent and controller gains 5 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gargantuan Leech")   // not summoning sick — can attack
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to declare-attackers and swing with the Leech at Player2.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Gargantuan Leech" to 2)).error shouldBe null

                // Resolve combat (no blockers): damage is dealt in the combat-damage step.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("opponent takes 5 damage from Gargantuan Leech") {
                    game.getLifeTotal(2) shouldBe 15
                }
                withClue("controller gains 5 life from lifelink") {
                    game.getLifeTotal(1) shouldBe 25
                }
            }
        }
    }
}
