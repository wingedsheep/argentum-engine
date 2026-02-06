package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Skirk Commando.
 *
 * Card reference:
 * - Skirk Commando ({1}{R}{R}): Creature — Goblin, 2/1
 *   "Whenever Skirk Commando deals combat damage to a player, you may have it deal 2 damage
 *   to target creature that player controls."
 *   Morph {2}{R}
 */
class SkirkCommandoScenarioTest : ScenarioTestBase() {

    init {
        context("Skirk Commando combat damage trigger") {
            test("deals 2 damage to target creature when dealing combat damage to player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should have Glory Seeker on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Skirk Commando
                val attackResult = game.declareAttackers(mapOf("Skirk Commando" to 2))
                withClue("Declaring Skirk Commando as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires
                // Pass priority until we get a pending decision (target selection)
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

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

                // Glory Seeker is 2/2 and takes 2 damage - should die
                withClue("Glory Seeker should be destroyed by 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                // Opponent should also have taken 2 combat damage from Skirk Commando
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("no trigger when blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Skirk Commando" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Skirk Commando with Glory Seeker
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Skirk Commando")))

                // Advance through combat - both die (2/1 vs 2/2), no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // No trigger should have fired - no combat damage to player
                withClue("Skirk Commando should be in graveyard") {
                    game.isInGraveyard(1, "Skirk Commando") shouldBe true
                }

                withClue("Opponent should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("may decline the triggered ability") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Skirk Commando" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Select target
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability
                game.resolveStack()

                // Decline the "you may" effect
                game.answerYesNo(false)

                // Glory Seeker should still be alive
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Opponent still took 2 combat damage
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
