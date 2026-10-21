package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Underdark Basilisk (AFR #208) — "Deathtouch".
 */
class UnderdarkBasiliskScenarioTest : ScenarioTestBase() {

    init {
        test("blocking with deathtouch destroys a bigger attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Underdark Basilisk")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Underdark Basilisk" to listOf("Hill Giant"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Underdark Basilisk") shouldBe true
        }
    }
}
