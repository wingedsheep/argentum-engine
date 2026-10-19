package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invoke the Ancients (NEO #193) — {1}{G}{G}{G}{G} Sorcery
 *
 *   Create two 4/5 green Spirit creature tokens. For each of them, put your choice of a reach
 *   counter, a vigilance counter, or a trample counter on it.
 *
 * Proves the counter is chosen separately for each token, and that a token doubler (Parallel
 * Lives) asks once per resulting token (2022-02-18 ruling).
 */
class InvokeTheAncientsScenarioTest : ScenarioTestBase() {
    init {
        fun setup(vararg extra: String) = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Invoke the Ancients")
            .withLandsOnBattlefield(1, "Forest", 5)
            .apply { extra.forEach { withCardOnBattlefield(1, it) } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.counter(id: EntityId, type: CounterType): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

        fun TestGame.choose(index: Int) {
            val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.options.size shouldBe 3
            submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
        }

        test("each token gets its own chosen keyword counter") {
            val game = setup()
            game.castSpell(1, "Invoke the Ancients").error shouldBe null
            game.resolveStack()

            game.choose(0) // reach
            game.choose(2) // trample
            withClue("no further choices") { game.state.pendingDecision shouldBe null }

            val spirits = game.findPermanents("Spirit Token")
            spirits.size shouldBe 2
            spirits.forEach {
                game.state.projectedState.getPower(it) shouldBe 4
                game.state.projectedState.getToughness(it) shouldBe 5
            }
            val reach = spirits.single { game.counter(it, CounterType.REACH) == 1 }
            val trample = spirits.single { game.counter(it, CounterType.TRAMPLE) == 1 }
            (reach != trample) shouldBe true
            spirits.sumOf { game.counter(it, CounterType.VIGILANCE) } shouldBe 0
            game.state.projectedState.hasKeyword(reach, Keyword.REACH) shouldBe true
            game.state.projectedState.hasKeyword(trample, Keyword.TRAMPLE) shouldBe true
        }

        test("with Parallel Lives, a choice is made for each of the four tokens") {
            val game = setup("Parallel Lives")
            game.castSpell(1, "Invoke the Ancients").error shouldBe null
            game.resolveStack()

            repeat(4) { game.choose(1) } // vigilance each time
            game.state.pendingDecision shouldBe null

            val spirits = game.findPermanents("Spirit Token")
            spirits.size shouldBe 4
            spirits.forEach {
                game.counter(it, CounterType.VIGILANCE) shouldBe 1
                game.state.projectedState.hasKeyword(it, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
