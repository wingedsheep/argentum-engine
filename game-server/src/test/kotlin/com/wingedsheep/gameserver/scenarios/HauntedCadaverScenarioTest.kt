package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Haunted Cadaver.
 *
 * Card reference:
 * - Haunted Cadaver ({3}{B}): Creature — Zombie, 2/2
 *   "Whenever Haunted Cadaver deals combat damage to a player,
 *    you may sacrifice it. If you do, that player discards three cards."
 *   Morph {1}{B}
 */
class HauntedCadaverScenarioTest : ScenarioTestBase() {

    /**
     * Advance by passing priority until a pending decision appears.
     */
    private fun TestGame.advanceUntilDecision(maxIterations: Int = 50) {
        var iterations = 0
        while (!hasPendingDecision() && iterations < maxIterations) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
            iterations++
        }
    }

    init {
        context("Haunted Cadaver combat damage trigger") {
            test("opponent discards 3 cards when player accepts sacrifice (auto-discard)") {
                // Opponent has exactly 3 cards so discard is automatic
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Haunted Cadaver")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 3 cards in hand") {
                    game.handSize(2) shouldBe 3
                }

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Haunted Cadaver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage until the MayEffect decision appears
                game.advanceUntilDecision()

                withClue("Should have a yes/no decision for sacrifice") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Accept the sacrifice
                game.answerYesNo(true)

                withClue("Haunted Cadaver should be sacrificed") {
                    game.isOnBattlefield("Haunted Cadaver") shouldBe false
                    game.isInGraveyard(1, "Haunted Cadaver") shouldBe true
                }

                withClue("Opponent should have discarded all 3 cards") {
                    game.handSize(2) shouldBe 0
                }

                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("opponent chooses which 3 cards to discard when they have more") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Haunted Cadaver")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 4 cards in hand") {
                    game.handSize(2) shouldBe 4
                }

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Haunted Cadaver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage until the MayEffect decision appears
                game.advanceUntilDecision()

                withClue("Should have a yes/no decision for sacrifice") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Accept the sacrifice
                game.answerYesNo(true)

                // Opponent needs to choose which 3 cards to discard
                withClue("Opponent should have a discard decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select 3 cards to discard (both Swamps + 1 Forest)
                val swamps = game.findCardsInHand(2, "Swamp")
                val forests = game.findCardsInHand(2, "Forest")
                game.selectCards(swamps + forests.take(1))

                withClue("Opponent should have 1 card left in hand") {
                    game.handSize(2) shouldBe 1
                }

                withClue("Remaining card should be a Forest") {
                    game.isInHand(2, "Forest") shouldBe true
                }

                withClue("Haunted Cadaver should be sacrificed") {
                    game.isOnBattlefield("Haunted Cadaver") shouldBe false
                    game.isInGraveyard(1, "Haunted Cadaver") shouldBe true
                }
            }

            test("creature stays when player declines sacrifice") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Haunted Cadaver")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should start with 3 cards in hand") {
                    game.handSize(2) shouldBe 3
                }

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Haunted Cadaver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage until the MayEffect decision appears
                game.advanceUntilDecision()

                withClue("Should have a yes/no decision for sacrifice") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Decline the sacrifice
                game.answerYesNo(false)

                withClue("Haunted Cadaver should still be on battlefield") {
                    game.isOnBattlefield("Haunted Cadaver") shouldBe true
                }

                withClue("Opponent should still have 3 cards in hand") {
                    game.handSize(2) shouldBe 3
                }

                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("no trigger when blocked and killed in combat") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Haunted Cadaver")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 blocker
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Haunted Cadaver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Haunted Cadaver")))

                // Advance through combat - both 2/2s kill each other, no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Haunted Cadaver should be dead") {
                    game.isOnBattlefield("Haunted Cadaver") shouldBe false
                    game.isInGraveyard(1, "Haunted Cadaver") shouldBe true
                }

                withClue("Opponent should still have 3 cards in hand (no trigger)") {
                    game.handSize(2) shouldBe 3
                }

                withClue("Opponent should not have taken damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
