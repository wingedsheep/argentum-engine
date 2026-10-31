package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Steadfast Sentry (M20 #40) — {2}{W} 3/2 Creature — Human Soldier, vigilance.
 *
 *   When this creature dies, put a +1/+1 counter on target creature you control.
 */
class SteadfastSentryScenarioTest : ScenarioTestBase() {
    /** Answers the trigger's target prompt if the engine asks for one. */
    private fun TestGame.chooseTarget(target: EntityId) {
        if (state.pendingDecision is ChooseTargetsDecision) selectTargets(listOf(target)).error shouldBe null
    }

    init {
        context("Steadfast Sentry") {
            test("has vigilance, and dying puts a +1/+1 counter on a creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Steadfast Sentry")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Steadfast Sentry")!!
                game.state.projectedState.hasKeyword(sentry, Keyword.VIGILANCE) shouldBe true

                game.castSpell(1, "Lightning Bolt", sentry).error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.chooseTarget(bears)
                game.resolveStack()

                game.isInGraveyard(1, "Steadfast Sentry") shouldBe true
                game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 3
            }
        }
    }
}
