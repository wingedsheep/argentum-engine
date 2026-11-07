package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.kld.cards.InsidiousWill
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Insidious Will (KLD #52) — "Choose one — • Counter target spell. • You may choose new targets for
 * target spell. • Copy target instant or sorcery spell. You may choose new targets for the copy."
 *
 * The second mode is the first use of `ChangeTriggeringObjectTargets(spell = …)` inside a modal
 * mode, so the test proves the mode-local target resolves to the targeted spell and the retarget
 * prompt actually moves it. The third mode's copy takes a new target of its own.
 */
class InsidiousWillScenarioTest : FunSpec({

    class Game(
        val d: GameTestDriver,
        val p1: EntityId,
        val p2: EntityId,
        val myLions: EntityId,
        val theirCourser: EntityId,
        val bolt: EntityId,
    )

    /** P2 bolts P1's Savannah Lions; P1 answers with Insidious Will in [mode] targeting the Bolt. */
    fun answerBolt(mode: Int): Game {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(InsidiousWill))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val myLions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        val theirCourser = d.putCreatureOnBattlefield(p2, "Centaur Courser")

        val bolt = d.putCardInHand(p2, "Lightning Bolt")
        val will = d.putCardInHand(p1, "Insidious Will")
        d.giveMana(p2, Color.RED, 1)
        d.giveMana(p1, Color.BLUE, 4)

        d.passPriority(p1)
        d.submit(
            CastSpell(
                p2, bolt,
                targets = listOf(ChosenTarget.Permanent(myLions)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(
                p1, will,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool,
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltOnStack)))
            )
        ).outcome shouldBe Outcome.Done

        d.bothPass() // Insidious Will resolves (and pauses on its decision, if the mode has one)
        return Game(d, p1, p2, myLions, theirCourser, boltOnStack)
    }

    fun Game.resolveAll() {
        var guard = 0
        while (d.stackSize > 0 && guard < 10) {
            d.bothPass()
            guard++
        }
    }

    test("mode 2 redirects the opponent's Bolt from my creature to theirs") {
        val g = answerBolt(mode = 1)
        val d = g.d

        // The retarget prompt for the Bolt's single slot: point it at the opponent's Courser.
        d.submitCardSelection(g.p1, listOf(g.theirCourser)).error shouldBe null
        g.resolveAll()

        withClue("the retargeted Bolt kills the opponent's 3/3 and spares my Lions") {
            d.findPermanent(g.p2, "Centaur Courser") shouldBe null
            d.findPermanent(g.p1, "Savannah Lions") shouldBe g.myLions
        }
        (g.bolt in d.getGraveyard(g.p2)) shouldBe true
    }

    test("mode 3 copies the Bolt and the copy takes a new target") {
        val g = answerBolt(mode = 2)
        val d = g.d

        withClue("the copy asks for its own targets") {
            (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        }
        d.submitTargetSelection(g.p1, listOf(g.theirCourser)).error shouldBe null
        g.resolveAll()

        withClue("the copy kills the Courser; the original Bolt still kills my Lions") {
            d.findPermanent(g.p2, "Centaur Courser") shouldBe null
            d.findPermanent(g.p1, "Savannah Lions") shouldBe null
        }
    }
})
