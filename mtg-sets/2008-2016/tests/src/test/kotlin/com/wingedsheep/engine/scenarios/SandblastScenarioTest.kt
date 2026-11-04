package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sandblast (FRF) — 5 damage to target attacking or blocking creature.
 */
class SandblastScenarioTest : ScenarioTestBase() {
    init {
        test("deals 5 damage to an attacking creature") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Sandblast")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(2).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()

            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passPriority().error shouldBe null // active player passes; defender gets priority

            game.castSpell(1, "Sandblast", game.findPermanent("Hill Giant")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("can't target a creature that isn't attacking or blocking") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Sandblast")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Sandblast", game.findPermanent("Hill Giant")).error shouldNotBe null
        }
    }
}
