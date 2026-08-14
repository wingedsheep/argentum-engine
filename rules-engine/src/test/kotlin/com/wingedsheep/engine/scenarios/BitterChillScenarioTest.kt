package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Bitter Chill. */
class BitterChillScenarioTest : ScenarioTestBase() {

    init {
        context("Bitter Chill — tap, lock, and a {1} refund") {
            test("entering taps the enchanted creature and keeps it from untapping") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bitter Chill")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Bitter Chill", bears)
                game.resolveStack()

                withClue("the ETB trigger tapped the enchanted creature") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
                withClue("the enchanted creature carries DOESNT_UNTAP") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }
            }

            test("destroying the enchanted creature offers the {1} scry-and-draw refund") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Bitter Chill", "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Doom Blade", bears)
                game.resolveStack()

                withClue("the Aura followed its creature into the graveyard") {
                    game.isInGraveyard(1, "Bitter Chill") shouldBe true
                }

                // "You may pay {1}. If you do, scry 1, then draw a card."
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.skipSelection()    // scry 1: nothing to the bottom
                game.keepLibraryOrder() // …and leave the top card where it is
                game.resolveStack()

                withClue("Doom Blade left the hand and the refund drew a card back — net zero") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("the scried-then-drawn card is the top of the library") {
                    game.isInHand(1, "Forest") shouldBe true
                }
            }
        }
    }
}
