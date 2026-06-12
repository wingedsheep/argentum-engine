package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Witch-king, Bringer of Ruin — "Whenever Witch-king attacks, defending player sacrifices a
 * creature with the least power among creatures they control." Exercises the new
 * `StatePredicate.HasLeastPower` filter: only the defending player's lowest-power creature is a
 * legal sacrifice, so with a unique minimum the edict auto-sacrifices it.
 */
class WitchKingBringerOfRuinScenarioTest : ScenarioTestBase() {

    init {
        test("on attack, defending player sacrifices their least-power creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Witch-king, Bringer of Ruin")  // 5/3 flying
                .withCardOnBattlefield(2, "Savannah Lions")                // 1/1 — least power
                .withCardOnBattlefield(2, "Centaur Courser")               // 2/3
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Witch-king, Bringer of Ruin" to 2))
            // The attack trigger goes on the stack; resolving it runs the edict. Only the 1/1 has
            // the least power, so it's the sole legal sacrifice and is taken automatically.
            game.resolveStack()

            game.isOnBattlefield("Savannah Lions") shouldBe false
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }
    }
}
