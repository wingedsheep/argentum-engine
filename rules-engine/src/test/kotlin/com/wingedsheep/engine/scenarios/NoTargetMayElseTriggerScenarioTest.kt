package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for a *no-target* "you may X. If you don't, Y" triggered ability — the latent
 * gap that made Yawgmoth Demon do nothing: the no-target trigger path used to silently drop both
 * `optional` and `elseEffect` (only the targeted path honored them, via target-decline → else).
 *
 * This test pins the general fix with an always-feasible may-action (gain life), so it exercises
 * the lowering into `GatedEffect(Gate.MayDecide, then, otherwise)` independently of the
 * sacrifice-style feasibility skip that Yawgmoth's test covers: the may question is always asked,
 * "yes" runs the body, "no" runs the else branch.
 */
class NoTargetMayElseTriggerScenarioTest : ScenarioTestBase() {

    // "At the beginning of your upkeep, you may gain 2 life. If you don't, you lose 1 life."
    // No target, optional, with an else branch — gaining life is always feasible, so no feasibility
    // skip applies and the may question is always presented.
    private val penitent = card("May-Else Penitent") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        oracleText = "At the beginning of your upkeep, you may gain 2 life. If you don't, you lose 1 life."
        triggeredAbility {
            trigger = Triggers.YourUpkeep
            optional = true
            effect = Effects.GainLife(2)
            elseEffect = Effects.LoseLife(1, EffectTarget.Controller)
        }
    }

    init {
        cardRegistry.register(penitent)

        context("no-target you-may-X / if-you-don't-Y trigger") {

            fun atPlayer1Upkeep(): TestGame {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "May-Else Penitent", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                return game
            }

            test("the may question is always asked even though the body is always feasible") {
                val game = atPlayer1Upkeep()
                withClue("Gaining life can't be infeasible → the player is always prompted") {
                    game.hasPendingDecision() shouldBe true
                }
            }

            test("saying yes runs the body (gain 2 life)") {
                val game = atPlayer1Upkeep()
                game.answerYesNo(true)
                game.resolveStack()
                withClue("Body ran: 20 → 22") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("saying no runs the else branch (lose 1 life)") {
                val game = atPlayer1Upkeep()
                game.answerYesNo(false)
                game.resolveStack()
                withClue("Else branch ran: 20 → 19") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
