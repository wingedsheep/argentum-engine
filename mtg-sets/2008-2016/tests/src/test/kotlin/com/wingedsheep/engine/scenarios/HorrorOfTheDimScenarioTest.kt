package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Horror of the Dim (GTC) — {U}: this creature gains hexproof until end of turn. */
class HorrorOfTheDimScenarioTest : ScenarioTestBase() {
    init {
        test("{U} grants itself hexproof") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Horror of the Dim")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val horror = game.findPermanent("Horror of the Dim")!!
            game.state.projectedState.hasKeyword(horror, Keyword.HEXPROOF) shouldBe false

            val abilityId = cardRegistry.getCard("Horror of the Dim")!!.script.activatedAbilities.single().id
            val result = game.execute(ActivateAbility(game.player1Id, horror, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(horror, Keyword.HEXPROOF) shouldBe true
        }
    }
}
