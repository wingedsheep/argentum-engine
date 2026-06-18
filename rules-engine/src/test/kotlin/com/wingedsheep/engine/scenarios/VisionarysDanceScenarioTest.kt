package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Visionary's Dance (Secrets of Strixhaven #242) — {5}{U}{R} Sorcery.
 *
 * "Create two 3/3 blue and red Elemental creature tokens with flying.
 *  {2}, Discard this card: Look at the top two cards of your library. Put one of them into your
 *  hand and the other into your graveyard."
 *
 * Exercises the main spell (two flying Elemental tokens) and the hand-activated dig ability
 * (pay {2}, discard the card; keep one of the top two in hand, the other to the graveyard).
 */
class VisionarysDanceScenarioTest : ScenarioTestBase() {

    init {
        context("Visionary's Dance") {

            test("the spell creates two 3/3 flying Elemental tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Visionary's Dance")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Visionary's Dance").error shouldBe null
                game.resolveStack()

                withClue("Two Elemental tokens should have been created") {
                    game.findPermanents("Elemental Token").size shouldBe 2
                }
            }

            test("the hand ability digs two, keeping one in hand and milling the other") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Visionary's Dance")
                    .withCardInLibrary(1, "Mountain")  // top card
                    .withCardInLibrary(1, "Island")    // second card
                    .withCardInLibrary(1, "Forest")    // remains in library
                    .withLandsOnBattlefield(1, "Island", 2) // pays {2}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val danceId = game.state.getZone(game.player1Id, Zone.HAND)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Visionary's Dance" }
                val abilityId = cardRegistry.getCard("Visionary's Dance")!!.script.activatedAbilities[0].id

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = danceId,
                        abilityId = abilityId,
                        targets = emptyList()
                    )
                )
                withClue("Activating the hand ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }

                // Resolution offers a selection: choose which of the top two to keep. Keep the
                // top card (Mountain) in hand; the other (Island) goes to the graveyard.
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val mountain = game.state.getLibrary(game.player1Id).first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Mountain"
                    }
                    game.selectCards(listOf(mountain))
                    game.resolveStack()
                }

                withClue("Visionary's Dance is discarded to the graveyard (its activation cost)") {
                    game.isInGraveyard(1, "Visionary's Dance") shouldBe true
                }
                withClue("Exactly one of the looked-at cards ends up in the graveyard with the spell") {
                    // Spell + the un-kept card = 2 cards in the graveyard.
                    game.graveyardSize(1) shouldBe 2
                }
                withClue("Exactly one looked-at card is kept in hand") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
