package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

class TrailblazingHistorianScenarioTest : ScenarioTestBase() {
    init {
        test("taps to give another creature haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Trailblazing Historian")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val hist = game.findPermanent("Trailblazing Historian")!!
            val ability = com.wingedsheep.mtg.sets.definitions.mom.cards.TrailblazingHistorian.activatedAbilities.single().id
            game.execute(com.wingedsheep.engine.core.ActivateAbility(game.player1Id, hist, ability,
                listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears)))).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
        }
    }
}
