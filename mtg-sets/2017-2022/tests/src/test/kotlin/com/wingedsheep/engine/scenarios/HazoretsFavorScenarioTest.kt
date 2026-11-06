package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hazoret's Favor (AKH #137) — {2}{R} Enchantment.
 *
 *   At the beginning of combat on your turn, you may have target creature you control get +2/+0
 *   and gain haste until end of turn. If you do, sacrifice it at the beginning of the next end step.
 *
 * Covers the accepted "may" (pump + haste, then the delayed sacrifice at the end step) and the
 * declined one (no pump, and the creature survives the end step — the "If you do" gate).
 */
class HazoretsFavorScenarioTest : ScenarioTestBase() {

    init {
        test("accepting pumps and hastes the creature, then sacrifices it at the beginning of the end step") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!

            resolveBeginCombatTrigger(game, bears, accept = true)

            withClue("+2/+0 and haste until end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("sacrificed at the beginning of the next end step") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("declining leaves the creature unchanged and it is not sacrificed") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!

            resolveBeginCombatTrigger(game, bears, accept = false)

            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hazoret's Favor")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun resolveBeginCombatTrigger(game: TestGame, target: EntityId, accept: Boolean) {
        game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        var answered = false
        var iterations = 0
        while (!answered && iterations++ < 20) {
            when (game.getPendingDecision()) {
                is ChooseTargetsDecision -> game.selectTargets(listOf(target))
                is YesNoDecision -> {
                    game.answerYesNo(accept)
                    answered = true
                }
                else -> game.passPriority()
            }
        }
        withClue("the begin-combat trigger asked whether to give the creature Hazoret's favor") {
            answered shouldBe true
        }
        game.resolveStack()
    }
}
