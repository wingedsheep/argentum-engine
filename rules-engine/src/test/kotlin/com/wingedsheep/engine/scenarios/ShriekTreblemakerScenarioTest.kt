package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.ShriekTreblemaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shriek, Treblemaker (SPM #144) — {2}{B/R} Legendary Creature — Mutant Villain 2/3.
 *
 * "At the beginning of your first main phase, you may discard a card. When you do, target
 *  creature can't block this turn.
 *  Sonic Blast — Whenever a creature an opponent controls dies, Shriek deals 1 damage to that player."
 *
 * The first-main ability is a "When you do" reflexive: the optional discard is the action, and only
 * if a card is actually discarded does the "target creature can't block this turn" restriction go on
 * the stack. Sonic Blast (an ability word — flavor only) is a dies trigger over opponent-controlled
 * creatures dealing 1 to that creature's controller.
 */
class ShriekTreblemakerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(ShriekTreblemaker)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    test("first-main optional discard makes a target creature unable to block this turn") {
        val d = newDriver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        // Shriek must be on the battlefield before the precombat main phase begins so the
        // FirstMainPhase trigger fires on the phase transition.
        d.putCreatureOnBattlefield(you, "Shriek, Treblemaker")
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears") // the "target creature"
        val handBefore = d.getHandSize(you)

        // Enter the precombat main step; the trigger fires and offers the optional discard.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        var targeted = false
        var guard = 0
        while (!targeted && guard++ < 40) {
            when (val dec = d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(you, true)                     // "you may discard a card"
                is SelectCardsDecision -> d.submitCardSelection(you, dec.options.take(1)) // discard a Mountain
                is ChooseTargetsDecision -> { d.submitTargetSelection(you, listOf(blocker)); targeted = true }
                else -> d.bothPass()
            }
        }
        targeted shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.cantBlock(blocker) shouldBe true
        d.getHandSize(you) shouldBe handBefore - 1
    }

    test("declining the optional discard leaves the creature able to block") {
        val d = newDriver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        d.putCreatureOnBattlefield(you, "Shriek, Treblemaker")
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val handBefore = d.getHandSize(you)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        var answered = false
        var guard = 0
        while (!answered && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> { d.submitYesNo(you, false); answered = true }
                else -> d.bothPass()
            }
        }
        answered shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.cantBlock(blocker) shouldBe false
        d.getHandSize(you) shouldBe handBefore
    }

    test("Sonic Blast: an opponent's creature dying deals 1 damage to that player") {
        val d = newDriver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(you, "Shriek, Treblemaker")
        val oppLifeBefore = d.getLifeTotal(opponent)

        // Kill an opponent-controlled creature -> Sonic Blast deals 1 to its controller.
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(giant)).isSuccess shouldBe true
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getLifeTotal(opponent) shouldBe oppLifeBefore - 1
    }

    test("Sonic Blast does not trigger when your own creature dies") {
        val d = newDriver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(you, "Shriek, Treblemaker")
        val youLifeBefore = d.getLifeTotal(you)

        // Kill your own creature -> the ability only watches opponents' creatures, so no damage.
        val ownGiant = d.putCreatureOnBattlefield(you, "Hill Giant")
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(ownGiant)).isSuccess shouldBe true
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getLifeTotal(you) shouldBe youLifeBefore
    }
})
