package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dirge of Dread.
 *
 * Card reference:
 * - Dirge of Dread ({2}{B}): Sorcery
 *   "All creatures gain fear until end of turn."
 *   Cycling {1}{B}
 *   "When you cycle Dirge of Dread, you may have target creature gain fear until end of turn."
 */
class DirgeOfDreadScenarioTest : ScenarioTestBase() {

    init {
        context("Dirge of Dread cycling trigger") {
            test("cycling trigger grants fear to target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Dirge of Dread")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Mountain") // Card to draw from cycling
                    .withCardOnBattlefield(1, "Glory Seeker") // Creature to target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle the card - triggers "When you cycle"
                val cycleResult = game.cycleCard(1, "Dirge of Dread")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Cycling trigger fires - pending decision for target selection
                withClue("Should have pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as the target
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // MayEffect asks yes/no
                withClue("Should have may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Verify Glory Seeker has fear via client state
                val clientState = game.getClientState(1)
                val glorySeeker = clientState.cards[targetId]
                withClue("Glory Seeker should have fear keyword") {
                    glorySeeker!!.keywords.contains(Keyword.FEAR) shouldBe true
                }
            }

            test("cycling trigger can be declined") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Dirge of Dread")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle
                game.cycleCard(1, "Dirge of Dread")

                // Target selection
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability
                game.resolveStack()

                // Decline
                game.answerYesNo(false)

                // Glory Seeker should NOT have fear
                val clientState = game.getClientState(1)
                val glorySeeker = clientState.cards[targetId]
                withClue("Glory Seeker should not have fear keyword") {
                    glorySeeker!!.keywords.contains(Keyword.FEAR) shouldBe false
                }
            }
        }

        context("Dirge of Dread main spell") {
            test("grants fear to all creatures until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Dirge of Dread")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the spell (no targets for the main spell)
                val castResult = game.castSpell(1, "Dirge of Dread")
                withClue("Casting Dirge of Dread should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Both creatures should have fear
                val clientState = game.getClientState(1)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!

                withClue("Glory Seeker should have fear") {
                    clientState.cards[glorySeekerId]!!.keywords.contains(Keyword.FEAR) shouldBe true
                }
                withClue("Elvish Warrior should have fear") {
                    clientState.cards[elvishWarriorId]!!.keywords.contains(Keyword.FEAR) shouldBe true
                }
            }
        }
    }
}
