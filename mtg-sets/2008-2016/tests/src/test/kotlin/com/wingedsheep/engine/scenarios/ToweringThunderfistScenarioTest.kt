package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Towering Thunderfist (GTC) — {W}: This creature gains vigilance until end of turn. */
class ToweringThunderfistScenarioTest : ScenarioTestBase() {
    init {
        test("{W} grants itself vigilance") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Towering Thunderfist")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Towering Thunderfist")!!
            val abilityId = cardRegistry.getCard("Towering Thunderfist")!!.script.activatedAbilities.single().id

            game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe false
            val result = game.execute(ActivateAbility(game.player1Id, giant, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe true
        }
    }
}
