package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Disturbing Conversion (MOM #54) — {1}{U} Enchantment — Aura.
 *
 *   Flash
 *   Enchant creature
 *   When this Aura enters, each player mills two cards.
 *   Enchanted creature gets -X/-0, where X is the number of cards in its controller's graveyard.
 *
 * The count follows the *enchanted creature's* controller (Player.ControllerOfAffectedEntity), not
 * the Aura's controller.
 */
class DisturbingConversionScenarioTest : ScenarioTestBase() {

    init {
        context("Disturbing Conversion") {

            test("entering makes each player mill two cards") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Disturbing Conversion")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                repeat(3) {
                    builder.withCardInLibrary(1, "Swamp")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Disturbing Conversion", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("both players milled two") {
                    game.graveyardSize(1) shouldBe 2
                    game.graveyardSize(2) shouldBe 2
                }
                withClue("opponent's Bears get -2/-0 from the opponent's two-card graveyard") {
                    game.state.projectedState.getPower(bears) shouldBe 0
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }

            test("X counts the enchanted creature's controller's graveyard, not the Aura controller's") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Disturbing Conversion", "Hill Giant")
                repeat(5) { builder.withCardInGraveyard(1, "Swamp") }
                repeat(2) { builder.withCardInGraveyard(2, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant (3/3) under Player2 with two cards in Player2's graveyard is 1/3") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("X follows a control change of the enchanted creature") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Disturbing Conversion", "Hill Giant")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                repeat(5) { builder.withCardInGraveyard(1, "Swamp") }
                repeat(2) { builder.withCardInGraveyard(2, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getPower(giant) shouldBe 1

                game.castSpell(1, "Threaten", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("now Player1 controls it: 5 Swamps + Threaten = 6 cards, so 3 - 6 = -3") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                    game.state.projectedState.getPower(giant) shouldBe -3
                }
            }
        }
    }
}
