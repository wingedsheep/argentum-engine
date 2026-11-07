package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Verdant Crescendo (KLD #273) — two searches in one resolution: a basic land onto the battlefield
 * tapped, then a mandatory search of library *and* graveyard for Nissa, Nature's Artisan, revealed
 * into hand, then one shuffle.
 */
class VerdantCrescendoScenarioTest : ScenarioTestBase() {

    init {
        context("Verdant Crescendo") {

            fun board(nissaInGraveyard: Boolean): TestGame {
                val builder = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Verdant Crescendo")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (nissaInGraveyard) {
                    builder.withCardInGraveyard(1, "Nissa, Nature's Artisan")
                } else {
                    builder.withCardInLibrary(1, "Nissa, Nature's Artisan")
                }
                return builder.build()
            }

            test("fetches a tapped basic, then finds Nissa in the graveyard") {
                val game = board(nissaInGraveyard = true)
                game.castSpell(1, "Verdant Crescendo").error shouldBe null
                game.resolveStack()

                val landSearch = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val libraryForest = game.findCardsInLibrary(1, "Forest").single()
                withClue("only basic lands are offered") {
                    landSearch.options shouldBe listOf(libraryForest)
                }
                game.selectCards(listOf(libraryForest)).error shouldBe null
                withClue("the fetched basic enters tapped") {
                    game.state.getEntity(libraryForest)?.has<TappedComponent>() shouldBe true
                }

                val nissaSearch = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val nissa = game.findCardsInGraveyard(1, "Nissa, Nature's Artisan").single()
                withClue("the second search reaches into the graveyard") {
                    nissaSearch.options shouldBe listOf(nissa)
                }
                game.selectCards(listOf(nissa)).error shouldBe null

                game.isInHand(1, "Nissa, Nature's Artisan") shouldBe true
                game.isInGraveyard(1, "Verdant Crescendo") shouldBe true
                game.librarySize(1) shouldBe 1
            }

            test("the second search also sees Nissa in the library") {
                val game = board(nissaInGraveyard = false)
                game.castSpell(1, "Verdant Crescendo").error shouldBe null
                game.resolveStack()

                game.selectCards(game.findCardsInLibrary(1, "Forest")).error shouldBe null

                val nissaSearch = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                nissaSearch.options shouldContainExactlyInAnyOrder
                    game.findCardsInLibrary(1, "Nissa, Nature's Artisan")
                game.selectCards(nissaSearch.options).error shouldBe null

                game.isInHand(1, "Nissa, Nature's Artisan") shouldBe true
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }
    }
}
