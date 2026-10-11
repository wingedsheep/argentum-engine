package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.NarsetsReversal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Narset's Reversal (WAR #62) — "Copy target instant or sorcery spell, then return it to its
 * owner's hand. You may choose new targets for the copy."
 *
 * The composition is a copy that pauses for a retarget decision followed by a bounce of the same
 * targeted spell; the test proves the composite resumes into the bounce after the copy's prompt,
 * and that the original never resolves.
 */
class NarsetsReversalScenarioTest : FunSpec({

    test("copies an opponent's Bolt back at them, then returns the original to its owner's hand") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NarsetsReversal))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val bolt = d.putCardInHand(p2, "Lightning Bolt")
        val reversal = d.putCardInHand(p1, "Narset's Reversal")
        d.giveMana(p2, Color.RED, 1)
        d.giveMana(p1, Color.BLUE, 2)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, bolt, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(
                p1, reversal,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done

        d.bothPass() // Narset's Reversal resolves and pauses on the copy's retarget prompt

        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(p2)).error shouldBe null

        withClue("the composite resumes into the bounce: the original Bolt is back in its owner's hand") {
            d.getHand(p2) shouldContain boltOnStack
            d.state.stack shouldNotContain boltOnStack
        }

        var guard = 0
        while (d.stackSize > 0 && guard < 10) {
            d.bothPass()
            guard++
        }

        withClue("only the copy resolved, and it hit the Bolt's caster") {
            d.getLifeTotal(p2) shouldBe 17
            d.getLifeTotal(p1) shouldBe 20
        }
    }
})
