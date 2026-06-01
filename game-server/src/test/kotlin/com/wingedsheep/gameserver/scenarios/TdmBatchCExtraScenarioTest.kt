package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the "TDM batch C" cards:
 *  - Jeskai Shrinekeeper (#197) — combat-damage gain-life + draw
 *  - Sunpearl Kirin (#29) — ETB bounce of an own nonland permanent; token → draw
 *  - Zurgo's Vanguard (#133) — characteristic-defining power = creatures you control
 */
class TdmBatchCExtraScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Jeskai Shrinekeeper") {

            test("combat damage to a player gains 1 life and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jeskai Shrinekeeper", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Jeskai Shrinekeeper" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.step == Step.COMBAT_DAMAGE && !game.hasPendingDecision() && iterations++ < 20) {
                    game.passPriority()
                }
                game.resolveStack()

                withClue("Jeskai Shrinekeeper (3/3) deals 3 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Controller gains 1 life from the trigger") {
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("Controller draws a card from the trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }

        context("Sunpearl Kirin") {

            test("returns a chosen nontoken permanent to hand and draws no card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunpearl Kirin")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Sunpearl Kirin")
                withClue("Casting Sunpearl Kirin should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // Kirin enters → ETB trigger on stack, asks for a target.

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("The chosen Grizzly Bears returns to its owner's hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Grizzly Bears left the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                // Cast Kirin (-1 hand), bounced Bears (+1 hand) → net unchanged; no extra draw (nontoken).
                withClue("Returning a nontoken permanent draws no card") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("The library card was not drawn") {
                    game.findCardsInLibrary(1, "Mountain").size shouldBe 1
                }
            }

            test("returning a token draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunpearl Kirin")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false, isToken = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Sunpearl Kirin")
                withClue("Casting Sunpearl Kirin should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val tokenBears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(tokenBears))
                game.resolveStack()

                withClue("The token ceases to exist when it leaves the battlefield (not in hand)") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("The token left the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                // Cast Kirin (-1 hand), token doesn't go to hand, but the token clause draws (+1).
                withClue("Returning a token draws a card, so hand size is unchanged after casting Kirin") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("The library card was drawn by the token clause") {
                    game.findCardsInLibrary(1, "Mountain").size shouldBe 0
                }
            }
        }

        context("Zurgo's Vanguard") {

            test("power equals the number of creatures you control; toughness stays 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Zurgo's Vanguard")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Zurgo's Vanguard")!!
                val projected = stateProjector.project(game.state)
                withClue("Power = 3 creatures you control (Vanguard + Bears + Giant); opponent's creature excluded") {
                    projected.getPower(vanguard) shouldBe 3
                }
                withClue("Toughness is the fixed printed 3") {
                    projected.getToughness(vanguard) shouldBe 3
                }
            }
        }
    }
}
