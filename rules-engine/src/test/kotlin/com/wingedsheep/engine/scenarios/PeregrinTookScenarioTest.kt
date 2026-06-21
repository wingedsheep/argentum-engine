package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Peregrin Took (LTR #181, {2}{G}, 2/3 Legendary Halfling Citizen).
 *
 *   If one or more tokens would be created under your control, those tokens plus an additional
 *   Food token are created instead.  ([com.wingedsheep.sdk.scripting.CreateAdditionalToken])
 *   Sacrifice three Foods: Draw a card.
 */
class PeregrinTookScenarioTest : ScenarioTestBase() {

    init {
        context("Peregrin Took") {

            test("creating two tokens under your control yields the two tokens plus one Food") {
                // Rally at the Hornburg ({1}{R}, "Create two 1/1 white Human Soldier creature
                // tokens.") is the token source — it resolves without any decision/target, so the
                // creation event flows straight through Peregrin's replacement.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peregrin Took")
                    .withCardInHand(1, "Rally at the Hornburg")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Rally at the Hornburg")
                game.resolveStack()

                withClue("Rally at the Hornburg makes two Human Soldiers") {
                    game.findPermanents("Human Soldier Token").size shouldBe 2
                }
                withClue("Peregrin Took adds exactly one additional Food token (fires once, not per token)") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }

            test("a Food-making source yields exactly two Foods — the added Food does not loop (CR 614.5)") {
                // Brandywine Farmer ({2}{G}) creates one Food token when it enters. With Peregrin
                // Took out, that single-token creation event is replaced to add one more Food:
                // total exactly 2. If the added Food itself re-triggered the replacement, we'd see
                // 3+ (or a runaway loop). Its ETB trigger resolves with no decision/target.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peregrin Took")
                    .withCardInHand(1, "Brandywine Farmer")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Brandywine Farmer")
                game.resolveStack()

                withClue("One Food from Brandywine Farmer + one additional Food from Peregrin = 2, no runaway loop") {
                    game.findPermanents("Food").size shouldBe 2
                }
            }

            test("Sacrifice three Foods: Draw a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peregrin Took")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size
                game.findPermanents("Food").size shouldBe 3

                val took = game.findPermanent("Peregrin Took")!!
                val foods = game.findPermanents("Food")
                val drawAbility = cardRegistry.getCard("Peregrin Took")!!.script.activatedAbilities[0]
                // The "Sacrifice three Foods" cost requires the activator to choose which three
                // Foods to sacrifice — supply them via the cost payment.
                game.execute(
                    ActivateAbility(
                        game.player1Id,
                        took,
                        drawAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = foods)
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("All three Foods are sacrificed to pay the cost") {
                    game.findPermanents("Food").size shouldBe 0
                }
                withClue("One card is drawn") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore + 1
                }
            }
        }
    }
}
