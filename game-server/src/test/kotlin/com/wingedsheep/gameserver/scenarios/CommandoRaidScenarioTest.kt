package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Commando Raid.
 *
 * Card reference:
 * - Commando Raid ({2}{R}): Instant
 *   "Until end of turn, target creature you control gains 'Whenever this creature deals
 *   combat damage to a player, you may have it deal damage equal to its power to target
 *   creature that player controls.'"
 */
class CommandoRaidScenarioTest : ScenarioTestBase() {

    init {
        context("Commando Raid grants combat damage trigger") {
            test("creature deals damage equal to its power to target creature after combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature (no built-in trigger)
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 creature
                    .withCardInHand(1, "Commando Raid")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat, declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))

                // Advance to declare blockers step
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Cast Commando Raid targeting Glory Seeker BEFORE declaring blockers
                val seekerId = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Commando Raid", seekerId)
                withClue("Commando Raid should cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell so the ability is granted
                game.resolveStack()

                withClue("Granted trigger should be stored in game state") {
                    game.state.grantedTriggeredAbilities.size shouldBe 1
                }

                // Declare no blockers
                game.declareNoBlockers()

                // Now advance through combat damage - trigger fires
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending may decision from granted trigger (iterations=$iterations, step=${game.state.step}, phase=${game.state.phase})") {
                    game.hasPendingDecision() shouldBe true
                }

                // MayEffect asks yes/no first
                game.answerYesNo(true)

                // Select Towering Baloth as the target
                val balothId = game.findPermanent("Towering Baloth")!!
                game.selectTargets(listOf(balothId))

                // Resolve the triggered ability
                game.resolveStack()

                // Towering Baloth is 7/6 and takes 2 damage (Glory Seeker's power) - should survive
                withClue("Towering Baloth should still be alive (7/6 with 2 damage)") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                // Opponent should also have taken 2 combat damage from Glory Seeker
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("creature kills target creature when power is sufficient") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando") // 2/1 goblin
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 creature
                    .withCardInHand(1, "Commando Raid")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Skirk Commando" to 2))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Cast Commando Raid targeting Skirk Commando
                val commandoId = game.findPermanent("Skirk Commando")!!
                game.castSpell(1, "Commando Raid", commandoId)
                game.resolveStack()

                // Advance through combat damage
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Accept the may trigger
                game.answerYesNo(true)

                // Select Glory Seeker as target
                val seekerId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(seekerId))

                // Resolve the triggered ability
                game.resolveStack()

                // Glory Seeker is 2/2 and takes 2 damage (Skirk Commando's power) - should die
                withClue("Glory Seeker should be destroyed by 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                // Opponent also took 2 combat damage
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("may decline the triggered ability") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando") // 2/1
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withCardInHand(1, "Commando Raid")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Skirk Commando" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                val commandoId = game.findPermanent("Skirk Commando")!!
                game.castSpell(1, "Commando Raid", commandoId)
                game.resolveStack()

                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Decline the may trigger
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

            test("no trigger when creature is blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skirk Commando") // 2/1
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withCardInHand(1, "Commando Raid")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Skirk Commando" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Cast Commando Raid before blockers declared
                val commandoId = game.findPermanent("Skirk Commando")!!
                game.castSpell(1, "Commando Raid", commandoId)
                game.resolveStack()

                // Block Skirk Commando with Glory Seeker
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Skirk Commando")))

                // Pass through combat - both creatures die, no combat damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // No trigger fired because Skirk Commando was blocked and didn't deal damage to player
                withClue("Skirk Commando should be in graveyard") {
                    game.isInGraveyard(1, "Skirk Commando") shouldBe true
                }

                withClue("Opponent should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
