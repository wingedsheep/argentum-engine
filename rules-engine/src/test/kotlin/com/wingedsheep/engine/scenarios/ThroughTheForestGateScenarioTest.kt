package com.wingedsheep.engine.scenarios

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

/**
 * Through the Forest Gate (HOB) — {6}{G}{G} Sorcery.
 *
 * "Look at the top twenty cards of your library, put any number of land cards from among them
 *  onto the battlefield tapped, then shuffle. You gain 8 life."
 *
 * The points worth pinning: only land cards are selectable (nonlands are shown but not eligible),
 * "any number" allows zero, the lands arrive *tapped*, the cards left behind stay in the library
 * rather than going to the graveyard, and the 8 life is gained either way.
 */
class ThroughTheForestGateScenarioTest : ScenarioTestBase() {

    init {
        context("Through the Forest Gate") {

            test("puts any number of the looked-at lands onto the battlefield tapped and gains 8 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Through the Forest Gate")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)
                game.castSpell(1, "Through the Forest Gate").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the land pick is a card selection over the looked-at cards") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val options = (decision as SelectCardsDecision).options
                withClue("only land cards are selectable — the Centaur Courser is not") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Forest", "Mountain")
                }
                withClue("'any number' means no card has to be chosen") {
                    decision.minSelections shouldBe 0
                }

                game.selectCards(options).error shouldBe null
                game.resolveStack()

                withClue("both lands entered, and they entered tapped") {
                    val forests = game.findAllPermanents("Forest")
                    forests.size shouldBe 9
                    val mountain = game.findPermanent("Mountain")!!
                    game.state.getEntity(mountain)?.get<TappedComponent>() shouldBe TappedComponent
                }
                withClue("the nonland card stayed in the library — nothing is milled") {
                    game.librarySize(1) shouldBe librarySizeBefore - 2
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                    game.findCardsInLibrary(1, "Centaur Courser").size shouldBe 1
                }
                withClue("8 life gained") {
                    game.getLifeTotal(1) shouldBe 28
                }
            }

            test("choosing no lands still shuffles and gains 8 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Through the Forest Gate")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)
                game.castSpell(1, "Through the Forest Gate").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("declining every land leaves the library untouched") {
                    game.librarySize(1) shouldBe librarySizeBefore
                    game.findAllPermanents("Forest").size shouldBe 8
                }
                withClue("the life gain is not conditional on putting lands in") {
                    game.getLifeTotal(1) shouldBe 28
                }
            }

            test("a library smaller than twenty cards simply offers what is there") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Through the Forest Gate")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Through the Forest Gate").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                decision.options.size shouldBe 1
                game.selectCards(decision.options).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mountain") shouldBe true
                game.librarySize(1) shouldBe 0
                game.getLifeTotal(1) shouldBe 28
            }
        }
    }
}
