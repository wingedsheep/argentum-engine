package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Head Games.
 *
 * Card reference:
 * - Head Games (3BB): Sorcery. "Target opponent puts the cards from their hand on top
 *   of their library. Search that player's library for that many cards. The player puts
 *   those cards into their hand, then shuffles."
 *
 * This tests:
 * - Opponent's hand is moved to library, then caster chooses replacement cards
 * - Caster can "fail to find" leaving opponent with empty hand
 * - Opponent's original hand cards appear in search results
 * - Empty hand does nothing
 */
class HeadGamesScenarioTest : ScenarioTestBase() {

    init {
        context("Head Games replaces opponent's hand") {

            test("caster searches opponent's library and chooses cards for their new hand") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Head Games")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(2, "Shock")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 2 cards in hand") {
                    game.handSize(2) shouldBe 2
                }

                // Cast Head Games targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Head Games", 2)
                withClue("Head Games should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Caster should get a SelectCardsDecision to pick cards from opponent's library
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                withClue("Decision should be for the caster") {
                    decision.playerId shouldBe game.player1Id
                }

                withClue("Max selections should equal opponent's original hand size") {
                    decision.maxSelections shouldBe 2
                }

                // The opponent's original hand cards should be in the library now
                withClue("Opponent's original hand cards should be in search options") {
                    val cardNames = decision.cardInfo!!.values.map { it.name }
                    cardNames.contains("Grizzly Bears") shouldBe true
                    cardNames.contains("Hill Giant") shouldBe true
                }

                // Select Shock and Forest from the library
                val shockId = decision.cardInfo!!.entries.find { it.value.name == "Shock" }?.key
                val forestId = decision.cardInfo!!.entries.find { it.value.name == "Forest" }?.key
                shockId shouldNotBe null
                forestId shouldNotBe null

                game.submitDecision(
                    CardsSelectedResponse(decision.id, listOfNotNull(shockId, forestId))
                )

                // Opponent should now have Shock and Forest in hand
                withClue("Opponent should have Shock in hand") {
                    game.isInHand(2, "Shock") shouldBe true
                }
                withClue("Opponent should have Forest in hand") {
                    game.isInHand(2, "Forest") shouldBe true
                }
                withClue("Opponent should have 2 cards in hand") {
                    game.handSize(2) shouldBe 2
                }
            }

            test("caster can fail to find, leaving opponent with empty hand") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Head Games")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(2, "Shock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(2) shouldBe 2

                val castResult = game.castSpellTargetingPlayer(1, "Head Games", 2)
                withClue("Head Games should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                // Fail to find - select nothing
                game.submitDecision(
                    CardsSelectedResponse(decision.id, emptyList())
                )

                withClue("Opponent should have empty hand after fail to find") {
                    game.handSize(2) shouldBe 0
                }
            }

            test("does nothing when opponent's hand is empty") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Head Games")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    // Opponent has no cards in hand
                    .withCardInLibrary(2, "Shock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(2) shouldBe 0

                val castResult = game.castSpellTargetingPlayer(1, "Head Games", 2)
                withClue("Head Games should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Pipeline produces a selection with maxSelections=0; submit empty selection
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                withClue("Max selections should be 0 since hand was empty") {
                    decision.maxSelections shouldBe 0
                }

                game.submitDecision(
                    CardsSelectedResponse(decision.id, emptyList())
                )

                game.handSize(2) shouldBe 0
            }
        }
    }
}
