package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kumano's Pupils (CHK #177) — "If a creature dealt damage by this creature this turn would die,
 * exile it instead."
 *
 * Combat damage is the case a damage-time trigger could never handle, and the ruling that the
 * replacement still applies when Kumano's Pupils dies at the same time is the SBA-batch case.
 */
class KumanosPupilsScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kumano's Pupils")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.attackAndGetBlockedBy(blocker: String) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Kumano's Pupils" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            declareBlockers(mapOf(blocker to listOf("Kumano's Pupils"))).error shouldBe null
            passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        }

        context("Kumano's Pupils") {
            test("a blocker it kills in combat is exiled") {
                val game = base().withCardOnBattlefield(2, "Grizzly Bears").build()

                game.attackAndGetBlockedBy("Grizzly Bears")

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Kumano's Pupils") shouldBe true
            }

            test("still exiles a creature that dies simultaneously with Kumano's Pupils") {
                val game = base().withCardOnBattlefield(2, "Hill Giant").build()

                game.attackAndGetBlockedBy("Hill Giant")

                withClue("both 3/3s die in the same state-based action check") {
                    game.isInGraveyard(1, "Kumano's Pupils") shouldBe true
                }
                game.isInExile(2, "Hill Giant") shouldBe true
            }

            test("a creature it didn't damage dies normally") {
                val game = base().withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
