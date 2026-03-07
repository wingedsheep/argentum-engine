package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Parallel Thoughts.
 *
 * Card reference:
 * - Parallel Thoughts ({3}{U}{U}): Enchantment
 *   When this enchantment enters, search your library for seven cards, exile them
 *   in a face-down pile, and shuffle that pile. Then shuffle your library.
 *   If you would draw a card, you may instead put the top card of the pile you
 *   exiled into your hand.
 *
 * Tests use Touch of Brilliance (draw 2) to trigger the draw replacement via spell,
 * since passUntilPhase auto-resolves YesNoDecisions during draw step advancement.
 */
class ParallelThoughtsScenarioTest : ScenarioTestBase() {

    private fun createExileCard(game: TestGame, name: String, index: Int): EntityId {
        val cardId = EntityId.of("exile-${index}")
        val cardDef = cardRegistry.getCard(name)!!
        game.state = game.state.withEntity(cardId, ComponentContainer.of(
            CardComponent(
                cardDefinitionId = cardDef.name,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                colors = cardDef.colors,
                baseKeywords = cardDef.keywords,
                baseFlags = cardDef.flags,
                baseStats = cardDef.creatureStats,
                ownerId = game.player1Id,
                spellEffect = cardDef.spellEffect,
                imageUri = cardDef.metadata.imageUri,
            ),
            OwnerComponent(game.player1Id)
        ))
        game.state = game.state.addToZone(ZoneKey(game.player1Id, Zone.EXILE), cardId)
        return cardId
    }

    private fun buildWithLinkedExile(
        exileCardNames: List<String>,
        p1LibraryCards: List<String> = listOf("Island", "Island", "Island")
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Parallel Thoughts")
            // Touch of Brilliance ({3}{U}) — draw 2 cards, used to trigger draw replacement
            .withCardInHand(1, "Touch of Brilliance")
            .withLandsOnBattlefield(1, "Island", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        for (name in p1LibraryCards) {
            builder.withCardInLibrary(1, name)
        }
        builder.withCardInLibrary(2, "Island")
        builder.withCardInLibrary(2, "Island")

        val game = builder.build()

        val exileIds = exileCardNames.mapIndexed { index, name ->
            createExileCard(game, name, index)
        }

        val ptId = game.findPermanent("Parallel Thoughts")!!
        game.state = game.state.updateEntity(ptId) { c ->
            c.with(LinkedExileComponent(exileIds))
        }

        return game
    }

    init {
        context("Parallel Thoughts draw replacement - accept") {

            test("accepting replacement takes top card from exiled pile instead of drawing") {
                val game = buildWithLinkedExile(
                    exileCardNames = listOf("Glory Seeker", "Carbonize"),
                    p1LibraryCards = listOf("Mountain", "Mountain")
                )

                val initialLibrarySize = game.librarySize(1)

                // Cast Touch of Brilliance (draw 2)
                game.castSpell(1, "Touch of Brilliance")
                game.resolveStack()

                // Should get a YesNoDecision for the first draw replacement
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()

                // Accept the replacement for first draw
                game.answerYesNo(true)

                // Glory Seeker should be in hand (from exile pile)
                game.isInHand(1, "Glory Seeker") shouldBe true

                // Library should be unchanged so far (replaced the draw)
                game.librarySize(1) shouldBe initialLibrarySize

                // Second draw should also prompt
                if (game.hasPendingDecision()) {
                    // Accept second replacement too
                    game.answerYesNo(true)
                    game.isInHand(1, "Carbonize") shouldBe true
                    // Library still unchanged
                    game.librarySize(1) shouldBe initialLibrarySize
                }

                // Linked exile should be depleted
                val ptId = game.findPermanent("Parallel Thoughts")!!
                val linked = game.state.getEntity(ptId)!!.get<LinkedExileComponent>()!!
                linked.exiledIds.size shouldBe 0
            }
        }

        context("Parallel Thoughts draw replacement - decline") {

            test("declining replacement draws normally from library") {
                val game = buildWithLinkedExile(
                    exileCardNames = listOf("Glory Seeker", "Carbonize"),
                    p1LibraryCards = listOf("Mountain", "Mountain")
                )

                val initialLibrarySize = game.librarySize(1)

                // Cast Touch of Brilliance (draw 2)
                game.castSpell(1, "Touch of Brilliance")
                game.resolveStack()

                // Should get a YesNoDecision
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()

                // Decline the replacement - should draw from library instead
                game.answerYesNo(false)

                // Library should shrink by 1
                game.librarySize(1) shouldBe initialLibrarySize - 1

                // Second draw should also prompt (Parallel Thoughts still has exile cards)
                if (game.hasPendingDecision()) {
                    // Decline again
                    game.answerYesNo(false)
                    game.librarySize(1) shouldBe initialLibrarySize - 2
                }

                // Exile pile should be unchanged (both declined)
                val ptId = game.findPermanent("Parallel Thoughts")!!
                val linked = game.state.getEntity(ptId)!!.get<LinkedExileComponent>()!!
                linked.exiledIds.size shouldBe 2
            }
        }

        context("Parallel Thoughts - empty pile") {

            test("replacement still offered when pile is empty, accepting does nothing") {
                val game = buildWithLinkedExile(
                    exileCardNames = emptyList(),
                    p1LibraryCards = listOf("Mountain", "Mountain")
                )

                // Cast Touch of Brilliance (draw 2)
                game.castSpell(1, "Touch of Brilliance")
                game.resolveStack()

                // Should get a YesNoDecision even with empty pile (per rulings)
                game.hasPendingDecision() shouldBe true

                val handBefore = game.handSize(1)
                val libBefore = game.librarySize(1)

                // Accept - pile is empty so no card gained, draw was replaced with nothing
                game.answerYesNo(true)

                // Hand should not have grown from the replacement (empty pile)
                game.handSize(1) shouldBe handBefore
                // Library unchanged for this draw
                game.librarySize(1) shouldBe libBefore
            }
        }

        context("Parallel Thoughts - mix accept and decline") {

            test("can accept some draws and decline others") {
                val game = buildWithLinkedExile(
                    exileCardNames = listOf("Glory Seeker", "Carbonize"),
                    p1LibraryCards = listOf("Mountain", "Mountain")
                )

                val initialLibrarySize = game.librarySize(1)

                // Cast Touch of Brilliance (draw 2)
                game.castSpell(1, "Touch of Brilliance")
                game.resolveStack()

                // First draw: accept replacement
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.isInHand(1, "Glory Seeker") shouldBe true
                game.librarySize(1) shouldBe initialLibrarySize

                // Second draw: decline replacement
                if (game.hasPendingDecision()) {
                    game.answerYesNo(false)
                    // Now should have drawn from library
                    game.librarySize(1) shouldBe initialLibrarySize - 1
                }

                // Exile pile should have 1 remaining (only first was taken)
                val ptId = game.findPermanent("Parallel Thoughts")!!
                val linked = game.state.getEntity(ptId)!!.get<LinkedExileComponent>()!!
                linked.exiledIds.size shouldBe 1
            }
        }
    }
}
