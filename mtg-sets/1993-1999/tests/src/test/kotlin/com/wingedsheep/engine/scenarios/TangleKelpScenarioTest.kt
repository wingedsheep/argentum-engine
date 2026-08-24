package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tangle Kelp.
 *
 * The interesting half is *whose* attack the untap clause reads. The source is the Aura, and an Aura
 * never attacks — so a source-scoped condition would always read false and the Kelp would do nothing
 * after its ETB tap. The condition is aimed at the enchanted permanent instead, and this test proves
 * it by attacking with the host and checking it is still tapped a turn later.
 */
class TangleKelpScenarioTest : ScenarioTestBase() {

    init {
        context("Tangle Kelp") {

            test("taps the creature on arrival") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tangle Kelp")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("it starts untapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }

                game.castSpell(1, "Tangle Kelp", bears).error shouldBe null
                game.resolveStack()

                withClue("the enters trigger taps the creature it enchanted") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("holds down a creature that attacked during its controller's last turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Tangle Kelp", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                withClue("attacking taps it") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }

                // Round to its controller's next untap step, asserting each hop.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("it attacked during its controller's last turn, so the Kelp held it down") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("a creature that did not attack untaps normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false, tapped = true)
                    .withCardAttachedTo(1, "Tangle Kelp", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("no attack last turn, so the Kelp's clause doesn't hold") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
