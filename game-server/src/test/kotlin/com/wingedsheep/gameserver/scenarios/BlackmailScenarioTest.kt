package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Blackmail.
 *
 * Card reference:
 * - Blackmail (B): Sorcery. "Target player reveals three cards from their hand
 *   and you choose one of them. That player discards that card."
 *
 * This tests:
 * - Target player with more than 3 cards chooses which 3 to reveal, then controller picks 1
 * - Target player with 3 or fewer cards auto-reveals all, then controller picks 1
 * - Target player with empty hand: nothing happens
 * - Can target yourself
 */
class BlackmailScenarioTest : ScenarioTestBase() {

    init {
        context("Blackmail forces target player to reveal and discard") {

            test("opponent with more than 3 cards must choose 3 to reveal, then caster chooses 1 to discard") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blackmail")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify starting hand sizes
                game.handSize(2) shouldBe 4

                // Cast Blackmail targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Blackmail", 2)
                withClue("Blackmail should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Step 1: Opponent should have a pending decision to choose 3 cards to reveal
                val revealDecision = game.getPendingDecision()
                withClue("There should be a pending decision for opponent to reveal cards") {
                    revealDecision shouldNotBe null
                    revealDecision!!.playerId shouldBe game.player2Id
                }

                // Opponent reveals 3 of their 4 cards (e.g., Grizzly Bears, Hill Giant, Shock)
                val bearsId = game.findCardsInHand(2, "Grizzly Bears").first()
                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                val shockId = game.findCardsInHand(2, "Shock").first()
                game.selectCards(listOf(bearsId, giantId, shockId))

                // Step 2: Caster should have a pending decision to choose 1 card to discard
                val chooseDecision = game.getPendingDecision()
                withClue("There should be a pending decision for caster to choose a card") {
                    chooseDecision shouldNotBe null
                    chooseDecision!!.playerId shouldBe game.player1Id
                }

                // Caster chooses Hill Giant for opponent to discard
                game.selectCards(listOf(giantId))

                // Verify Hill Giant is in opponent's graveyard
                withClue("Hill Giant should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }

                // Opponent's hand should have 3 cards remaining
                withClue("Opponent should have 3 cards left in hand") {
                    game.handSize(2) shouldBe 3
                }
            }

            test("opponent with 3 or fewer cards auto-reveals all, caster chooses 1 to discard") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blackmail")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(2) shouldBe 2

                // Cast Blackmail targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Blackmail", 2)
                withClue("Blackmail should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Since opponent has only 2 cards (≤3), they skip the reveal step
                // Caster should directly get the choice of which card to discard
                val chooseDecision = game.getPendingDecision()
                withClue("Caster should have a pending decision to choose a card to discard") {
                    chooseDecision shouldNotBe null
                    chooseDecision!!.playerId shouldBe game.player1Id
                }

                // Caster chooses Grizzly Bears
                val bearsId = game.findCardsInHand(2, "Grizzly Bears").first()
                game.selectCards(listOf(bearsId))

                // Verify Grizzly Bears is in opponent's graveyard
                withClue("Grizzly Bears should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                // Opponent should have 1 card remaining
                withClue("Opponent should have 1 card left in hand") {
                    game.handSize(2) shouldBe 1
                }
            }

            test("opponent with empty hand - nothing happens") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blackmail")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    // Opponent has no cards in hand
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(2) shouldBe 0

                // Cast Blackmail targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Blackmail", 2)
                withClue("Blackmail should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // No decision should be pending since hand was empty
                withClue("No pending decision when targeting player with empty hand") {
                    game.getPendingDecision() shouldBe null
                }

                // Opponent still has 0 cards
                game.handSize(2) shouldBe 0
            }

            test("can target yourself") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blackmail")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 has Blackmail + 2 other cards = 3 cards
                // After casting Blackmail (removed from hand), they'll have 2 cards
                // So Blackmail goes to stack, hand has 2 cards

                // Cast Blackmail targeting self
                val castResult = game.castSpellTargetingPlayer(1, "Blackmail", 1)
                withClue("Blackmail should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Player 1's hand now has 2 cards (Grizzly Bears, Hill Giant)
                // Since ≤3, skip to controller choosing
                val chooseDecision = game.getPendingDecision()
                withClue("Caster should choose from their own revealed hand") {
                    chooseDecision shouldNotBe null
                    chooseDecision!!.playerId shouldBe game.player1Id
                }

                // Choose to discard Hill Giant from own hand
                val giantId = game.findCardsInHand(1, "Hill Giant").first()
                game.selectCards(listOf(giantId))

                withClue("Hill Giant should be in caster's graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("Caster should have 1 card left in hand") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
