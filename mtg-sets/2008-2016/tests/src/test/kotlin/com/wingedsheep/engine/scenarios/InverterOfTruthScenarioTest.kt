package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Inverter of Truth (OGW #72) — {2}{B}{B} Creature — Eldrazi, 6/6, devoid, flying.
 *
 *   When this creature enters, exile all cards from your library face down, then shuffle all
 *   cards from your graveyard into your library.
 *
 * Pins the ordering ("then": the old graveyard becomes the new library and is not itself exiled),
 * that the exiled library is face down and was never shown to anyone (ruling: no player can look
 * at the cards), and that only the controller's zones are touched.
 */
class InverterOfTruthScenarioTest : ScenarioTestBase() {

    init {
        context("Inverter of Truth") {

            test("exiles the library face down, then shuffles the graveyard in as the new library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inverter of Truth")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInGraveyard(2, "Shock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oldLibrary = game.state.getLibrary(game.player1Id).toList()
                val oldGraveyard = game.state.getGraveyard(game.player1Id).toList()

                game.castSpell(1, "Inverter of Truth").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Inverter of Truth") shouldBe true

                withClue("the old graveyard is the new library, and none of it was exiled") {
                    game.state.getLibrary(game.player1Id) shouldContainExactlyInAnyOrder oldGraveyard
                    game.graveyardSize(1) shouldBe 0
                }

                val exile = game.state.getExile(game.player1Id)
                withClue("every card of the old library is in exile") {
                    exile shouldContainExactlyInAnyOrder oldLibrary
                }
                for (card in oldLibrary) {
                    val entity = game.state.getEntity(card)!!
                    withClue("exiled card is face down") { entity.has<FaceDownComponent>() shouldBe true }
                    withClue("no player was shown the exiled card") {
                        (entity.get<RevealedToComponent>()?.playerIds ?: emptySet()) shouldBe emptySet()
                    }
                }

                withClue("the opponent's zones are untouched") {
                    game.librarySize(2) shouldBe 1
                    game.graveyardSize(2) shouldBe 1
                }
            }

            test("with an empty graveyard the library is simply exiled, leaving no library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inverter of Truth")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Inverter of Truth").error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe 0
                game.state.getExile(game.player1Id).size shouldBe 2
            }
        }
    }
}
