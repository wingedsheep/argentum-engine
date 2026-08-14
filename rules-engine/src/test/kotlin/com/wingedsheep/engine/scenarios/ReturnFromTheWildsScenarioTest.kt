package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Return from the Wilds. */
class ReturnFromTheWildsScenarioTest : ScenarioTestBase() {

    init {
        context("Return from the Wilds — choose two of three modes") {
            test("Human + Food makes one of each token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Return from the Wilds")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Return from the Wilds").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, emptyList(), chosenModes = listOf(1, 2))
                ).isSuccess shouldBe true
                game.resolveStack()

                game.findPermanents("Human Token").size shouldBe 1
                game.findPermanents("Food").size shouldBe 1
                withClue("the land-search mode was not chosen") {
                    game.librarySize(1) shouldBe 1
                }
            }

            test("land search + Human puts a basic land onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Return from the Wilds")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Return from the Wilds").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, emptyList(), chosenModes = listOf(0, 1))
                ).isSuccess shouldBe true
                game.resolveStack()

                // The library holds a single Plains — the search auto-resolves to it.
                if (game.hasPendingDecision()) {
                    game.selectCards(game.findCardsInLibrary(1, "Plains"))
                    game.resolveStack()
                }

                val plains = game.findPermanent("Plains")
                withClue("the searched basic land is on the battlefield") { plains shouldNotBe null }
                withClue("…and it entered tapped") {
                    game.state.getEntity(plains!!)?.has<TappedComponent>() shouldBe true
                }
                game.findPermanents("Human Token").size shouldBe 1
                game.findPermanents("Food").size shouldBe 0
            }

            test("choosing only one mode is illegal — the spell demands two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Return from the Wilds")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Return from the Wilds").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, emptyList(), chosenModes = listOf(2))
                ).isSuccess shouldBe false
            }
        }
    }
}
