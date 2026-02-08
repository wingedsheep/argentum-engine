package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cabal Executioner.
 *
 * Card reference:
 * - Cabal Executioner ({2}{B}{B}): Creature — Human Cleric, 2/2
 *   "Whenever Cabal Executioner deals combat damage to a player, that player sacrifices a creature."
 *   Morph {3}{B}{B}
 */
class CabalExecutionerScenarioTest : ScenarioTestBase() {

    init {
        context("Cabal Executioner combat damage trigger") {
            test("opponent sacrifices a creature when Cabal Executioner deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Executioner")
                    .withCardOnBattlefield(2, "Glory Seeker") // Only creature - auto-sacrifice
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should have Glory Seeker on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Cabal Executioner
                val attackResult = game.declareAttackers(mapOf("Cabal Executioner" to 2))
                withClue("Declaring Cabal Executioner as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires and resolves
                // With only 1 creature, the sacrifice is automatic
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent's creature should be sacrificed
                withClue("Glory Seeker should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }

                withClue("Glory Seeker should not be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                // Opponent should have taken 2 combat damage
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("opponent with multiple creatures gets a sacrifice decision") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Executioner")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Cabal Executioner" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires, resolves,
                // and creates a sacrifice decision
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Opponent should have a pending sacrifice decision
                withClue("Opponent should have pending sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker to sacrifice
                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.selectCards(listOf(glorySeeker))

                withClue("Glory Seeker should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }

                withClue("Elvish Warrior should still be on battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
            }

            test("no sacrifice when opponent has no creatures") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Executioner")
                    // Opponent has no creatures
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Cabal Executioner" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires but nothing to sacrifice
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should have taken 2 combat damage
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("Cabal Executioner blocked does not trigger sacrifice") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Executioner")
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Cabal Executioner" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Cabal Executioner with Elvish Warrior
                game.declareBlockers(mapOf("Elvish Warrior" to listOf("Cabal Executioner")))

                // Advance through combat - Cabal Executioner dies (2/2 vs 2/3), no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should NOT have sacrificed (no combat damage to player)
                withClue("Elvish Warrior should still be on battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                // Cabal Executioner should be dead
                withClue("Cabal Executioner should be in graveyard") {
                    game.isInGraveyard(1, "Cabal Executioner") shouldBe true
                }

                // Opponent should not have taken damage
                withClue("Opponent should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
