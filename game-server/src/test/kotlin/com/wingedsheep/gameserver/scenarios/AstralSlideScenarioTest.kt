package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AstralSlideScenarioTest : ScenarioTestBase() {

    /**
     * Helper to check if a card is in a player's exile zone.
     */
    private fun ScenarioTestBase.TestGame.isInExile(playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Astral Slide") {
            test("basic cycle-exile-return: cycle a card, exile creature, creature returns at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain") // Card to draw from cycling
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should have Glory Seeker on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Cycle the card - Astral Slide triggers
                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Astral Slide triggers - pending decision for target selection
                withClue("Should have pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as the target
                val targetId = game.findPermanent("Glory Seeker")
                withClue("Glory Seeker should be on battlefield for targeting") {
                    targetId shouldNotBe null
                }
                game.selectTargets(listOf(targetId!!))

                // Ability is now on the stack - resolve it (both players pass priority)
                game.resolveStack()

                // MayEffect should now present a yes/no decision
                withClue("Should have may decision after resolving stack") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Glory Seeker should now be in exile
                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(2, "Glory Seeker") shouldBe true
                }

                // Advance to end step - the delayed trigger should return the creature
                game.passUntilPhase(Phase.ENDING, Step.END)

                // The delayed trigger fires and resolves - the creature comes back
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Glory Seeker should be back on battlefield after end step") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in exile anymore") {
                    game.isInExile(2, "Glory Seeker") shouldBe false
                }
            }

            test("may decline exile: cycle but choose not to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle
                game.cycleCard(1, "Disciple of Grace")

                // Target selection
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Decline the "you may" effect
                withClue("Should have may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                // Creature should still be on the battlefield
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in exile") {
                    game.isInExile(2, "Glory Seeker") shouldBe false
                }
            }

            test("cycling without Astral Slide does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Cycling should succeed without Astral Slide") {
                    cycleResult.error shouldBe null
                }
                withClue("Should not have pending decision") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Should have drawn a card") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Cycled card should be in graveyard") {
                    game.isInGraveyard(1, "Disciple of Grace") shouldBe true
                }
            }
        }
    }
}
