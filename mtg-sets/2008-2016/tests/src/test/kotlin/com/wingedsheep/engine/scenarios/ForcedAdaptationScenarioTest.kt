package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Forced Adaptation (GTC) — at the beginning of your upkeep, put a +1/+1 counter on enchanted
 * creature. Starts in the opponent's end step so the next step reached is the Aura controller's
 * upkeep.
 */
class ForcedAdaptationScenarioTest : ScenarioTestBase() {
    init {
        test("puts a +1/+1 counter on the enchanted creature at its controller's upkeep") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Forced Adaptation", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }
    }
}
