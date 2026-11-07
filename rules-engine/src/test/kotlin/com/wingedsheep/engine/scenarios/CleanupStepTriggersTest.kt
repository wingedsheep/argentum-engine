package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 514.3a: abilities that trigger during the cleanup step (the hand-size discard is the usual
 * source) or state-based actions performed then go on the stack in that cleanup step, the active
 * player gets priority there, and once the stack is empty and all players pass another cleanup step
 * begins. Without either no player gets priority and the turn ends.
 */
class CleanupStepTriggersTest : FunSpec({

    val discardTax = card("Cleanup Test Discard Tax") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.discards()
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You))
        }
    }

    val endTheTurn = card("Cleanup Test End the Turn") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.EndTheTurn }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(discardTax, endTheTurn))
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20), skipMulligans = true)
        return d
    }

    fun discardDownToSeven(d: GameTestDriver) {
        val me = d.activePlayer!!
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, d.getHand(me).take(decision.minSelections))
    }

    test("a discard trigger during cleanup goes on the stack in that cleanup step, with priority") {
        val d = driver()
        val me = d.activePlayer!!
        val turn = d.state.turnNumber
        d.putPermanentOnBattlefield(me, "Cleanup Test Discard Tax")
        repeat(2) { d.putCardInHand(me, "Grizzly Bears") }

        d.passPriorityUntil(Step.END)
        d.bothPass()
        discardDownToSeven(d)

        d.state.turnNumber shouldBe turn
        d.state.step shouldBe Step.CLEANUP
        d.assertStackSize(2)
        d.state.priorityPlayerId shouldBe me
        // Players may respond: the opponent receives priority in the cleanup step too.
        d.passPriority(me)
        d.state.priorityPlayerId shouldBe d.getOpponent(me)
        d.passPriority(d.getOpponent(me))

        d.bothPass()
        d.getLifeTotal(me) shouldBe 18
        // The stack is empty and both players passed: another cleanup step begins, finds nothing
        // to do, and the turn ends.
        d.bothPass()
        d.activePlayer shouldBe d.getOpponent(me)
    }

    test("the next cleanup step checks the hand size again") {
        val d = driver()
        val me = d.activePlayer!!
        val turn = d.state.turnNumber
        d.putPermanentOnBattlefield(me, "Cleanup Test Discard Tax")
        d.putCardInHand(me, "Grizzly Bears")

        d.passPriorityUntil(Step.END)
        d.bothPass()
        discardDownToSeven(d)
        d.bothPass() // resolve the life-loss trigger
        // Holding priority in the cleanup step, the player's hand grows past seven again.
        d.putCardInHand(me, "Grizzly Bears")
        d.bothPass()

        d.state.turnNumber shouldBe turn
        d.state.step shouldBe Step.CLEANUP
        discardDownToSeven(d)
        d.assertStackSize(1)
        d.bothPass()
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.activePlayer shouldBe d.getOpponent(me)
    }

    test("a discard that triggers nothing ends the turn without priority in cleanup") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCardInHand(me, "Grizzly Bears")

        d.passPriorityUntil(Step.END)
        d.bothPass()
        discardDownToSeven(d)

        d.activePlayer shouldBe d.getOpponent(me)
        d.getLifeTotal(me) shouldBe 20
    }

    test("a creature an ending 'until end of turn' effect leaves at 0 toughness dies in the cleanup step") {
        val d = driver()
        val me = d.activePlayer!!
        val turn = d.state.turnNumber
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, d.putCardInHand(me, "Giant Growth"), listOf(bears))
        d.bothPass()
        d.replaceState(d.state.updateEntity(bears) { container ->
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            container.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, 2))
        })

        d.passPriorityUntil(Step.END)
        d.bothPass()

        withClue("Giant Growth ends in cleanup (CR 514.2); the 0/0 dies there and the player gets priority") {
            d.state.turnNumber shouldBe turn
            d.state.step shouldBe Step.CLEANUP
            d.getGraveyardCardNames(me).contains("Grizzly Bears") shouldBe true
            d.state.priorityPlayerId shouldBe me
        }
        d.bothPass()
        d.activePlayer shouldBe d.getOpponent(me)
    }

    test("an end-the-turn effect's cleanup discard triggers in that cleanup step") {
        val d = driver()
        val me = d.activePlayer!!
        val turn = d.state.turnNumber
        d.putPermanentOnBattlefield(me, "Cleanup Test Discard Tax")
        repeat(2) { d.putCardInHand(me, "Grizzly Bears") }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castSpell(me, d.putCardInHand(me, "Cleanup Test End the Turn"))
        d.bothPass()
        discardDownToSeven(d)

        d.state.turnNumber shouldBe turn
        d.state.step shouldBe Step.CLEANUP
        d.assertStackSize(2)
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.activePlayer shouldBe d.getOpponent(me)
    }
})
