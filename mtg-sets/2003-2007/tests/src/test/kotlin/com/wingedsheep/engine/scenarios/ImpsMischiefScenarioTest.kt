package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.plc.cards.ImpsMischief
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Imp's Mischief (PLC #72) — "Change the target of target spell with a single target. You lose
 * life equal to that spell's mana value."
 *
 * The life loss follows a retarget that pauses for the new-target choice, so this pins that the
 * composite resumes into the loss after the pause and reads the redirected spell's mana value.
 */
class ImpsMischiefScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ImpsMischief))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("redirects the Bolt back at its caster and costs life equal to its mana value") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val bolt = d.putCardInHand(p2, "Lightning Bolt")
        val mischief = d.putCardInHand(p1, "Imp's Mischief")
        d.giveMana(p2, Color.RED, 1)
        d.giveMana(p1, Color.BLACK, 2)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, bolt, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(
                p1, mischief,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done

        d.bothPass() // Imp's Mischief resolves and pauses for the new target
        d.submitCardSelection(p1, listOf(p2)).error shouldBe null

        withClue("you lose life equal to the Bolt's mana value") {
            d.getLifeTotal(p1) shouldBe 19
        }

        var guard = 0
        while (d.stackSize > 0 && guard < 10) {
            d.bothPass()
            guard++
        }

        withClue("the redirected Bolt hits its own caster") {
            d.getLifeTotal(p2) shouldBe 17
        }
        d.getLifeTotal(p1) shouldBe 19
    }
})
