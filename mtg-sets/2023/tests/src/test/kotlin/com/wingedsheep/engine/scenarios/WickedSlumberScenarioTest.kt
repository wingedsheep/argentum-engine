package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.WickedSlumber
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wicked Slumber (MOM #84) — {3}{U} Instant, convoke.
 * "Tap up to two target creatures. Put a stun counter on either of them. Then put a stun counter
 * on either of them."
 *
 * The two stun counters are split at resolution among the targets, both allowed on one creature;
 * a lone surviving target takes both (ruling 2023-04-14).
 */
class WickedSlumberScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WickedSlumber)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun stun(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    fun cast(driver: GameTestDriver, card: EntityId, targets: List<EntityId>) =
        driver.submitSuccess(
            CastSpell(
                playerId = driver.player1,
                cardId = card,
                targets = targets.map { ChosenTarget.Permanent(it) },
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    test("two targets: both tapped, one stun counter on each") {
        val driver = newDriver()
        val card = driver.putCardInHand(driver.player1, "Wicked Slumber")
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.giveMana(driver.player1, Color.BLUE, 4)

        cast(driver, card, listOf(a, b))
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.totalAmount shouldBe 2
        driver.submitDecision(driver.player1, DistributionResponse(decision.id, mapOf(a to 1, b to 1)))

        driver.isTapped(a) shouldBe true
        driver.isTapped(b) shouldBe true
        stun(driver, a) shouldBe 1
        stun(driver, b) shouldBe 1
    }

    test("two targets: both stun counters may go on the same creature") {
        val driver = newDriver()
        val card = driver.putCardInHand(driver.player1, "Wicked Slumber")
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.giveMana(driver.player1, Color.BLUE, 4)

        cast(driver, card, listOf(a, b))
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        driver.submitDecision(driver.player1, DistributionResponse(decision.id, mapOf(a to 0, b to 2)))

        driver.isTapped(a) shouldBe true
        driver.isTapped(b) shouldBe true
        stun(driver, a) shouldBe 0
        stun(driver, b) shouldBe 2
    }

    test("one target left illegal: the remaining target gets both counters") {
        val driver = newDriver()
        val card = driver.putCardInHand(driver.player1, "Wicked Slumber")
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.giveMana(driver.player1, Color.BLUE, 4)

        cast(driver, card, listOf(a, b))
        driver.moveToGraveyard(a)
        driver.bothPass()

        driver.pendingDecision.shouldBeNull()
        driver.isTapped(b) shouldBe true
        stun(driver, b) shouldBe 2
        stun(driver, a) shouldBe 0
    }

    test("a single target takes both counters") {
        val driver = newDriver()
        val card = driver.putCardInHand(driver.player1, "Wicked Slumber")
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.giveMana(driver.player1, Color.BLUE, 4)

        cast(driver, card, listOf(a))
        driver.bothPass()

        driver.pendingDecision.shouldBeNull()
        driver.isTapped(a) shouldBe true
        stun(driver, a) shouldBe 2
    }

    test("convoke: creatures pay for the spell, and zero targets is legal") {
        val driver = newDriver()
        val card = driver.putCardInHand(driver.player1, "Wicked Slumber")
        val c1 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val c2 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val c3 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.giveMana(driver.player1, Color.BLUE, 1)

        driver.submitSuccess(
            CastSpell(
                playerId = driver.player1,
                cardId = card,
                targets = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = listOf(c1, c2, c3).associateWith { ConvokePayment(color = null) }
                ),
            )
        )
        driver.bothPass()

        driver.isTapped(c1) shouldBe true
        driver.isTapped(c2) shouldBe true
        driver.isTapped(c3) shouldBe true
        driver.getGraveyardCardNames(driver.player1).contains("Wicked Slumber") shouldBe true
    }
})
