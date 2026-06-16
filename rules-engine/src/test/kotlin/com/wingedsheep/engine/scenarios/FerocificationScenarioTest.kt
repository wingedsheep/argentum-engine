package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ferocification (OTJ #123).
 *
 * {2}{R} · Enchantment
 * "At the beginning of combat on your turn, choose one —
 *  • Target creature you control gets +2/+0 until end of turn.
 *  • Target creature you control gains menace and haste until end of turn."
 *
 * Exercises the modal beginning-of-combat trigger ([com.wingedsheep.sdk.dsl.Triggers.BeginCombat])
 * with per-mode targets: each mode picks "target creature you control" and applies an existing
 * effect. The trigger only fires on the controller's turn.
 */
class FerocificationScenarioTest : ScenarioTestBase() {

    init {
        test("mode 1 gives the chosen creature +2/+0 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ferocification")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            // Advance to beginning of combat; the trigger goes on the stack, then resolve it.
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision for the begin-combat trigger; got ${game.state.pendingDecision}")
            game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

            val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision after mode pick; got ${game.state.pendingDecision}")
            game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(bears))))
            game.resolveStack()

            withClue("Grizzly Bears should be 4/2 after +2/+0") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }

        test("mode 2 grants the chosen creature menace and haste until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ferocification")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision for the begin-combat trigger; got ${game.state.pendingDecision}")
            game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 1))

            val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision after mode pick; got ${game.state.pendingDecision}")
            game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(bears))))
            game.resolveStack()

            withClue("Grizzly Bears should have gained menace") {
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
            }
            withClue("Grizzly Bears should have gained haste") {
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }
            withClue("Mode 2 does not change power/toughness") {
                game.state.projectedState.getPower(bears) shouldBe 2
            }
        }

        test("the trigger does not fire on the opponent's combat") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ferocification")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2) // opponent's turn
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            withClue("Ferocification only triggers on its controller's combat — no decision on the opponent's turn") {
                (game.state.pendingDecision is ChooseOptionDecision) shouldBe false
            }
        }
    }
}
