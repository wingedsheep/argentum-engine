package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EvisceratorsInsight
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Eviscerator's Insight {1}{B} — Instant.
 * "As an additional cost to cast this spell, sacrifice an artifact or creature.
 *  Draw two cards. Flashback {4}{B}"
 */
class EvisceratorsInsightScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EvisceratorsInsight, Bonesplitter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sacrificing an artifact draws two cards") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val insight = driver.putCardInHand(me, "Eviscerator's Insight")
        val bonesplitter = driver.putPermanentOnBattlefield(me, "Bonesplitter")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }
        val handBefore = driver.getHandSize(me)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = insight,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bonesplitter)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).outcome shouldBe Outcome.Done
        driver.state.getBattlefield().contains(bonesplitter) shouldBe false
        driver.bothPass()

        driver.getHandSize(me) shouldBe handBefore - 1 + 2
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(insight) shouldBe true
    }

    test("artifacts and creatures are valid sacrifices, lands are not") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCardInHand(me, "Eviscerator's Insight")
        val bonesplitter = driver.putPermanentOnBattlefield(me, "Bonesplitter")
        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        val cast = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, me)
            .firstOrNull { it.actionType == "CastSpell" && it.additionalCostInfo != null }
            .shouldNotBeNull()
        cast.additionalCostInfo!!.validSacrificeTargets shouldContainExactlyInAnyOrder listOf(bonesplitter, bear)
    }

    test("cannot be cast without an artifact or creature to sacrifice") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCardInHand(me, "Eviscerator's Insight")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        val cast = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, me)
            .firstOrNull { it.actionType == "CastSpell" }
        (cast == null || !cast.affordable) shouldBe true
    }

    test("flashback still requires the sacrifice, draws two, and exiles the card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val insight = driver.putCardInGraveyard(me, "Eviscerator's Insight")
        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        repeat(5) { driver.putLandOnBattlefield(me, "Swamp") }
        val handBefore = driver.getHandSize(me)

        val flashback = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, me)
            .firstOrNull { it.actionType == "CastWithFlashback" }
            .shouldNotBeNull()
        flashback.additionalCostInfo.shouldNotBeNull().validSacrificeTargets shouldContainExactlyInAnyOrder listOf(bear)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = insight,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getBattlefield().contains(bear) shouldBe false
        driver.getHandSize(me) shouldBe handBefore + 2
        driver.state.getZone(ZoneKey(me, Zone.EXILE)).contains(insight) shouldBe true
    }
})
