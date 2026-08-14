package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Thrór's Map (HOB #—) — {2} Legendary Artifact.
 *
 * "When Thrór's Map enters, search your library for a basic land card, reveal it, put it into
 *  your hand, then shuffle.
 *  {2}, {T}: Draw a card, then discard a card."
 *
 * Covers the ETB search (including that the search filter really is *basic* land), and the
 * activated loot ability's net card flow plus its tap cost.
 */
class ThrorsMapScenarioTest : ScenarioTestBase() {

    init {
        context("Thrór's Map") {

            test("ETB searches the library for a basic land and puts it in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thrór's Map")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    // A nonbasic land and a creature that the "basic land card" filter must exclude.
                    .withCardInLibrary(1, "Hobbit Hole")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thrór's Map").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the ETB trigger asks which card to fetch") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val options = (decision as SelectCardsDecision).options
                withClue("only the basic land is a legal choice — not the nonbasic land or the creature") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Forest")
                }

                game.selectCards(listOf(options.single())).error shouldBe null
                game.resolveStack()

                withClue("the fetched Forest is in hand and out of the library") {
                    game.isInHand(1, "Forest") shouldBe true
                    game.findCardsInLibrary(1, "Forest").isEmpty() shouldBe true
                }
                withClue("Thrór's Map resolved onto the battlefield") {
                    game.isOnBattlefield("Thrór's Map") shouldBe true
                }
            }

            test("{2}, {T} draws a card then discards one, taps the Map, and nets zero cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thrór's Map")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val map = game.findPermanent("Thrór's Map")!!
                val loot = cardRegistry.requireCard("Thrór's Map").activatedAbilities.single().id
                val handBefore = game.handSize(1)
                val libraryBefore = game.librarySize(1)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = map, abilityId = loot)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val discard = game.getPendingDecision()
                withClue("resolution asks which card to discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf((discard as SelectCardsDecision).options.first())).error shouldBe null
                game.resolveStack()

                withClue("drew one and discarded one — hand size is unchanged") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("the draw came off the library") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
                withClue("one card was discarded to the graveyard") {
                    game.graveyardSize(1) shouldBe 1
                }
                withClue("the {T} cost tapped the Map") {
                    game.state.getEntity(map)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
