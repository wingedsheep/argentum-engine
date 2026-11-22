package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m14.cards.SavageSummoning
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Savage Summoning (M14 #194) — "This spell can't be countered. The next creature spell you cast
 * this turn can be cast as though it had flash. That spell can't be countered. That creature enters
 * with an additional +1/+1 counter on it."
 *
 * The flash grant's rules are pinned in `NextSpellFlashGrantTest`; this pins the card: one creature
 * spell gets all three riders, a noncreature spell in between spends none, and the creature after
 * it gets nothing.
 */
class SavageSummoningScenarioTest : FunSpec({

    val instant = CardDefinition.instant(
        name = "Summoning Test Instant",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = Effects.DrawCards(1))
    )

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SavageSummoning, instant))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
    }

    test("the next creature spell is flashed in, can't be countered, and enters with a +1/+1 counter") {
        val d = setup()
        val p1 = d.player1
        d.passPriorityUntil(Step.END)

        val summoning = d.putCardInHand(p1, "Savage Summoning")
        val other = d.putCardInHand(p1, "Summoning Test Instant")
        val courser = d.putCardInHand(p1, "Centaur Courser")
        val second = d.putCardInHand(p1, "Centaur Courser")
        d.giveMana(p1, Color.GREEN, 1)
        d.submitSuccess(CastSpell(p1, summoning, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()

        // A noncreature spell doesn't spend the creature riders.
        d.giveMana(p1, Color.GREEN, 1)
        d.submitSuccess(CastSpell(p1, other, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()

        d.giveMana(p1, Color.GREEN, 6)
        d.submit(CastSpell(p1, courser, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.state.getEntity(courser)?.has<CantBeCounteredComponent>() shouldBe true
        d.state.pendingNextSpellRiders shouldBe emptyList()
        d.bothPass()
        d.plusOnes(courser) shouldBe 1

        // The second creature spell has no flash left.
        d.submit(CastSpell(p1, second, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
    }

    test("a creature cast at sorcery speed is still the next creature spell; the one after gets nothing") {
        val d = setup()
        val p1 = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val summoning = d.putCardInHand(p1, "Savage Summoning")
        val courser = d.putCardInHand(p1, "Centaur Courser")
        val second = d.putCardInHand(p1, "Centaur Courser")
        d.giveMana(p1, Color.GREEN, 7)
        d.submitSuccess(CastSpell(p1, summoning, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()

        d.submitSuccess(CastSpell(p1, courser, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitSuccess(CastSpell(p1, second, paymentStrategy = PaymentStrategy.FromPool))
        d.state.getEntity(second)?.has<CantBeCounteredComponent>() shouldBe false
        d.bothPass()

        d.plusOnes(courser) shouldBe 1
        d.plusOnes(second) shouldBe 0
    }
})
