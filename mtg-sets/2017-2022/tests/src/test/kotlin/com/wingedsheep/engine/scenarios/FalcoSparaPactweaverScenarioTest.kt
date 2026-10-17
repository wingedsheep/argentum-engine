package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.snc.cards.FalcoSparaPactweaver
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Falco Spara, Pactweaver: "You may cast spells from the top of your library by removing a counter
 * from a creature you control in addition to paying their other costs."
 */
class FalcoSparaPactweaverScenarioTest : FunSpec({

    /** Falco + a Grizzly Bears carrying [bearCounters] +1/+1 counters, two Forests, [top] on top. */
    fun setup(bearCounters: Int, top: String = "Grizzly Bears"): Triple<EnumerationTestDriver, EntityId, EntityId> {
        val driver = setupP1(
            battlefield = listOf("Falco Spara, Pactweaver", "Grizzly Bears", "Forest", "Forest"),
            extraSetCards = listOf(FalcoSparaPactweaver),
        )
        val p1 = driver.player1
        val state = driver.game.state
        val bears = state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
        if (bearCounters > 0) {
            driver.game.replaceState(state.updateEntity(bears) { c ->
                c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to bearCounters)))
            })
        }
        val topId = driver.game.putCardOnTopOfLibrary(p1, top)
        return Triple(driver, bears, topId)
    }

    test("the top card is castable, owing a counter removed from a creature you control") {
        val (driver, _, topId) = setup(bearCounters = 1)

        val casts = driver.enumerateFor(driver.player1).castActions()
            .filter { (it.action as CastSpell).cardId == topId }
        casts shouldHaveSize 1
        val cast = casts.single()
        cast.sourceZone shouldBe "LIBRARY"
        cast.affordable shouldBe true
        cast.additionalCostInfo shouldNotBe null
        cast.additionalCostInfo!!.costType shouldBe "RemoveCounters"
    }

    test("casting from the top removes the counter and pays the mana cost") {
        val (driver, bears, topId) = setup(bearCounters = 2)
        val p1 = driver.player1

        val result = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = topId,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(DistributedCounterRemoval(bears, "+1/+1", 1))
                )
            )
        )
        result.outcome shouldBe Outcome.Done

        driver.game.state.getEntity(bears)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.game.state.getLibrary(p1).firstOrNull() shouldNotBe topId
    }

    test("without a counter on any creature you control the top card isn't castable") {
        val (driver, _, topId) = setup(bearCounters = 0)

        driver.enumerateFor(driver.player1).castActions()
            .filter { (it.action as CastSpell).cardId == topId }
            .shouldBeEmpty()

        val result = driver.game.submit(
            CastSpell(playerId = driver.player1, cardId = topId, paymentStrategy = PaymentStrategy.AutoPay)
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("a land on top can't be played — Falco only lets you cast spells") {
        val (driver, _, topId) = setup(bearCounters = 1, top = "Forest")

        driver.enumerateFor(driver.player1)
            .filter { it.actionType == "PlayLand" && it.sourceZone == "LIBRARY" }
            .shouldBeEmpty()
        driver.game.state.getLibrary(driver.player1).first() shouldBe topId
    }
})
