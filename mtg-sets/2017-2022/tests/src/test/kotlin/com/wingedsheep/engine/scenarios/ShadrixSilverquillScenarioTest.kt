package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shadrix Silverquill (STX #230) — {3}{W}{B} Legendary Creature — Elder Dragon 2/5.
 *
 *   Flying, double strike
 *   At the beginning of combat on your turn, you may choose two. Each mode must target a different
 *   player.
 *   • Target player creates a 2/1 white and black Inkling creature token with flying.
 *   • Target player draws a card and loses 1 life.
 *   • Target player puts a +1/+1 counter on each creature they control.
 *
 * Exactly zero or two modes (ruling), each aimed at a different player, all decided as the trigger
 * goes on the stack. The third mode may target a player who controls no creatures (ruling).
 */
class ShadrixSilverquillScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Shadrix Silverquill")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .apply { repeat(3) { withCardInLibrary(1, "Swamp") } }
        .apply { repeat(3) { withCardInLibrary(2, "Swamp") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.modeQuestion(): ChooseOptionDecision =
        getPendingDecision() as? ChooseOptionDecision ?: error("expected Shadrix's mode question; got ${getPendingDecision()}")

    private fun TestGame.pickMode(fragment: String) {
        val decision = modeQuestion()
        val index = decision.options.indexOfFirst { it.contains(fragment) }
        check(index >= 0) { "'$fragment' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("opponent draws and loses 1 life, you put a +1/+1 counter on each of your creatures") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            game.pickMode("draws a card")
            game.pickMode("+1/+1 counter")
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            withClue("the counter mode can only target the other player, so it isn't asked") {
                game.getPendingDecision() shouldBe null
            }

            val opponentHand = game.handSize(2)
            game.resolveStack()

            game.handSize(2) shouldBe opponentHand + 1
            game.getLifeTotal(2) shouldBe 19
            game.plusOneCounters("Grizzly Bears") shouldBe 1
            game.plusOneCounters("Shadrix Silverquill") shouldBe 1
        }

        test("an opponent with no creatures can still get the Inkling while you draw") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            game.pickMode("Inkling")
            game.pickMode("draws a card")
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            val myHand = game.handSize(1)
            game.resolveStack()

            val inkling = game.findPermanents("Inkling Token").single()
            game.state.projectedState.getController(inkling) shouldBe game.player2Id
            game.handSize(1) shouldBe myHand + 1
            game.getLifeTotal(1) shouldBe 19
        }

        test("the counter mode may target an opponent who controls no creatures") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            game.pickMode("draws a card")
            game.pickMode("+1/+1 counter")
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            withClue("Player2 controls no creatures yet is the counter mode's target") {
                game.getPendingDecision() shouldBe null
            }
            val myHand = game.handSize(1)
            game.resolveStack()

            game.handSize(1) shouldBe myHand + 1
            game.getLifeTotal(1) shouldBe 19
            game.plusOneCounters("Grizzly Bears") shouldBe 0
        }

        test("you may choose no mode, but never just one") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            val first = game.modeQuestion()
            first.options.last() shouldBe ModalEffectExecutor.DECLINE_MODE_LABEL
            game.pickMode("Inkling")
            withClue("having picked one mode, a second is mandatory") {
                game.modeQuestion().options shouldNotContain ModalEffectExecutor.DECLINE_MODE_LABEL
            }
        }

        test("choosing no mode puts nothing on the stack") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            val first = game.modeQuestion()
            game.submitDecision(OptionChosenResponse(first.id, first.options.lastIndex)).error shouldBe null
            game.state.stack.mapNotNull { game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
                .shouldBeEmpty()
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
