package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Plague Mare (M19 #114) — can't be blocked by white creatures; on entering, creatures your
 * opponents control get -1/-1 until end of turn.
 */
class PlagueMareScenarioTest : ScenarioTestBase() {
    init {
        test("entering shrinks only the opponent's creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Plague Mare")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Plague Mare").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Raging Goblin") shouldBe true
            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.state.projectedState.getPower(game.findPermanent("Hill Giant")!!) shouldBe 2
            game.state.projectedState.getToughness(game.findPermanent("Hill Giant")!!) shouldBe 2
        }

        test("a white creature can't block it, a nonwhite one can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Plague Mare")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Plague Mare" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Plague Mare"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Plague Mare"))).error shouldBe null
        }
    }
}
