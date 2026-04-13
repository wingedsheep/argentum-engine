package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Ruthless Negotiation.
 *
 * Card reference:
 * - Ruthless Negotiation ({B}): Sorcery
 *   "Target opponent exiles a card from their hand. If this spell was cast
 *    from a graveyard, draw a card.
 *    Flashback {4}{B}"
 */
class RuthlessNegotiationScenarioTest : ScenarioTestBase() {

    init {
        context("Ruthless Negotiation - normal cast from hand") {
            test("target opponent exiles a card from their hand, no draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ruthless Negotiation")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialP1HandSize = game.state.getHand(game.player1Id).size

                // Cast Ruthless Negotiation targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Ruthless Negotiation", 2)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - pauses for opponent to select card
                game.resolveStack()

                // Should have a pending selection decision for opponent
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                // Opponent selects first card to exile
                game.selectCards(listOf(decision.options.first()))

                // Opponent should have 1 card in hand (started with 2, exiled 1)
                val opponentHand = game.state.getHand(game.player2Id)
                withClue("Opponent should have 1 card in hand") {
                    opponentHand shouldHaveSize 1
                }

                // Opponent should have 1 card in exile
                val opponentExile = game.state.getExile(game.player2Id)
                withClue("Opponent should have 1 card in exile") {
                    opponentExile shouldHaveSize 1
                }

                // Controller should NOT have drawn a card (not cast from graveyard)
                val controllerHand = game.state.getHand(game.player1Id)
                withClue("Controller should not have drawn (hand should be empty)") {
                    controllerHand.size shouldBe (initialP1HandSize - 1)
                }
            }
        }

        context("Ruthless Negotiation - flashback from graveyard") {
            test("cast from graveyard exiles opponent card, draws a card, spell goes to exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Ruthless Negotiation")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialP1HandSize = game.state.getHand(game.player1Id).size

                // Find the card in graveyard
                val graveyard = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD))
                val cardId = graveyard.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ruthless Negotiation"
                } ?: error("Ruthless Negotiation not found in graveyard")

                // Cast with flashback (useAlternativeCost = true, targeting opponent)
                val targets = listOf(ChosenTarget.Player(game.player2Id))
                val castResult = game.execute(
                    CastSpell(game.player1Id, cardId, targets, useAlternativeCost = true)
                )
                withClue("Flashback cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - pauses for opponent to select card
                game.resolveStack()

                // Should have a pending selection decision
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                // Opponent selects first card to exile
                game.selectCards(listOf(decision.options.first()))

                // Opponent should have 1 card in hand
                val opponentHand = game.state.getHand(game.player2Id)
                withClue("Opponent should have 1 card in hand after exile") {
                    opponentHand shouldHaveSize 1
                }

                // Controller SHOULD have drawn a card (cast from graveyard)
                val controllerHand = game.state.getHand(game.player1Id)
                withClue("Controller should have drawn a card") {
                    controllerHand.size shouldBe (initialP1HandSize + 1)
                }

                // The spell should be in exile (not graveyard) due to flashback
                val controllerGraveyard = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD))
                val spellInGraveyard = controllerGraveyard.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ruthless Negotiation"
                }
                withClue("Spell should NOT be in graveyard (flashback exiles it)") {
                    spellInGraveyard shouldBe false
                }

                val controllerExile = game.state.getExile(game.player1Id)
                val spellInExile = controllerExile.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Ruthless Negotiation"
                }
                withClue("Spell should be in exile after flashback resolution") {
                    spellInExile shouldBe true
                }
            }
        }
    }
}
