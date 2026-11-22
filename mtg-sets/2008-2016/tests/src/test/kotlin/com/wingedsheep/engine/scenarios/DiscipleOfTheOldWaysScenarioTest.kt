package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Disciple of the Old Ways (GTC) — {R}: gains first strike until end of turn. */
class DiscipleOfTheOldWaysScenarioTest : ScenarioTestBase() {
    init {
        test("{R} gives it first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Disciple of the Old Ways")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val disciple = game.findPermanent("Disciple of the Old Ways")!!
            val abilityId = cardRegistry.getCard("Disciple of the Old Ways")!!.script.activatedAbilities.single().id
            game.state.projectedState.hasKeyword(disciple, Keyword.FIRST_STRIKE) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, disciple, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(disciple, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
