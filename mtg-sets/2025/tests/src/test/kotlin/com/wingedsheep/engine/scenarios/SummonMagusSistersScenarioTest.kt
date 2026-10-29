package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Summon: Magus Sisters (FIC #71) — "I, II, III — Choose one at random — • Combine Powers! — Put
 * three +1/+1 counters on target creature. • Defense! — Put a shield counter on target creature. You
 * gain 3 life. • Fight! — This creature fights up to one target creature an opponent controls."
 *
 * The mode is drawn as the chapter ability goes onto the stack, before targets; the controller is
 * only asked for the drawn mode's target.
 */
class SummonMagusSistersScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(name: String, type: CounterType): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(type) ?: 0

    /** Chapter II triggers off the precombat-main lore counter with [seed] pinned. */
    private fun chapterTwo(seed: Long): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Summon: Magus Sisters")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .withRngSeed(seed)
            .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
            .build()
        val saga = game.findPermanent("Summon: Magus Sisters")!!
        game.state = game.state.updateEntity(saga) { container ->
            container.with(CountersComponent().withAdded(CounterType.LORE, 1))
                .with(SagaComponent(triggeredChapters = setOf(1)))
        }
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    init {
        test("each chapter's mode is drawn at random, and the drawn mode is what resolves") {
            val seen = mutableSetOf<Int>()
            for (seed in 1L..30L) {
                val game = chapterTwo(seed)
                withClue("seed $seed: the controller is asked for a target, never a mode") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                val sisters = game.findPermanent("Summon: Magus Sisters")!!
                val target = if (
                    (game.getPendingDecision() as ChooseTargetsDecision).legalTargets[0].orEmpty().contains(sisters)
                ) sisters else bears
                game.selectTargets(listOf(target)).error shouldBe null

                val mode = game.state.stack
                    .mapNotNull { game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
                    .single().chosenModes.single()
                seen += mode

                val lifeBefore = game.getLifeTotal(1)
                game.resolveStack()
                withClue("seed $seed, mode $mode") {
                    when (mode) {
                        0 -> game.counters("Summon: Magus Sisters", CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                        1 -> {
                            game.counters("Summon: Magus Sisters", CounterType.SHIELD) shouldBe 1
                            game.getLifeTotal(1) shouldBe lifeBefore + 3
                        }
                        else -> game.findPermanent("Grizzly Bears") shouldBe null
                    }
                }
            }
            seen shouldContainExactlyInAnyOrder listOf(0, 1, 2)
        }
    }
}
