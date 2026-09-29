package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Protocol Knight (MOM #74): ETB taps target creature an opponent controls, and stuns it only if
 * you control another Knight.
 */
class ProtocolKnightScenarioTest : ScenarioTestBase() {
    init {
        fun run(withOtherKnight: Boolean): Int? {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Protocol Knight")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (withOtherKnight) builder.withCardOnBattlefield(1, "Protocol Knight")
            val game = builder.build()

            game.castSpell(1, "Protocol Knight").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!))
            game.resolveStack()

            val giant = game.state.getEntity(game.findPermanent("Hill Giant")!!)!!
            (giant.get<TappedComponent>() != null) shouldBe true
            return giant.get<CountersComponent>()?.getCount(CounterType.STUN)
        }

        test("without another Knight, taps the target but adds no stun counter") {
            (run(withOtherKnight = false) ?: 0) shouldBe 0
        }

        test("with another Knight, taps the target and adds a stun counter") {
            run(withOtherKnight = true) shouldBe 1
        }
    }
}
