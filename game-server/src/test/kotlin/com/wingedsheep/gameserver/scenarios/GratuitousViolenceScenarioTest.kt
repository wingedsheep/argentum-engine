package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gratuitous Violence.
 *
 * Card reference:
 * - Gratuitous Violence ({2}{R}{R}{R}): Enchantment
 *   "If a creature you control would deal damage to a permanent or player,
 *    it deals double that damage instead."
 */
class GratuitousViolenceScenarioTest : ScenarioTestBase() {

    init {
        context("Gratuitous Violence - damage doubling") {
            test("doubles combat damage dealt by your creature to opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gratuitous Violence")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should take doubled combat damage (2 * 2 = 4)") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
            }

            test("does not double damage dealt by opponent's creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gratuitous Violence")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 opponent's creature
                    .withActivePlayer(2) // opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent's creature damage should NOT be doubled") {
                    game.getLifeTotal(1) shouldBe startingLife - 2
                }
            }

            test("doubles combat damage dealt to blocking creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gratuitous Violence")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Towering Baloth" to listOf("Glory Seeker")))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val creatureId = game.findPermanent("Towering Baloth")!!
                val damage = game.state.getEntity(creatureId)?.get<DamageComponent>()?.amount ?: 0

                withClue("Towering Baloth should take doubled damage (2 * 2 = 4)") {
                    damage shouldBe 4
                }
            }

            test("does not double non-creature damage (e.g., Shock)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gratuitous Violence")
                    .withCardInHand(1, "Shock") // instant: deal 2 damage
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)

                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                withClue("Shock is not a creature source, so damage should not be doubled") {
                    game.getLifeTotal(2) shouldBe startingLife - 2
                }
            }
        }
    }
}
