package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Within Range (TDC #32) — "Whenever you attack, each opponent loses life equal to the number of
 * creatures attacking them."
 *
 * Pins the per-opponent rebinding: inside `ForEachPlayer(EachOpponent)` the attacker count is
 * scoped to creatures attacking the visited opponent, and the loss lands on that opponent, not on
 * Within Range's controller. The trigger resolves before combat damage, so Bob's life reflects
 * only the life loss.
 */
class WithinRangeScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Within Range")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        test("each opponent loses life equal to the number of creatures attacking them") {
            val game = setup()
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("a single attacker costs the opponent one life") {
            val game = setup()
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
