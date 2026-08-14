package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hobbit Hole (HOB #184) — Land.
 *
 * "{T}, Sacrifice this land: Search your library for a basic land card, put it onto the
 *  battlefield tapped, then shuffle.
 *  Halflingcycling {4}"
 *
 * The activated ability has to consume the land and produce a *tapped* basic; the typecycling
 * ability has to discard the card and fetch a Halfling — and only a Halfling — to hand.
 */
class HobbitHoleScenarioTest : ScenarioTestBase() {

    init {
        context("Hobbit Hole") {

            test("{T}, Sacrifice fetches a basic land onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hobbit Hole")
                    .withCardInLibrary(1, "Forest")
                    // Neither of these is a basic land card.
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hole = game.findPermanent("Hobbit Hole")!!
                val search = cardRegistry.requireCard("Hobbit Hole").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hole, abilityId = search)
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the ability raises a library search") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val options = (decision as SelectCardsDecision).options
                withClue("the search offers only basic land cards") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Forest")
                }
                game.selectCards(listOf(options.single())).error shouldBe null
                game.resolveStack()

                withClue("the sacrifice cost consumed the Hobbit Hole") {
                    game.findPermanent("Hobbit Hole") shouldBe null
                    game.isInGraveyard(1, "Hobbit Hole") shouldBe true
                }
                withClue("the fetched basic is on the battlefield") {
                    game.isOnBattlefield("Forest") shouldBe true
                }
                val forest = game.findPermanent("Forest")!!
                withClue("and it entered tapped") {
                    game.state.getEntity(forest)?.has<TappedComponent>() shouldBe true
                }
                withClue("it is no longer in the library") {
                    game.findCardsInLibrary(1, "Forest").isEmpty() shouldBe true
                }
            }

            test("Halflingcycling {4} discards it and fetches a Halfling to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hobbit Hole")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    // Nimble Hobbit is a Halfling; the others must not be offered.
                    .withCardInLibrary(1, "Nimble Hobbit")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.typecycleCard(1, "Hobbit Hole").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

                withClue("typecycling discards the card as a cost") {
                    game.isInGraveyard(1, "Hobbit Hole") shouldBe true
                    game.isInHand(1, "Hobbit Hole") shouldBe false
                }

                val decision = game.getPendingDecision()
                withClue("typecycling raises a library search") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val options = (decision as SelectCardsDecision).options
                withClue("only Halfling cards are offered") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Nimble Hobbit")
                }
                game.selectCards(listOf(options.single())).error shouldBe null

                withClue("the Halfling went to hand, not the battlefield") {
                    game.isInHand(1, "Nimble Hobbit") shouldBe true
                    game.isOnBattlefield("Nimble Hobbit") shouldBe false
                }
            }

            test("Halflingcycling is unaffordable on three lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hobbit Hole")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Nimble Hobbit")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Halflingcycling {4} needs four mana") {
                    game.typecycleCard(1, "Hobbit Hole").error shouldNotBe null
                }
                withClue("the card stayed in hand") {
                    game.isInHand(1, "Hobbit Hole") shouldBe true
                }
            }
        }
    }
}
