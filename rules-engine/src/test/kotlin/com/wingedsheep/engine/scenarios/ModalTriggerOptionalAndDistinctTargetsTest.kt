package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.effects.Mode
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The two [com.wingedsheep.sdk.scripting.effects.ModalEffect] axes a modal *triggered* ability
 * reads as its modes and targets are announced (CR 603.3c):
 *
 * - `mayChooseNone` — "you **may** choose two": zero or exactly two modes, never one (Shadrix
 *   Silverquill's ruling). Declining is offered only before the first pick, and declining removes
 *   the ability rather than putting it on the stack.
 * - `eachModeTargetsDifferent` — "each mode must target a different player": no player is a target
 *   of two chosen modes. Later modes' legal targets exclude what earlier modes took; a mode that
 *   couldn't get a distinct target isn't offered (Vindictive Lich with one opponent picks one of
 *   its opponent modes); and an earlier mode can't take the only target a later mode could use.
 *
 * Inline test cards, each an ETB trigger so the tests only need to cast them.
 */
class ModalTriggerOptionalAndDistinctTargetsTest : ScenarioTestBase() {

    private fun drawMode(name: String) = mode("Target player draws a card ($name)") {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(1, player)
    }

    private fun opponentLosesMode(amount: Int) = mode("Target opponent loses $amount life") {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(amount, opponent)
    }

    private fun etbModal(name: String, vararg modes: Mode, choose: Int, min: Int, mayChooseNone: Boolean) =
        card(name) {
            manaCost = "{0}"
            typeLine = "Artifact Creature — Construct"
            power = 1
            toughness = 1
            oracleText = "When this creature enters, choose modes."
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = Effects.Modal(
                    modes = modes.toList(),
                    chooseCount = choose,
                    minChooseCount = min,
                    mayChooseNone = mayChooseNone,
                    eachModeTargetsDifferent = true
                )
            }
        }

    /** Shadrix's shape: you may choose two of three "target player" modes. */
    private val mayChooseTwo = etbModal(
        "Test May Choose Two",
        drawMode("A"), drawMode("B"), drawMode("C"),
        choose = 2, min = 2, mayChooseNone = true
    )

    /** Vindictive Lich's shape: choose one or more, every mode targets an opponent. */
    private val oneOrMoreOpponents = etbModal(
        "Test One Or More Opponents",
        opponentLosesMode(1), opponentLosesMode(2), opponentLosesMode(3),
        choose = 3, min = 1, mayChooseNone = false
    )

    /** Must choose two, but both modes can only target the one opponent. */
    private val mayChooseTwoOpponentModes = etbModal(
        "Test May Choose Two Opponent Modes",
        opponentLosesMode(1), opponentLosesMode(2),
        choose = 2, min = 2, mayChooseNone = true
    )

    /** One "any player" mode and one "target opponent" mode, must choose both. */
    private val anyThenOpponent = etbModal(
        "Test Any Then Opponent",
        drawMode("any"), opponentLosesMode(2),
        choose = 2, min = 2, mayChooseNone = false
    )

    private fun TestGame.triggersOnStack(): List<TriggeredAbilityOnStackComponent> =
        state.stack.mapNotNull { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }

    private fun TestGame.modeQuestion(): ChooseOptionDecision =
        getPendingDecision() as? ChooseOptionDecision ?: error("expected a mode question; got ${getPendingDecision()}")

    private fun TestGame.pickMode(label: String) {
        val decision = modeQuestion()
        val index = decision.options.indexOfFirst { it.contains(label) }
        check(index >= 0) { "'$label' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun castInto(cardName: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, cardName)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, cardName).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game
    }

    private fun ChosenTarget.playerId(): Any? = (this as? ChosenTarget.Player)?.playerId

    init {
        cardRegistry.register(mayChooseTwo)
        cardRegistry.register(oneOrMoreOpponents)
        cardRegistry.register(mayChooseTwoOpponentModes)
        cardRegistry.register(anyThenOpponent)

        context("mayChooseNone — you may choose two") {

            test("choosing no mode is on the first menu, and removes the ability") {
                val game = castInto("Test May Choose Two")
                val first = game.modeQuestion()
                first.options shouldContainExactly listOf(
                    "Target player draws a card (A)", "Target player draws a card (B)",
                    "Target player draws a card (C)", ModalEffectExecutor.DECLINE_MODE_LABEL
                )

                game.submitDecision(OptionChosenResponse(first.id, first.options.lastIndex)).error shouldBe null

                withClue("no mode chosen — the ability never reaches the stack (CR 603.3c)") {
                    game.triggersOnStack().shouldBeEmpty()
                    game.getPendingDecision() shouldBe null
                }
            }

            test("after one pick there is no way out but a second mode — never exactly one") {
                val game = castInto("Test May Choose Two")
                game.pickMode("(A)")

                val second = game.modeQuestion()
                withClue("the decline option is gone once a mode is picked") {
                    second.options shouldNotContain ModalEffectExecutor.DECLINE_MODE_LABEL
                }
                second.options shouldContainExactly listOf(
                    "Target player draws a card (B)", "Target player draws a card (C)"
                )
            }
        }

        context("eachModeTargetsDifferent — each mode must target a different player") {

            test("the second mode's targets exclude the player the first mode took") {
                val game = castInto("Test May Choose Two")
                game.pickMode("(A)")
                game.pickMode("(C)")

                val firstTargets = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                firstTargets.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder
                    listOf(game.player1Id, game.player2Id)
                game.selectTargets(listOf(game.player2Id)).error shouldBe null

                withClue("only Player1 is left for mode C, so it is assigned without a prompt") {
                    game.getPendingDecision() shouldBe null
                }
                val onStack = game.triggersOnStack().single()
                onStack.chosenModes shouldBe listOf(0, 2)
                onStack.modeTargetsOrdered.map { slot -> slot.map { it.playerId() } } shouldBe
                    listOf(listOf(game.player2Id), listOf(game.player1Id))

                val handsBefore = game.handSize(1) to game.handSize(2)
                game.resolveStack()
                (game.handSize(1) to game.handSize(2)) shouldBe (handsBefore.first + 1 to handsBefore.second + 1)
            }

            test("with one opponent, only one opponent-targeting mode can be chosen") {
                val game = castInto("Test One Or More Opponents")
                game.pickMode("loses 2")

                withClue("no other mode could get a distinct target, so the pick ends there") {
                    game.getPendingDecision() shouldBe null
                }
                val onStack = game.triggersOnStack().single()
                onStack.chosenModes shouldBe listOf(1)

                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
            }

            test("when two distinct targets are impossible, may-choose-two removes the ability unasked") {
                val game = castInto("Test May Choose Two Opponent Modes")

                withClue("no pair of modes can target different players, so no mode is offered") {
                    game.getPendingDecision() shouldBe null
                    game.triggersOnStack().shouldBeEmpty()
                }
                game.getLifeTotal(2) shouldBe 20
            }

            test("an earlier mode can't take the only target a later mode could use") {
                val game = castInto("Test Any Then Opponent")
                game.pickMode("(any)")
                game.pickMode("loses 2")

                withClue("the opponent is reserved for the opponent-only mode, so the draw mode is auto-assigned to you") {
                    game.getPendingDecision() shouldBe null
                }
                val onStack = game.triggersOnStack().single()
                onStack.modeTargetsOrdered.map { slot -> slot.map { it.playerId() } } shouldBe
                    listOf(listOf(game.player1Id), listOf(game.player2Id))
            }
        }
    }
}
