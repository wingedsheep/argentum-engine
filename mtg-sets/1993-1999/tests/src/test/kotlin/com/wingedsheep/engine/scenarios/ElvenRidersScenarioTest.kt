package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elven Riders — "This creature can't be blocked except by Walls and/or creatures with flying."
 *
 * Covers both branches of the union and the Scryfall ruling that reach alone is not enough.
 */
class ElvenRidersScenarioTest : ScenarioTestBase() {

    init {
        context("Elven Riders can only be blocked by Walls and/or flyers") {

            test("a Wall can block it") {
                val result = blockWith("Wall of Stone")
                withClue("a Wall must be allowed to block Elven Riders") {
                    result.error shouldBe null
                }
            }

            test("a creature with flying can block it") {
                val result = blockWith("Wind Drake")
                withClue("a flyer must be allowed to block Elven Riders") {
                    result.error shouldBe null
                }
            }

            test("a creature with reach but no flying cannot block it") {
                val result = blockWith("Giant Spider")
                withClue("reach is not flying, so Giant Spider must not block Elven Riders") {
                    result.error shouldNotBe null
                }
            }

            test("an ordinary ground creature cannot block it") {
                val result = blockWith("Grizzly Bears")
                withClue("a non-Wall without flying must not block Elven Riders") {
                    result.error shouldNotBe null
                }
            }
        }
    }

    private fun blockWith(blockerName: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Elven Riders", summoningSickness = false)
        .withCardOnBattlefield(2, blockerName, summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()
        .let { game ->
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Elven Riders" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf(blockerName to listOf("Elven Riders")))
        }
}
