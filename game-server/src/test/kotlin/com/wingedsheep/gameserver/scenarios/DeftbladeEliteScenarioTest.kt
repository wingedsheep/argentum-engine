package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Deftblade Elite's combat damage prevention ability.
 *
 * Card reference:
 * - Deftblade Elite ({W}): 1/1 Creature — Human Soldier
 *   Provoke
 *   {1}{W}: Prevent all combat damage that would be dealt to and dealt by
 *   Deftblade Elite this turn.
 */
class DeftbladeEliteScenarioTest : ScenarioTestBase() {

    init {
        context("Deftblade Elite combat damage prevention") {

            test("activated ability prevents combat damage to and by Deftblade Elite when blocking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deftblade Elite")
                    .withLandsOnBattlefield(1, "Plains", 2) // For {1}{W} cost
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(2) // Opponent's turn so they can attack
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val eliteId = game.findPermanent("Deftblade Elite")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Find the activated ability
                val cardDef = cardRegistry.getCard("Deftblade Elite")!!
                val preventAbility = cardDef.script.activatedAbilities.first()

                // Player 2 (active) passes priority, now player 1 can respond
                game.passPriority()

                // Activate prevention ability
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = eliteId,
                        abilityId = preventAbility.id
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Opponent attacks with Hill Giant
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1))

                // Block with Deftblade Elite
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Deftblade Elite" to listOf("Hill Giant")))

                // Advance through combat damage to postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Deftblade Elite should survive with no damage
                withClue("Deftblade Elite should still be on the battlefield") {
                    game.findPermanent("Deftblade Elite") shouldNotBe null
                }
                val eliteDamage = game.state.getEntity(eliteId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Deftblade Elite should take no combat damage") {
                    eliteDamage shouldBe 0
                }

                // Hill Giant should take no damage (damage BY Deftblade Elite is also prevented)
                val giantDamage = game.state.getEntity(hillGiantId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Hill Giant should take no damage from Deftblade Elite") {
                    giantDamage shouldBe 0
                }
            }

            test("activated ability prevents combat damage when attacking unblocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deftblade Elite")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val eliteId = game.findPermanent("Deftblade Elite")!!

                // Activate the prevention ability
                val cardDef = cardRegistry.getCard("Deftblade Elite")!!
                val preventAbility = cardDef.script.activatedAbilities.first()
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = eliteId,
                        abilityId = preventAbility.id
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Go to combat and attack
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Deftblade Elite" to 2))

                // Wait for the optional provoke trigger
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }
                if (game.hasPendingDecision()) {
                    game.skipTargets()
                }
                game.resolveStack()

                // Advance through blockers (no blockers) and combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 2's life should not change (damage by Deftblade Elite is prevented)
                val player2Life = game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life ?: 0
                withClue("Player 2 should take no damage from Deftblade Elite") {
                    player2Life shouldBe 20
                }
            }

            test("without prevention ability, combat damage is dealt normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deftblade Elite")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(2) // Opponent attacks
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Opponent attacks with Hill Giant
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1))

                // Block with Deftblade Elite (do NOT activate prevention ability)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Deftblade Elite" to listOf("Hill Giant")))

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Deftblade Elite (1/1) should be dead from 3 damage
                withClue("Deftblade Elite should be dead") {
                    game.findPermanent("Deftblade Elite") shouldBe null
                }

                // Hill Giant should have 1 damage from Deftblade Elite
                val giantDamage = game.state.getEntity(hillGiantId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Hill Giant should take 1 damage from Deftblade Elite") {
                    giantDamage shouldBe 1
                }
            }
        }
    }
}
