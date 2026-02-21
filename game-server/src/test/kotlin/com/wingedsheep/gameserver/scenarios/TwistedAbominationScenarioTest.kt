package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Twisted Abomination.
 *
 * Twisted Abomination: {5}{B} Creature — Zombie Mutant 5/3
 * {B}: Regenerate Twisted Abomination.
 * Swampcycling {2}
 */
class TwistedAbominationScenarioTest : ScenarioTestBase() {

    init {
        context("Swampcycling") {
            test("swampcycling discards card and searches library for a Swamp") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Abomination")
                    .withLandsOnBattlefield(1, "Mountain", 2) // {2} for swampcycling cost
                    .withCardInLibrary(1, "Forest") // Non-swamp card
                    .withCardInLibrary(1, "Swamp") // Swamp to find
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Should start with 1 card in hand") {
                    game.handSize(1) shouldBe 1
                }

                // Swampcycle the card
                val result = game.typecycleCard(1, "Twisted Abomination")
                withClue("Should typecycle successfully (pauses for library search)") {
                    result.error shouldBe null
                }

                // Should have a pending library search decision
                withClue("Should have pending library search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select the Swamp from the library
                val decision = game.getPendingDecision()!! as com.wingedsheep.engine.core.SelectCardsDecision
                val swampId = decision.cardInfo!!.entries.first { it.value.name == "Swamp" }.key
                val selectResult = game.selectCards(listOf(swampId))

                // Verify that CardsRevealedEvent is emitted with imageUris
                // (opponent relies on event imageUris since the card is in a hidden zone)
                val revealEvent = selectResult.events.filterIsInstance<CardsRevealedEvent>().firstOrNull()
                withClue("Should have CardsRevealedEvent in selection result") {
                    revealEvent.shouldNotBeNull()
                }
                withClue("CardsRevealedEvent should have non-empty imageUris") {
                    revealEvent!!.imageUris.shouldNotBeEmpty()
                }
                withClue("CardsRevealedEvent imageUri should not be null for the revealed card") {
                    revealEvent!!.imageUris[0] shouldNotBe null
                }

                // Twisted Abomination should be in graveyard (discarded)
                withClue("Twisted Abomination should be in graveyard") {
                    game.isInGraveyard(1, "Twisted Abomination") shouldBe true
                }

                // Swamp should be in hand (found via search)
                withClue("Should have 1 card in hand (Swamp from search)") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Swamp should be in hand") {
                    game.isInHand(1, "Swamp") shouldBe true
                }
            }

            test("swampcycling with fail-to-find when no Swamps in library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Abomination")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest") // No Swamps
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.typecycleCard(1, "Twisted Abomination")
                withClue("Should typecycle successfully") {
                    result.error shouldBe null
                }

                // When no matching cards exist, search auto-completes (no decision needed)
                withClue("Should not have pending decision (no matching cards)") {
                    game.hasPendingDecision() shouldBe false
                }

                // Twisted Abomination should be in graveyard
                withClue("Twisted Abomination should be in graveyard") {
                    game.isInGraveyard(1, "Twisted Abomination") shouldBe true
                }

                // Hand should be empty (discarded card, found nothing)
                withClue("Should have 0 cards in hand") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("cannot swampcycle without enough mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Abomination")
                    .withLandsOnBattlefield(1, "Mountain", 1) // Only 1 mana, need 2
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.typecycleCard(1, "Twisted Abomination")
                withClue("Should not be able to typecycle without enough mana") {
                    result.error shouldBe "Not enough mana to typecycle this card"
                }

                // Hand should be unchanged
                withClue("Should still have 1 card in hand") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
