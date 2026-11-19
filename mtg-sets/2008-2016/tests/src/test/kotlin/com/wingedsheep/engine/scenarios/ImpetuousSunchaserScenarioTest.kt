package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Impetuous Sunchaser (BNG #99) — "Flying, haste. This creature attacks each combat if able."
 */
class ImpetuousSunchaserScenarioTest : ScenarioTestBase() {
    init {
        test("with haste it must attack the turn it enters") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Impetuous Sunchaser", summoningSickness = true)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            withClue("declaring no attackers is refused") {
                game.declareAttackers(emptyMap()).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Impetuous Sunchaser" to 2)).error shouldBe null
        }
    }
}
