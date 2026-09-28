package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EmptyCityRuseScenarioTest : ScenarioTestBase() {
    init {
        context("Empty City Ruse") {
            test("target opponent skips combat on their next turn only") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardInHand(1, "Empty City Ruse")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(1, "Empty City Ruse", 2).error shouldBe null
                game.resolveStack()
                game.state.getEntity(game.player2Id)?.has<SkipCombatPhasesComponent>() shouldBe true
                game.state.getEntity(game.player1Id)?.has<SkipCombatPhasesComponent>() shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getEntity(game.player2Id)?.has<SkipCombatPhasesComponent>() shouldBe false
            }
        }
    }
}
