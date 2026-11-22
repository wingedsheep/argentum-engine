package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Spark Trooper (GTC) — 6/1 trample, lifelink, haste; sacrificed at the beginning of the end step. */
class SparkTrooperScenarioTest : ScenarioTestBase() {
    init {
        test("attacks the turn it arrives, gains life, then is sacrificed at the end step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Spark Trooper", summoningSickness = true)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Spark Trooper" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 14
            game.getLifeTotal(1) shouldBe 26
            game.isOnBattlefield("Spark Trooper") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Spark Trooper") shouldBe false
            game.isInGraveyard(1, "Spark Trooper") shouldBe true
        }
    }
}
