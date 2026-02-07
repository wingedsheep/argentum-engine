package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Snapping Thragg.
 *
 * Card reference:
 * - Snapping Thragg ({4}{R}): Creature — Beast, 3/3
 *   "Whenever Snapping Thragg deals combat damage to a player, you may have it deal 3 damage
 *   to target creature that player controls."
 *   Morph {4}{R}{R}
 */
class SnappingThraggScenarioTest : ScenarioTestBase() {

    init {
        context("Snapping Thragg combat damage trigger") {
            test("deals 3 damage to target creature when dealing combat damage to player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Snapping Thragg")
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should have Towering Baloth on battlefield") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Snapping Thragg
                val attackResult = game.declareAttackers(mapOf("Snapping Thragg" to 2))
                withClue("Declaring Snapping Thragg as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // MayEffect asks yes/no first
                game.answerYesNo(true)

                // Select Towering Baloth as the target
                val targetId = game.findPermanent("Towering Baloth")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Towering Baloth is 7/6 and takes 3 damage - should survive
                withClue("Towering Baloth should survive 3 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                // Opponent should have taken 3 combat damage from Snapping Thragg
                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("kills a smaller creature with the trigger") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Snapping Thragg")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Snapping Thragg" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                game.answerYesNo(true)

                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))
                game.resolveStack()

                // Glory Seeker is 2/2 and takes 3 damage - should die
                withClue("Glory Seeker should be destroyed by 3 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("may decline the triggered ability") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Snapping Thragg")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Snapping Thragg" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Decline the "you may" effect
                game.answerYesNo(false)

                // Glory Seeker should still be alive
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Opponent still took 3 combat damage
                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("no trigger when blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Snapping Thragg")
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Snapping Thragg" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Towering Baloth
                game.declareBlockers(mapOf("Towering Baloth" to listOf("Snapping Thragg")))

                // Advance through combat - Snapping Thragg dies, no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Snapping Thragg should be in graveyard") {
                    game.isInGraveyard(1, "Snapping Thragg") shouldBe true
                }

                withClue("Opponent should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
