package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Transcendent Message (MOM #83) — {X}{U}{U}{U}{U} Instant, convoke. "Draw X cards."
 */
class TranscendentMessageScenarioTest : FunSpec({

    val blueDrake = card("Message Test Drake") {
        manaCost = "{U}"
        typeLine = "Creature — Drake"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(blueDrake))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("draws X cards when paid with mana") {
        val d = driver()
        val p1 = d.activePlayer!!
        val spell = d.putCardInHand(p1, "Transcendent Message")
        d.giveMana(p1, Color.BLUE, 4)
        d.giveColorlessMana(p1, 3)
        val before = d.getHandSize(p1)

        d.castXSpell(p1, spell, xValue = 3).outcome shouldBe Outcome.Done
        d.bothPass()

        // The spell left the hand, then three cards were drawn.
        d.getHandSize(p1) shouldBe before - 1 + 3
    }

    test("convoke: blue creatures pay {U}, any creature pays generic, X still counts") {
        val d = driver()
        val p1 = d.activePlayer!!
        val drakes = (1..3).map { d.putCreatureOnBattlefield(p1, "Message Test Drake") }
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val spell = d.putCardInHand(p1, "Transcendent Message")
        // X = 2: total {2}{U}{U}{U}{U}. Three drakes pay {U}{U}{U}, Bears pays {1}, pool pays {U}{1}.
        d.giveMana(p1, Color.BLUE, 1)
        d.giveColorlessMana(p1, 1)
        val before = d.getHandSize(p1)

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                xValue = 2,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = drakes.associateWith { ConvokePayment(color = Color.BLUE) } +
                        (bears to ConvokePayment(color = null))
                )
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        (drakes + bears).forEach { d.isTapped(it) shouldBe true }
        d.getHandSize(p1) shouldBe before - 1 + 2
    }

    test("X = 0 draws nothing") {
        val d = driver()
        val p1 = d.activePlayer!!
        val spell = d.putCardInHand(p1, "Transcendent Message")
        d.giveMana(p1, Color.BLUE, 4)
        val before = d.getHandSize(p1)

        d.castXSpell(p1, spell, xValue = 0).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getHandSize(p1) shouldBe before - 1
    }
})
