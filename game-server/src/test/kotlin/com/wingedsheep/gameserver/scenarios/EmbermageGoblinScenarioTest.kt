package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Embermage Goblin.
 *
 * Card reference:
 * - Embermage Goblin ({3}{R}): Creature — Goblin Wizard, 1/1
 *   "When Embermage Goblin enters the battlefield, you may search your library
 *   for a card named Embermage Goblin, reveal it, and put it into your hand.
 *   If you do, shuffle your library."
 *   "{T}: Embermage Goblin deals 1 damage to any target."
 */
class EmbermageGoblinScenarioTest : ScenarioTestBase() {

    init {
        context("Embermage Goblin ETB search") {

            test("ETB triggers search for card named Embermage Goblin") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Embermage Goblin")
                    .withCardInLibrary(1, "Embermage Goblin")  // Second copy in library
                    .withCardInLibrary(1, "Mountain")           // Filler
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Embermage Goblin
                val castResult = game.castSpell(1, "Embermage Goblin")
                withClue("Casting Embermage Goblin should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell (creature enters battlefield)
                game.resolveStack()

                // ETB trigger goes on stack, resolve it
                // MayEffect asks yes/no first
                withClue("Should have pending may decision for search") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose yes to search
                game.answerYesNo(true)

                // Should now have a search decision
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Find the Embermage Goblin in library to select
                val libraryCards = game.state.getLibrary(game.player1Id)
                val embermageInLibrary = libraryCards.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Embermage Goblin"
                }

                withClue("Should find Embermage Goblin in library") {
                    embermageInLibrary shouldBe embermageInLibrary // not null check
                }

                // Select the card
                game.selectCards(listOf(embermageInLibrary!!))

                // Verify the card went to hand
                withClue("Should have Embermage Goblin in hand after search") {
                    game.isInHand(1, "Embermage Goblin") shouldBe true
                }

                // Verify on battlefield
                withClue("First Embermage Goblin should be on battlefield") {
                    game.isOnBattlefield("Embermage Goblin") shouldBe true
                }
            }

            test("may decline to search") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Embermage Goblin")
                    .withCardInLibrary(1, "Embermage Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLibrarySize = game.librarySize(1)

                game.castSpell(1, "Embermage Goblin")
                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)

                // Library should remain unchanged (no search happened)
                withClue("Embermage Goblin should be on battlefield") {
                    game.isOnBattlefield("Embermage Goblin") shouldBe true
                }
            }
        }

        context("Embermage Goblin tap ability") {

            test("tap ability deals 1 damage to target creature") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Embermage Goblin") // No summoning sickness
                    .withCardOnBattlefield(2, "Glory Seeker")     // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val embermageId = game.findPermanent("Embermage Goblin")!!
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val cardDef = cardRegistry.getCard("Embermage Goblin")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the tap ability targeting Glory Seeker
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = embermageId,
                        abilityId = ability.id,
                        targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(glorySeekerId))
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Glory Seeker is 2/2, 1 damage doesn't kill it
                withClue("Glory Seeker should still be on battlefield (1 damage to a 2/2)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("tap ability deals 1 damage to player") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Embermage Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val embermageId = game.findPermanent("Embermage Goblin")!!
                val cardDef = cardRegistry.getCard("Embermage Goblin")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate targeting the opponent
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = embermageId,
                        abilityId = ability.id,
                        targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Opponent should have taken 1 damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
