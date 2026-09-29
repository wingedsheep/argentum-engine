package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hybrid Phyrexian mana `{R/G/P}` (CR 107.4f): payable with one red mana, one green mana, or
 * 2 life. Paying with mana goes through the same hybrid branches as `{R/G}`; paying with life
 * goes through the same `phyrexianLifePayments` choice as `{R/P}`, naming the pip by its first
 * color.
 */
class HybridPhyrexianManaTest : FunSpec({

    val probe = card("Hybrid Phyrexian Test Probe") {
        manaCost = "{R/G/P}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    val walker = card("Hybrid Phyrexian Test Walker") {
        manaCost = "{R/G/P}"
        typeLine = "Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "Compleated\n+1: Draw a card."
        keywords(Keyword.COMPLEATED)
        loyaltyAbility(+1) { effect = Effects.DrawCards(1) }
    }

    val convokeProbe = card("Hybrid Phyrexian Convoke Probe") {
        manaCost = "{R/G/P}"
        typeLine = "Sorcery"
        oracleText = "Convoke\nDraw a card."
        keywords(Keyword.CONVOKE)
        spell { effect = Effects.DrawCards(1) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(probe, walker, convokeProbe))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.castProbe(me: EntityId, strategy: PaymentStrategy = PaymentStrategy.AutoPay) {
        val cardId = putCardInHand(me, "Hybrid Phyrexian Test Probe")
        submitSuccess(CastSpell(playerId = me, cardId = cardId, paymentStrategy = strategy))
        bothPass()
    }

    test("auto-pay pays the pip with green mana") {
        val (driver, me) = newDriver()
        val forest = driver.putLandOnBattlefield(me, "Forest")
        driver.castProbe(me)
        driver.isTapped(forest) shouldBe true
        driver.getLifeTotal(me) shouldBe 20
    }

    test("auto-pay pays the pip with red mana") {
        val (driver, me) = newDriver()
        val mountain = driver.putLandOnBattlefield(me, "Mountain")
        driver.castProbe(me)
        driver.isTapped(mountain) shouldBe true
        driver.getLifeTotal(me) shouldBe 20
    }

    test("auto-pay spends 2 life when no red or green source exists") {
        val (driver, me) = newDriver()
        val island = driver.putLandOnBattlefield(me, "Island")
        val hand = driver.getHandSize(me)
        driver.castProbe(me)
        withClue("an Island can't pay {R/G/P}") { driver.isTapped(island) shouldBe false }
        driver.getLifeTotal(me) shouldBe 18
        driver.getHandSize(me) shouldBe hand + 1
    }

    test("an explicit life payment names the pip by its first color even with a Forest untapped") {
        val (driver, me) = newDriver()
        val forest = driver.putLandOnBattlefield(me, "Forest")
        driver.castProbe(
            me,
            PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.RED))
        )
        driver.isTapped(forest) shouldBe false
        driver.getLifeTotal(me) shouldBe 18
    }

    test("compleated takes two loyalty off when the hybrid Phyrexian pip is paid with life") {
        val (driver, me) = newDriver()
        val cardId = driver.putCardInHand(me, "Hybrid Phyrexian Test Walker")
        driver.submitSuccess(
            CastSpell(
                playerId = me, cardId = cardId,
                paymentStrategy = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.RED))
            )
        )
        driver.bothPass()
        val loyalty = driver.state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY)
        driver.getLifeTotal(me) shouldBe 18
        loyalty shouldBe 3
    }

    test("compleated keeps full loyalty when the hybrid Phyrexian pip is paid with mana") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Forest")
        val cardId = driver.putCardInHand(me, "Hybrid Phyrexian Test Walker")
        driver.submitSuccess(CastSpell(playerId = me, cardId = cardId))
        driver.bothPass()
        driver.state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a green creature convokes the hybrid Phyrexian pip") {
        val (driver, me) = newDriver()
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val cardId = driver.putCardInHand(me, "Hybrid Phyrexian Convoke Probe")
        driver.submitSuccess(
            CastSpell(
                playerId = me, cardId = cardId,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(bear to ConvokePayment(color = Color.GREEN))
                )
            )
        )
        driver.bothPass()
        driver.isTapped(bear) shouldBe true
        driver.getLifeTotal(me) shouldBe 20
    }
})
