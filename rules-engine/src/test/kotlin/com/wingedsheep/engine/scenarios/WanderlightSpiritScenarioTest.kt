package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Wanderlight Spirit (VOW #86) — {2}{U} Creature — Spirit, 2/3, flying.
 *
 *   This creature can block only creatures with flying.
 *
 * Exercises the [com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith] restriction: it may
 * legally block a flying attacker (Birds of Paradise), but not a non-flying attacker (Hill Giant).
 */
class WanderlightSpiritScenarioTest : ScenarioTestBase() {

    init {
        context("Wanderlight Spirit can only block creatures with flying") {

            test("is a 2/3 flyer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wanderlight Spirit")
                    .build()

                val spirit = game.findPermanent("Wanderlight Spirit")!!
                game.state.projectedState.getPower(spirit) shouldBe 2
                game.state.projectedState.getToughness(spirit) shouldBe 3
                game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
            }

            test("can legally block a flying attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wanderlight Spirit", summoningSickness = false)
                    .withCardOnBattlefield(2, "Birds of Paradise", summoningSickness = false) // flying
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Birds of Paradise" to 1)).error shouldBe null

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val result = game.declareBlockers(mapOf("Wanderlight Spirit" to listOf("Birds of Paradise")))
                withClue("blocking a flying attacker is legal: ${result.error}") {
                    result.error shouldBe null
                }
            }

            test("cannot legally block a non-flying attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wanderlight Spirit", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // no flying
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val result = game.declareBlockers(mapOf("Wanderlight Spirit" to listOf("Hill Giant")))
                withClue("blocking a non-flying attacker is illegal: ${result.error}") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
