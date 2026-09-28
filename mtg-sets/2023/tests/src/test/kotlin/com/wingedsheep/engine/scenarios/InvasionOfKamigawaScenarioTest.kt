package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Invasion of Kamigawa // Rooftop Saboteurs (MOM #62).
 *
 * Front: ETB taps target artifact or creature an opponent controls and puts a stun counter on it.
 * Back: flying, draws on combat damage to a player or battle.
 */
class InvasionOfKamigawaScenarioTest : ScenarioTestBase() {
    init {
        test("ETB taps the target creature and puts a stun counter on it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Kamigawa")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Kamigawa").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!))
            game.resolveStack()

            val giant = game.state.getEntity(game.findPermanent("Hill Giant")!!)!!
            (giant.get<TappedComponent>() != null) shouldBe true
            giant.get<CountersComponent>()?.getCount(CounterType.STUN) shouldBe 1
        }
    }
}
