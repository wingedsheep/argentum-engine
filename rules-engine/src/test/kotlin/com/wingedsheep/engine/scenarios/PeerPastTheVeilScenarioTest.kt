package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Peer Past the Veil. */
class PeerPastTheVeilScenarioTest : ScenarioTestBase() {

    init {
        context("Peer Past the Veil — discard hand, draw X = card types in graveyard") {

            test("discards the whole hand then draws one card per distinct card type in the graveyard") {
                // Graveyard seeded with three distinct card types: creature, instant, land.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Peer Past the Veil")
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInGraveyard(1, "Grizzly Bears") // creature
                    .withCardInGraveyard(1, "Lightning Bolt") // instant
                    .withCardInGraveyard(1, "Forest") // land
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Graveyard starts with 3 cards (creature, instant, land)") {
                    game.state.getGraveyard(game.player1Id).size shouldBe 3
                }

                game.castSpell(1, "Peer Past the Veil").error shouldBe null
                game.resolveStack()

                // After resolution the 2 Grizzly Bears are discarded (creature already present) and
                // Peer Past the Veil itself reaches the graveyard as an instant (already present). So the
                // card types among graveyard cards = {creature, instant, land} = 3, and 3 cards are drawn.
                withClue("Hand was discarded then refilled with 3 drawn cards (X = 3 card types)") {
                    game.state.getHand(game.player1Id).size shouldBe 3
                }
                withClue("Discarded Grizzly Bears are now in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
