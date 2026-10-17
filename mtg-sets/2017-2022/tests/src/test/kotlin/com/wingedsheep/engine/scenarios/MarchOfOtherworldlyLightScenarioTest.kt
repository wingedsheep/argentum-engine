package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.MarchOfOtherworldlyLight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * March of Otherworldly Light — {X}{W} instant: "you may exile any number of white cards from your
 * hand. This spell costs {2} less to cast for each card exiled this way. Exile target artifact,
 * creature, or enchantment with mana value X or less."
 */
class MarchOfOtherworldlyLightScenarioTest : FunSpec({

    val threeDrop = card("Test Three Drop") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
    }

    val whiteCard = card("Test White Card") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarchOfOtherworldlyLight, threeDrop, whiteCard))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("exiling a white card pays {2} of X = 3, exiling a mana value 3 creature for {1}{W}") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val beast = d.putCreatureOnBattlefield(opponent, "Test Three Drop")
        val march = d.putCardInHand(me, "March of Otherworldly Light")
        val fodder = d.putCardInHand(me, "Test White Card")
        d.giveMana(me, Color.WHITE, 2)

        val cast = d.submit(CastSpell(
            me, march, targets = listOf(ChosenTarget.Permanent(beast)), xValue = 3,
            additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        cast.outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(opponent, "Test Three Drop") shouldBe null
        d.getExileCardNames(opponent) shouldBe listOf("Test Three Drop")
        d.getExileCardNames(me) shouldBe listOf("Test White Card")
    }

    test("X bounds the target — a mana value 3 creature can't be targeted with X = 2") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val beast = d.putCreatureOnBattlefield(opponent, "Test Three Drop")
        val march = d.putCardInHand(me, "March of Otherworldly Light")
        d.giveMana(me, Color.WHITE, 3)

        val cast = d.submit(CastSpell(
            me, march, targets = listOf(ChosenTarget.Permanent(beast)), xValue = 2,
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        (cast.outcome is Outcome.Rejected) shouldBe true
    }
})
