package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Troll of Khazad-dûm — "can't be blocked except by three or more creatures." Exercises the new
 * `CantBeBlockedByFewerThan(3)` static (a generalization of menace's minimum of two).
 */
class TrollOfKhazadDumScenarioTest : ScenarioTestBase() {

    private fun trollCombat() = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Troll of Khazad-dûm")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Goblin Guide")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        test("cannot be blocked by fewer than three creatures") {
            val game = trollCombat()
            game.declareAttackers(mapOf("Troll of Khazad-dûm" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            // Two blockers is illegal (validation fails, state unchanged).
            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Troll of Khazad-dûm"),
                    "Grizzly Bears" to listOf("Troll of Khazad-dûm"),
                )
            ).error shouldNotBe null

            // Three blockers is legal.
            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Troll of Khazad-dûm"),
                    "Grizzly Bears" to listOf("Troll of Khazad-dûm"),
                    "Goblin Guide" to listOf("Troll of Khazad-dûm"),
                )
            ).error shouldBe null
        }

        test("may still be left unblocked") {
            val game = trollCombat()
            game.declareAttackers(mapOf("Troll of Khazad-dûm" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
        }
    }
}
