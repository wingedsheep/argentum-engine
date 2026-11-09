package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "You may choose not to untap" (`AbilityFlag.MAY_NOT_UNTAP`) — the AI keeps such a permanent
 * tapped only while an effect of its own lasts "for as long as it remains tapped"
 * ([KeepTappedChoice]). The generic "choose up to N" ranking used to answer the untap-step choice
 * and, under `selectionCountsByValue`, left Lwaxana Troi tapped every turn with no creature chosen.
 */
class MayNotUntapKeepTappedAiTest : ScenarioTestBase() {

    private val lwaxana = "Lwaxana Troi, Charming Guest"

    private fun responders() = listOf(false, true).map { countsByValue ->
        countsByValue to DecisionResponder(
            GameSimulator(cardRegistry), AIPlayer.defaultEvaluator(), selectionCountsByValue = countsByValue,
        )
    }

    /** Player 2's end step, with Lwaxana on Player 1's side and mana to activate her. */
    private fun board(lwaxanaTapped: Boolean) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, lwaxana, tapped = lwaxanaTapped)
        .withCardOnBattlefield(1, "Ajani's Pridemate")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    /** Pass priority until Player 1's untap step asks which permanents to keep tapped. */
    private fun TestGame.passToUntapChoice(): SelectCardsDecision {
        repeat(20) {
            state.pendingDecision?.let { return it.shouldBeInstanceOf<SelectCardsDecision>() }
            execute(PassPriority(state.priorityPlayerId ?: error("no priority player"))).error shouldBe null
        }
        error("no untap-step choice was raised")
    }

    init {
        test("Lwaxana with no chosen creature untaps") {
            val game = board(lwaxanaTapped = true)
            val decision = game.passToUntapChoice()
            decision.options shouldBe listOf(game.findPermanent(lwaxana)!!)

            for ((countsByValue, responder) in responders()) {
                withClue("selectionCountsByValue = $countsByValue") {
                    responder.respond(game.state, decision, game.player1Id)
                        .shouldBeInstanceOf<CardsSelectedResponse>().selectedCards.shouldBeEmpty()
                }
            }
        }

        test("Lwaxana doubling a chosen creature's triggers stays tapped") {
            val game = board(lwaxanaTapped = false)
            val lwaxanaId = game.findPermanent(lwaxana)!!
            game.execute(PassPriority(game.player2Id)).error shouldBe null
            game.execute(
                ActivateAbility(
                    game.player1Id, lwaxanaId,
                    cardRegistry.getCard(lwaxana)!!.script.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Ajani's Pridemate")!!)),
                )
            ).error shouldBe null
            game.resolveStack()

            val decision = game.passToUntapChoice()
            for ((countsByValue, responder) in responders()) {
                withClue("selectionCountsByValue = $countsByValue") {
                    responder.respond(game.state, decision, game.player1Id)
                        .shouldBeInstanceOf<CardsSelectedResponse>().selectedCards shouldBe listOf(lwaxanaId)
                }
            }
        }
    }
}
