package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Voracious Wurm — "This creature enters with X +1/+1 counters on it, where X is the amount of life
 * you've gained this turn."
 *
 * The count is read as the Wurm enters (a replacement effect), from its controller's life gained this
 * turn — the amount gained, not the net life change.
 */
class VoraciousWurmScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Voracious Wurm")
        .withCardInHand(1, "Renewed Faith")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Plains", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enters with no counters when you've gained no life this turn") {
            val g = board()
            g.castSpell(1, "Voracious Wurm").error shouldBe null
            g.resolveStack()
            g.plusOnes(g.findPermanent("Voracious Wurm")!!) shouldBe 0
        }

        test("enters with a +1/+1 counter for each life you gained this turn") {
            val g = board()
            g.castSpell(1, "Renewed Faith").error shouldBe null
            g.resolveStack()
            g.castSpell(1, "Voracious Wurm").error shouldBe null
            g.resolveStack()
            g.plusOnes(g.findPermanent("Voracious Wurm")!!) shouldBe 6
        }
    }
}
