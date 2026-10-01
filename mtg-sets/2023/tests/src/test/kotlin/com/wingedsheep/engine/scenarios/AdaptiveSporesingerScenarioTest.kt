package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
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

/**
 * Adaptive Sporesinger (ONE #157) — {2}{G} 2/2 Creature — Phyrexian Druid.
 *
 *   Vigilance
 *   When this creature enters, choose one —
 *   • Target creature gets +2/+2 and gains vigilance until end of turn.
 *   • Proliferate.
 *
 * Casts the Sporesinger so its enters trigger really fires, then exercises each mode.
 */
class AdaptiveSporesingerScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Adaptive Sporesinger")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast the Sporesinger, resolve it, and pick the mode whose label contains [needle]. */
    private fun castAndChoose(game: TestGame, needle: String) {
        game.castSpell(1, "Adaptive Sporesinger").error shouldBe null
        var guard = 0
        while (game.state.pendingDecision !is ChooseOptionDecision && guard++ < 10) game.resolveStack()
        val modal = game.state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
        val index = modal.options.indexOfFirst { it.contains(needle, ignoreCase = true) }
        check(index >= 0) { "no mode matching '$needle' in ${modal.options}" }
        game.submitDecision(OptionChosenResponse(modal.id, index)).error shouldBe null
    }

    init {
        test("Sporesinger has vigilance") {
            val game = board()
            game.castSpell(1, "Adaptive Sporesinger").error shouldBe null
            game.resolveStack()
            val singer = game.findPermanent("Adaptive Sporesinger")!!
            game.state.projectedState.hasKeyword(singer, Keyword.VIGILANCE) shouldBe true
        }

        test("mode 1 gives target creature +2/+2 and vigilance until end of turn") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            castAndChoose(game, "+2/+2")
            game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Bears are pumped to 4/4") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
            withClue("Bears gained vigilance") {
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("The pump wears off at end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
            }
        }

        test("mode 2 proliferates") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)

            castAndChoose(game, "Proliferate")
            var prompts = 0
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) {
                    game.selectCards(listOf(bears)).error shouldBe null
                    prompts++
                } else {
                    game.resolveStack()
                }
            }

            prompts shouldBe 1
            counters(game, bears) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 4
        }
    }
}
