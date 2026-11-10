package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Steppe Glider (OGW) — {4}{W} 2/4 Elemental.
 * "Flying, vigilance. {1}{W}: Target creature with a +1/+1 counter on it gains flying and
 * vigilance until end of turn."
 */
class SteppeGliderScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Steppe Glider")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Steppe Glider")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.addPlusOneCounter(id: EntityId) {
            state = state.updateEntity(id) { container ->
                container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        }

        test("has flying and vigilance itself") {
            val game = board()
            val glider = game.findPermanent("Steppe Glider")!!
            game.state.projectedState.hasKeyword(glider, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(glider, Keyword.VIGILANCE) shouldBe true
        }

        test("a creature with a +1/+1 counter gains flying and vigilance") {
            val game = board()
            val glider = game.findPermanent("Steppe Glider")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.addPlusOneCounter(bears)

            val result = game.execute(
                ActivateAbility(game.player1Id, glider, abilityId, listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }

        test("a creature without a +1/+1 counter can't be targeted") {
            val game = board()
            val glider = game.findPermanent("Steppe Glider")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, glider, abilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
        }
    }
}
