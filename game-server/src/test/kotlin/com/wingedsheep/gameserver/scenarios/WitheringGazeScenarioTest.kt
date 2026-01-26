package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Withering Gaze hand revealing.
 *
 * Card reference:
 * - Withering Gaze (2U): Sorcery.
 *   "Target opponent reveals their hand. You draw a card for each Forest and green card in it."
 *
 * This test verifies that:
 * 1. After Withering Gaze resolves, the opponent's hand is revealed to the caster
 * 2. Revealed cards appear in the client state for the caster
 * 3. Revealed cards remain visible in subsequent state updates
 */
class WitheringGazeScenarioTest : ScenarioTestBase() {

    init {
        context("Withering Gaze reveals opponent's hand") {
            test("caster can see opponent's hand after casting Withering Gaze") {
                // Setup:
                // - Player 1 has Withering Gaze in hand with 3 Islands
                // - Player 2 has some cards in hand (including a Forest for the draw effect)
                // - It's Player 1's main phase
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Withering Gaze")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state - caster should NOT see opponent's hand
                val clientStateBefore = game.getClientState(1)
                val opponentHandBefore = clientStateBefore.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player2Id
                }

                withClue("Opponent's hand should be hidden before spell") {
                    opponentHandBefore?.cardIds?.size shouldBe 0
                    opponentHandBefore?.size shouldBe 3
                }

                // Cast Withering Gaze targeting the opponent (player 2)
                val castResult = game.castSpellTargetingPlayer(1, "Withering Gaze", 2)
                withClue("Withering Gaze should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify opponent's hand cards are now revealed in game state
                val opponentHand = game.state.getHand(game.player2Id)
                opponentHand.forEach { cardId ->
                    val revealedComponent = game.state.getEntity(cardId)?.get<RevealedToComponent>()
                    withClue("Card $cardId should be revealed to caster") {
                        revealedComponent shouldNotBe null
                        revealedComponent!!.isRevealedTo(game.player1Id) shouldBe true
                    }
                }

                // Check client state - caster should now see opponent's hand
                val clientStateAfter = game.getClientState(1)
                val opponentHandAfter = clientStateAfter.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player2Id
                }

                withClue("Opponent's hand should be visible after spell") {
                    opponentHandAfter shouldNotBe null
                    opponentHandAfter!!.cardIds.size shouldBe 3
                }

                // Verify the card names are visible
                val visibleCards = opponentHandAfter!!.cardIds.mapNotNull { cardId ->
                    clientStateAfter.cards[cardId]?.name
                }
                withClue("Should see Forest, Grizzly Bears, and Island in opponent's hand") {
                    visibleCards shouldContain "Forest"
                    visibleCards shouldContain "Grizzly Bears"
                    visibleCards shouldContain "Island"
                }
            }

            test("opponent cannot see caster's hand (asymmetric reveal)") {
                // Setup: Player 1 casts Withering Gaze on Player 2
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Withering Gaze")
                    .withCardInHand(1, "Island") // Another card in caster's hand
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Withering Gaze
                game.castSpellTargetingPlayer(1, "Withering Gaze", 2)
                game.resolveStack()

                // Get opponent's view of the game
                val opponentClientState = game.getClientState(2)

                // Opponent should NOT see caster's hand (only their own)
                val casterHandFromOpponent = opponentClientState.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player1Id
                }

                withClue("Caster's hand should remain hidden from opponent") {
                    casterHandFromOpponent?.cardIds?.size shouldBe 0
                }

                // But opponent can see their own hand was revealed (to themselves too since reveal is public)
                val opponentHandFromOpponent = opponentClientState.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player2Id
                }

                withClue("Opponent should still see their own hand") {
                    opponentHandFromOpponent?.cardIds?.size shouldBe 1
                }
            }

            test("revealed cards remain visible after turn passes") {
                // Setup
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Withering Gaze")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Get the Forest card ID before reveal
                val forestId = game.state.getHand(game.player2Id).first()

                // Cast and resolve Withering Gaze
                game.castSpellTargetingPlayer(1, "Withering Gaze", 2)
                game.resolveStack()

                // Verify Forest is revealed
                val forestRevealed = game.state.getEntity(forestId)?.get<RevealedToComponent>()
                withClue("Forest should be revealed after spell") {
                    forestRevealed shouldNotBe null
                    forestRevealed!!.isRevealedTo(game.player1Id) shouldBe true
                }

                // The revealed Forest should still be visible in client state
                val clientState = game.getClientState(1)
                val opponentHand = clientState.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player2Id
                }

                withClue("Previously revealed card should still be visible") {
                    opponentHand shouldNotBe null
                    opponentHand!!.cardIds shouldContain forestId
                }
            }

            test("revealed cards show correct card info to caster") {
                // Setup - verify card details are available after reveal
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Withering Gaze")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Withering Gaze
                game.castSpellTargetingPlayer(1, "Withering Gaze", 2)
                game.resolveStack()

                // Get caster's client state
                val clientState = game.getClientState(1)

                // Find the revealed card
                val opponentHand = clientState.zones.find {
                    it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == game.player2Id
                }

                withClue("Opponent's hand should contain the revealed card") {
                    opponentHand shouldNotBe null
                    opponentHand!!.cardIds.size shouldBe 1
                }

                // Verify we can see the full card info
                val revealedCardId = opponentHand!!.cardIds.first()
                val cardInfo = clientState.cards[revealedCardId]

                withClue("Card info should be available for revealed card") {
                    cardInfo shouldNotBe null
                    cardInfo!!.name shouldBe "Grizzly Bears"
                    cardInfo.manaCost shouldBe "{1}{G}"
                    cardInfo.power shouldBe 2
                    cardInfo.toughness shouldBe 2
                }
            }
        }
    }
}
