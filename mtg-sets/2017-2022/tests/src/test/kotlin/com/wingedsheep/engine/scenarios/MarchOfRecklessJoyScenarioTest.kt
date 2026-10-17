package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.MarchOfRecklessJoy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * March of Reckless Joy — {X}{R} instant: "you may exile any number of red cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way. Exile the top X cards of your
 * library. You may play up to two of those cards until the end of your next turn."
 */
class MarchOfRecklessJoyScenarioTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val redCard = card("Test Red Card") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarchOfRecklessJoy, bear, redCard))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? CastSpell)?.cardId == cardId }

    test("X = 3 for {1}{R} with one red card exiled; a land and a spell are the two plays, the third card is locked") {
        val d = setup()
        val me = d.activePlayer!!
        val third = d.putCardOnTopOfLibrary(me, "Test Bear")
        val second = d.putCardOnTopOfLibrary(me, "Test Bear")
        val land = d.putCardOnTopOfLibrary(me, "Mountain")
        val march = d.putCardInHand(me, "March of Reckless Joy")
        val fodder = d.putCardInHand(me, "Test Red Card")
        d.giveMana(me, Color.RED, 2)

        val cast = d.submit(CastSpell(
            me, march, xValue = 3,
            additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        cast.outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("The top three cards are exiled alongside the exiled red card") {
            d.getExile(me).containsAll(listOf(land, second, third, fodder)) shouldBe true
            d.canCast(me, second) shouldBe true
            d.canCast(me, third) shouldBe true
        }

        d.playLand(me, land).error shouldBe null
        d.giveMana(me, Color.GREEN, 2)
        d.submit(CastSpell(me, second, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("The land and the Bear were the two plays: the last card stays in exile, unplayable") {
            d.findPermanent(me, "Test Bear") shouldBe second
            d.canCast(me, third) shouldBe false
            d.getExile(me).contains(third) shouldBe true
        }
    }
})
