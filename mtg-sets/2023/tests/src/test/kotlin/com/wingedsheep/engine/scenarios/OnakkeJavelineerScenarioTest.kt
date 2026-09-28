package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class OnakkeJavelineerScenarioTest : ScenarioTestBase() {
    init {
        context("Onakke Javelineer") { test("tap: deals 2 damage to target player") {
            val game = scenario()
                .withPlayers("Owner", "Victim")
                .withCardOnBattlefield(1, "Onakke Javelineer", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jav = game.findPermanent("Onakke Javelineer")!!
            val abilityId = cardRegistry.getCard("Onakke Javelineer")!!.script.activatedAbilities[0].id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = jav,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, game.player2Id))
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        } }
    }
}
