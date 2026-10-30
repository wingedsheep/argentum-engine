package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frost Lynx (M15 #55) — {2}{U} Creature — Elemental Cat 2/2.
 *
 *   "When this creature enters, tap target creature an opponent controls. That creature doesn't
 *    untap during its controller's next untap step."
 *
 * The freeze covers exactly one of the opponent's untap steps: the creature stays tapped through
 * the next one and untaps normally on the one after.
 */
class FrostLynxScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Advance to the next turn's precombat main (two hops: the call stops as soon as the step matches). */
    fun GameTestDriver.passToNextTurnMain() {
        listOf(player1, player2).forEach { p -> getHand(p).forEach { moveToGraveyard(it) } }
        passPriorityUntil(Step.END)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("taps an opponent's creature, which stays tapped through exactly one of its controller's untap steps") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val ours = d.putCreatureOnBattlefield(d.player1, "Hill Giant")

        val lynx = d.putCardInHand(d.player1, "Frost Lynx")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.castSpell(d.player1, lynx)

        // Pass until Frost Lynx has resolved and its ETB trigger asks for a target.
        var decision: ChooseTargetsDecision? = null
        var guard = 0
        while (guard++ < 8) {
            val pending = d.pendingDecision
            if (pending is ChooseTargetsDecision) { decision = pending; break }
            if (d.stackSize == 0) break
            d.bothPass()
        }
        val targetPrompt = checkNotNull(decision) { "Frost Lynx's ETB should ask for a target" }
        withClue("only a creature an opponent controls is legal") {
            val options = targetPrompt.legalTargets.values.flatten()
            options.contains(victim) shouldBe true
            options.contains(ours) shouldBe false
        }
        d.submitTargetSelection(d.player1, listOf(victim))
        d.bothPass()

        d.isTapped(victim) shouldBe true

        // Player 2's turn: their untap step skips the frozen Bears.
        d.passToNextTurnMain()
        d.activePlayer shouldBe d.player2
        withClue("doesn't untap during its controller's next untap step") {
            d.isTapped(victim) shouldBe true
        }

        // Back to player 1, then player 2's following turn: the freeze has expired.
        d.passToNextTurnMain()
        d.passToNextTurnMain()
        d.activePlayer shouldBe d.player2
        withClue("the freeze covers only one untap step") {
            d.isTapped(victim) shouldBe false
        }
    }
})
