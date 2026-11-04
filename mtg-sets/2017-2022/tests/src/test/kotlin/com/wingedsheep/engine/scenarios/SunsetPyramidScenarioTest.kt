package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.SunsetPyramid
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sunset Pyramid (HOU) — enters with three brick counters; {2}, {T}, remove a brick counter:
 * draw a card; {2}, {T}: scry 1.
 */
class SunsetPyramidScenarioTest : ScenarioTestBase() {
    private val drawId = SunsetPyramid.activatedAbilities[0].id

    init {
        test("enters with three brick counters; the draw ability removes one") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Sunset Pyramid")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Sunset Pyramid").error shouldBe null
            game.resolveStack()
            val pyramid = game.findPermanent("Sunset Pyramid")!!
            fun bricks() = game.state.getEntity(pyramid)?.get<CountersComponent>()?.getCount(CounterType.BRICK) ?: 0
            bricks() shouldBe 3

            game.execute(ActivateAbility(game.player1Id, pyramid, drawId)).error shouldBe null
            bricks() shouldBe 2
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }
    }
}
