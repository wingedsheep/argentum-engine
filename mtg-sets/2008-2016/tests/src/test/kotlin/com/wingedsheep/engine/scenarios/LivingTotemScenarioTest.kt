package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Living Totem: when it enters, you may put a +1/+1 counter on another target creature.
 */
class LivingTotemScenarioTest : ScenarioTestBase() {
    init {
        test("its enters trigger may put a +1/+1 counter on another creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Living Totem")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .build()
            game.castSpell(1, "Living Totem").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            val totem = game.findPermanent("Living Totem")!!
            val choose = game.state.pendingDecision as ChooseTargetsDecision
            withClue("the Totem itself is not a legal target") {
                choose.legalTargets.values.flatten().contains(totem) shouldBe false
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("the counter is optional") {
                (game.state.pendingDecision is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(bears) shouldBe 3
        }

        test("declining the may leaves the target without a counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Living Totem")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .build()
            game.castSpell(1, "Living Totem").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            (game.state.pendingDecision is YesNoDecision) shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            (game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        }
    }
}
