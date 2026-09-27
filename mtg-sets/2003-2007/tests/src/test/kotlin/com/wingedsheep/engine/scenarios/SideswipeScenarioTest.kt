package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.GlacialRay
import com.wingedsheep.mtg.sets.definitions.chk.cards.Sideswipe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sideswipe (CHK #187) — "You may change any targets of target Arcane spell."
 */
class SideswipeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Sideswipe, GlacialRay))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && guard < 10) {
            d.bothPass()
            guard++
        }
    }

    test("redirects an opponent's Arcane spell back at its caster") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val ray = d.putCardInHand(p2, "Glacial Ray")
        val sideswipe = d.putCardInHand(p1, "Sideswipe")
        d.giveMana(p2, Color.RED, 2)
        d.giveMana(p1, Color.RED, 2)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, ray, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val rayOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(p1, sideswipe, targets = listOf(ChosenTarget.Spell(rayOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done

        d.bothPass() // Sideswipe resolves and offers the Ray's one target slot
        d.submitCardSelection(p1, listOf(p2)).error shouldBe null

        resolveAll(d)

        withClue("the retargeted Glacial Ray hits its own caster") {
            d.getLifeTotal(p2) shouldBe 18
            d.getLifeTotal(p1) shouldBe 20
        }
    }

    test("choosing to keep the current target leaves the spell pointed where it was") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val ray = d.putCardInHand(p2, "Glacial Ray")
        val sideswipe = d.putCardInHand(p1, "Sideswipe")
        d.giveMana(p2, Color.RED, 2)
        d.giveMana(p1, Color.RED, 2)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, ray, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val rayOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(p1, sideswipe, targets = listOf(ChosenTarget.Spell(rayOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done

        d.bothPass()
        d.submitCardSelection(p1, listOf(p1)).error shouldBe null

        resolveAll(d)

        d.getLifeTotal(p1) shouldBe 18
        d.getLifeTotal(p2) shouldBe 20
    }

    test("cannot target a non-Arcane spell") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val bolt = d.putCardInHand(p2, "Lightning Bolt")
        val sideswipe = d.putCardInHand(p1, "Sideswipe")
        d.giveMana(p2, Color.RED, 1)
        d.giveMana(p1, Color.RED, 2)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, bolt, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(p2)

        d.submit(
            CastSpell(p1, sideswipe, targets = listOf(ChosenTarget.Spell(boltOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldNotBe null
    }
})
