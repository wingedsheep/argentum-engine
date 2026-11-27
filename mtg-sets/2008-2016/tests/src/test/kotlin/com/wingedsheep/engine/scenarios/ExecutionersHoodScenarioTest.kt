package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.ExecutionersHood
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ExecutionersHoodScenarioTest : ScenarioTestBase() {
    init {
        test("equipped creature has intimidate") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Executioner's Hood")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val hood = game.findPermanent("Executioner's Hood")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe false

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = hood,
                    abilityId = ExecutionersHood.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
        }
    }
}
