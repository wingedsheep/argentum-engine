package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Memory Theft (ELD #96) — "Target opponent reveals their hand. You choose a nonland card from it.
 * That player discards that card. You may put a card that has an Adventure that player owns from
 * exile into that player's graveyard."
 *
 * The exile half is the interesting part: only adventurer cards the *target* owns are offered, the
 * pick is optional, and it is offered even when the hand had nothing to discard.
 */
class MemoryTheftScenarioTest : ScenarioTestBase() {

    private fun exiledCardNamed(game: TestGame, playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    init {
        context("Memory Theft") {

            test("discards a nonland card, then moves an adventurer card the opponent owns from exile to their graveyard") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Memory Theft")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withCardInExile(2, "Brazen Borrower")
                    .withCardInExile(2, "Hill Giant")
                    .withCardInExile(1, "Merfolk Secretkeeper")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInHand(2, "Grizzly Bears").single()
                val borrower = exiledCardNamed(game, 2, "Brazen Borrower")

                game.castSpellTargetingPlayer(1, "Memory Theft", 2).error shouldBe null
                game.resolveStack()

                val discardDecision = game.getPendingDecision()
                discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                val exileDecision = game.getPendingDecision()
                exileDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses") { exileDecision.playerId shouldBe game.player1Id }
                withClue("only the target's face-up adventurer card is offered") {
                    exileDecision.options shouldBe listOf(borrower)
                }
                withClue("the pick is optional") { exileDecision.minSelections shouldBe 0 }
                game.selectCards(listOf(borrower)).error shouldBe null
                game.resolveStack()

                withClue("the nonland card was discarded") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                withClue("the land stays in hand") { game.isInHand(2, "Forest") shouldBe true }
                withClue("the adventurer card went to its owner's graveyard") {
                    game.isInGraveyard(2, "Brazen Borrower") shouldBe true
                }
                withClue("a card without an Adventure stays exiled") { game.isInExile(2, "Hill Giant") shouldBe true }
                withClue("the caster's own adventurer card stays exiled") {
                    game.isInExile(1, "Merfolk Secretkeeper") shouldBe true
                }
            }

            test("the exile pick is offered with an empty hand, and may be declined") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Memory Theft")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInExile(2, "Merfolk Secretkeeper")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val secretkeeper = exiledCardNamed(game, 2, "Merfolk Secretkeeper")

                game.castSpellTargetingPlayer(1, "Memory Theft", 2).error shouldBe null
                game.resolveStack()

                // An empty-hand discard prompt (alwaysPrompt) may come first; skip past it.
                val first = game.getPendingDecision()
                if (first is SelectCardsDecision && secretkeeper !in first.options) {
                    game.selectCards(emptyList()).error shouldBe null
                    game.resolveStack()
                }

                val exileDecision = game.getPendingDecision()
                exileDecision.shouldBeInstanceOf<SelectCardsDecision>()
                exileDecision.options shouldBe listOf(secretkeeper)
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("declining leaves the card in exile") {
                    game.isInExile(2, "Merfolk Secretkeeper") shouldBe true
                }
            }
        }
    }
}
