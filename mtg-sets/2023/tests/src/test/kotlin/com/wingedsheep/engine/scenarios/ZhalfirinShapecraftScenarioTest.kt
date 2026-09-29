package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Zhalfirin Shapecraft — target creature has base P/T 4/3 until end of turn; draw a card. */
class ZhalfirinShapecraftScenarioTest : ScenarioTestBase() {
    init {
        test("sets the target's base power and toughness to 4/3, keeps pumps on top, and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Zhalfirin Shapecraft")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val counters = (game.state.getEntity(bears)?.get<CountersComponent>() ?: CountersComponent())
                .withCounters(CounterType.PLUS_ONE_PLUS_ONE, 1)
            game.state = game.state.updateEntity(bears) { it.with(counters) }
            val handBefore = game.handSize(1)

            game.castSpell(1, "Zhalfirin Shapecraft", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.handSize(1) shouldBe handBefore // cast one, drew one

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }
    }
}
