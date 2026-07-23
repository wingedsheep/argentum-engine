package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invisibility — "Enchanted creature can't be blocked except by Walls."
 */
class InvisibilityScenarioTest : ScenarioTestBase() {

    init {
        context("enchanted creature can only be blocked by Walls") {

            test("a non-Wall creature cannot block it") {
                val game = combatScenario("Grizzly Bears")

                val result = game.declareBlockers(
                    mapOf("Grizzly Bears" to listOf("Hill Giant"))
                )

                withClue("a non-Wall must not be allowed to block a creature enchanted by Invisibility") {
                    result.error shouldNotBe null
                }
            }

            test("a Wall creature can block it") {
                val game = combatScenario("Wall of Mulch")

                val result = game.declareBlockers(
                    mapOf("Wall of Mulch" to listOf("Hill Giant"))
                )

                withClue("a Wall must be allowed to block a creature enchanted by Invisibility") {
                    result.error shouldBe null
                }
            }
        }
    }

    private fun combatScenario(blockerName: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardAttachedTo(1, "Invisibility", "Hill Giant")
        .withCardOnBattlefield(2, blockerName, summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()
        .also { game ->
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }
}
