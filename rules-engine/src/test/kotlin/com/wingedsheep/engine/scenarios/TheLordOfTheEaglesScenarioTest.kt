package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Lord of the Eagles (HOB #46) — {7}{U}{U} 8/8 Legendary Creature.
 *
 * Oracle: "Flash / This spell costs {X} less to cast, where X is the total power of creatures you
 * control with flying. / Flying"
 *
 * These cover the new `CostReductionSource.TotalPropertyAmongPermanentsYouControl`, so the cases
 * that matter are the ones a naive implementation gets wrong: ground creatures must not count, the
 * opponent's fliers must not count, and a creature that only has flying from a continuous effect
 * must count (the filter has to read projected keywords, not the printed type line).
 */
class TheLordOfTheEaglesScenarioTest : ScenarioTestBase() {

    init {
        context("The Lord of the Eagles — costs {X} less for the total power of your fliers") {

            test("no creatures → full {7}{U}{U} (7 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Lord of the Eagles")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("The Lord of the Eagles"),
                    game.player1Id,
                )

                withClue("an empty board reduces nothing") {
                    cost.genericAmount shouldBe 7
                }
            }

            test("two fliers reduce by their total power, ground creatures don't count") {
                // Air Elemental 4/4 flying + Wind Drake 2/2 flying = 6, so {1}{U}{U}.
                // Hill Giant 3/3 has no flying and must be ignored.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Lord of the Eagles")
                    .withCardOnBattlefield(1, "Air Elemental")
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("The Lord of the Eagles"),
                    game.player1Id,
                )

                withClue("4 + 2 flying power reduces the generic from 7 to 1; the 3/3 ground creature is ignored") {
                    cost.genericAmount shouldBe 1
                }
            }

            test("the opponent's fliers do not count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Lord of the Eagles")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("The Lord of the Eagles"),
                    game.player1Id,
                )

                withClue("the source is 'you control', so eight power of opposing fliers reduces nothing") {
                    cost.genericAmount shouldBe 7
                }
            }

            test("a creature that only has flying from a continuous effect counts") {
                // Levitation gives creatures you control flying, so Grizzly Bears (2/2) and Hill
                // Giant (3/3) become fliers worth 5 total power.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Lord of the Eagles")
                    .withCardOnBattlefield(1, "Levitation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("The Lord of the Eagles"),
                    game.player1Id,
                )

                withClue("granted flying is projected state, so 2 + 3 power reduces the generic to 2") {
                    cost.genericAmount shouldBe 2
                }
            }

            test("the reduction never eats the colored pips") {
                // 4 + 4 + 2 = 10 flying power against 7 generic: it bottoms out at 0 and {U}{U} survives.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Lord of the Eagles")
                    .withCardOnBattlefield(1, "Air Elemental")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("The Lord of the Eagles"),
                    game.player1Id,
                )

                withClue("ten flying power clears all 7 generic but cannot reduce below {U}{U}") {
                    cost.genericAmount shouldBe 0
                    cost.colorCount[Color.BLUE] shouldBe 2
                }
            }
        }
    }
}
