package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Honden of Seeing Winds (CHK #69) — "At the beginning of your upkeep, draw a card for each Shrine you control."
 *
 * The count is Shrines *you* control, the Honden included: a second Shrine of ours doubles it, and
 * an opponent's Shrine adds nothing. The Spirit Oasis is the extra Shrine because it only has
 * enters triggers, so it adds no upkeep trigger of its own.
 */
class HondenOfSeeingWindsScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveUpkeep(targetId: EntityId? = null) {
        var guard = 0
        while (guard++ < 20) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(targetId!!))
                is SelectCardsDecision -> selectCards(decision.options.take(decision.minSelections))
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board(extraShrine: Boolean) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Honden of Seeing Winds")
        .apply { if (extraShrine) withCardOnBattlefield(1, "The Spirit Oasis") }
        .withCardOnBattlefield(2, "The Spirit Oasis")
        .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.BEGINNING, Step.UNTAP)
        .build()

    init {
        context("Honden of Seeing Winds") {
            test("alone, it counts only itself") {
                val game = board(extraShrine = false)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep(null)
                game.handSize(1) shouldBe 1
            }

            test("a second Shrine you control doubles it; the opponent's Shrine does not count") {
                val game = board(extraShrine = true)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep(null)
                game.handSize(1) shouldBe 2
            }
        }
    }
}
