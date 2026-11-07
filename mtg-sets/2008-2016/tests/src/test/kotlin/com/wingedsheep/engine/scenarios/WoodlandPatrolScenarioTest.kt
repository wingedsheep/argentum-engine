package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Woodland Patrol (EMN #180) — {2}{G} Creature — Human Scout, 3/2.
 *
 *   Vigilance
 */
class WoodlandPatrolScenarioTest : ScenarioTestBase() {

    init {
        test("a 3/2 that attacks without tapping") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Woodland Patrol")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val patrol = game.findPermanent("Woodland Patrol")!!
            game.state.projectedState.getPower(patrol) shouldBe 3
            game.state.projectedState.getToughness(patrol) shouldBe 2
            game.state.projectedState.hasKeyword(patrol, Keyword.VIGILANCE) shouldBe true

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Woodland Patrol" to 2)).error shouldBe null

            withClue("vigilance: attacking doesn't tap it") {
                game.state.getEntity(patrol)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
