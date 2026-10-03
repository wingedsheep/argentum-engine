package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tune the Narrative (MH3) — "Draw a card. You get {E}{E} (two energy counters)."
 */
class TuneTheNarrativeScenarioTest : ScenarioTestBase() {

    init {
        test("draws a card and gives its controller two energy counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Tune the Narrative")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.castSpell(1, "Tune the Narrative").error shouldBe null
            game.resolveStack()

            // Cast one card from hand, drew one.
            game.handSize(1) shouldBe handBefore
            game.state.getEntity(game.player1Id)?.get<CountersComponent>()
                ?.getCount(CounterType.ENERGY) shouldBe 2
            (game.state.getEntity(game.player2Id)?.get<CountersComponent>()
                ?.getCount(CounterType.ENERGY) ?: 0) shouldBe 0
        }
    }
}
