package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Zarichi Tiger (GTC) — {1}{W}, {T}: You gain 2 life. */
class ZarichiTigerScenarioTest : ScenarioTestBase() {
    init {
        test("{1}{W}, tap: gain 2 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zarichi Tiger")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tiger = game.findPermanent("Zarichi Tiger")!!
            val abilityId = cardRegistry.getCard("Zarichi Tiger")!!.script.activatedAbilities.single().id
            val before = game.getLifeTotal(1)

            val result = game.execute(ActivateAbility(game.player1Id, tiger, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe before + 2
        }
    }
}
