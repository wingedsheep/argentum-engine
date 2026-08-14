package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario coverage for Rangers' Aetherhive's exhaust-activation trigger. */
class RangersAetherhiveScenarioTest : ScenarioTestBase() {
    init {
        test("activating an exhaust ability creates a flying Thopter") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Rangers' Aetherhive")
                .withCardOnBattlefield(1, "Prowcatcher Specialist", summoningSickness = false)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val specialist = game.findPermanent("Prowcatcher Specialist")!!
            val exhaust = cardRegistry.getCard("Prowcatcher Specialist")!!
                .script.activatedAbilities.single { it.isExhaust }

            val activation = game.execute(ActivateAbility(game.player1Id, specialist, exhaust.id))
            withClue("the exhaust activation should be legal: ${activation.error}") {
                activation.error shouldBe null
            }
            game.resolveStack()

            game.findPermanents("Thopter Token").size shouldBe 1
        }
    }
}
