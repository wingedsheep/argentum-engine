package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class BrothersOfFireScenarioTest : ScenarioTestBase() {
    init {
        test("the activated ability damages its target and its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Brothers of Fire")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val brothers = game.findPermanent("Brothers of Fire")!!
            val abilityId = cardRegistry.getCard("Brothers of Fire")!!.script.activatedAbilities.single().id
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = brothers,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)),
                )
            )

            result.error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 19
        }
    }
}
