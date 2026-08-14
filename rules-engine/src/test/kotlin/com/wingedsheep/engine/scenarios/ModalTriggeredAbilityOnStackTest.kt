package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A modal *triggered* ability announces its mode as the ability is put onto the stack
 * (CR 603.3c / 700.2b), and CR 603.3d sends its per-mode targets the same way (via 601.2c–d) — none
 * of it waits for resolution.
 *
 * That ordering is the point: by the time anybody holds priority the ability is already on the stack
 * with `chosenModes` filled in, so an opponent decides whether to respond knowing *which* mode they
 * are responding to. Picking the mode at resolution instead would show them an opaque trigger and
 * hand them the mode only once it was too late to act on it.
 *
 * Per-card behaviour lives in each card's own scenario test (Bumi, King of Three Trials for the
 * "choose up to X" runtime cap; Breeches, Eager Pillager for "…that hasn't been chosen this turn";
 * Silent Hallcreeper for a mode that can't be chosen for want of a legal target). This file pins the
 * shared guarantee, using Lord Skitter's Butcher — three modes, none of which targets, so nothing
 * but the mode question itself is in play.
 */
class ModalTriggeredAbilityOnStackTest : ScenarioTestBase() {

    /** The triggered abilities currently on the stack, innermost component only. */
    private fun TestGame.triggeredAbilitiesOnStack(): List<TriggeredAbilityOnStackComponent> =
        state.stack.mapNotNull { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }

    /** Cast the Butcher and stop on its ETB mode question. */
    private fun butcherAtModeQuestion(): Pair<TestGame, ChooseOptionDecision> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Lord Skitter's Butcher")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Lord Skitter's Butcher").error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()

        val decision = game.getPendingDecision() as? ChooseOptionDecision
            ?: error("expected the ETB mode question; got ${game.getPendingDecision()}")
        return game to decision
    }

    init {
        context("a modal triggered ability's mode is settled on the way to the stack") {

            test("the mode question is not a resolution-time question") {
                val (game, decision) = butcherAtModeQuestion()

                withClue("the ability is on its way to the stack, so the decision says so") {
                    decision.context.phase shouldBe DecisionPhase.TRIGGER
                }
                withClue("and it is genuinely not on the stack yet — nothing to respond to") {
                    game.triggeredAbilitiesOnStack().shouldBeEmpty()
                }
            }

            test("answering it puts the ability on the stack with the mode baked in, unresolved") {
                val (game, decision) = butcherAtModeQuestion()
                val ratMode = decision.options.indexOfFirst { it.contains("Rat", ignoreCase = true) }
                check(ratMode >= 0) { "Rat token mode not offered; options=${decision.options}" }

                game.submitDecision(OptionChosenResponse(decision.id, optionIndex = ratMode))

                val onStack = game.triggeredAbilitiesOnStack().single()
                withClue("the chosen mode rides on the stack object (CR 603.3c)") {
                    onStack.chosenModes shouldBe listOf(ratMode)
                }
                withClue("nothing has resolved — both players still get priority first") {
                    game.findAllPermanents("Rat Token").shouldBeEmpty()
                }

                game.resolveStack()
                withClue("and the chosen mode is what resolves") {
                    game.findAllPermanents("Rat Token").size shouldBe 1
                }
            }

            test("the opponent can read the chosen mode off the stack before responding") {
                val (game, decision) = butcherAtModeQuestion()
                val menaceMode = decision.options.indexOfFirst { it.contains("menace", ignoreCase = true) }
                check(menaceMode >= 0) { "menace mode not offered; options=${decision.options}" }

                game.submitDecision(OptionChosenResponse(decision.id, optionIndex = menaceMode))

                val abilityId = game.state.stack.single()
                val opponentView = game.getClientState(2).cards[abilityId]
                    ?: error("the ability should be visible on the opponent's stack")
                withClue("the opponent sees the mode, not an opaque trigger") {
                    opponentView.chosenModeDescriptions.size shouldBe 1
                    opponentView.chosenModeDescriptions.single()
                        .contains("menace", ignoreCase = true) shouldBe true
                }
            }
        }
    }
}
