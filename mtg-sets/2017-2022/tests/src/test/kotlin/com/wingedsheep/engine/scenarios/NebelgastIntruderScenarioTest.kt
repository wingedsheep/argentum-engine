package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nebelgast Intruder (MID #64) — Flash, Flying; "When this creature enters, up to one target
 * creature an opponent controls gets -2/-0 until end of turn."
 */
class NebelgastIntruderScenarioTest : ScenarioTestBase() {

    init {
        test("flashed in on the opponent's turn, it gives an opposing creature -2/-0 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nebelgast Intruder")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Nebelgast Intruder").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Nebelgast Intruder") shouldBe true
            game.state.projectedState.getPower(giant) shouldBe 1
            game.state.projectedState.getToughness(giant) shouldBe 3
        }

        test("the target is optional — it can enter with no target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nebelgast Intruder")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nebelgast Intruder").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.skipTargets().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Nebelgast Intruder") shouldBe true
            game.state.projectedState.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
        }
    }
}
