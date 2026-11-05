package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * `Costs.additional.PutCountersOnPermanent` — "As an additional cost to cast this spell, put a -1/-1
 * counter on a creature you control" (Lethal Sting). The caster picks one creature they control that
 * can take the counter (CR 601.2h pays it after targets are chosen, so the target may be the same
 * creature); with no such creature the spell can't be cast. The placement is not blight, so a spell
 * reading "if this spell's blight cost was paid" sees it as unpaid.
 */
class PutCountersOnPermanentSpellCostTest : FunSpec({

    val sting = card("Test Sting") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.PutCountersOnPermanent(CounterType.MINUS_ONE_MINUS_ONE))
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t) then Effects.If(BlightWasPaid, then = Effects.DrawCards(1))
        }
    }

    data class Board(val driver: GameTestDriver, val caster: EntityId, val spell: EntityId)

    fun board(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(sting))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        driver.putLandOnBattlefield(caster, "Swamp")
        return Board(driver, caster, driver.putCardInHand(caster, "Test Sting"))
    }

    fun Board.castAction(target: EntityId, chosen: List<EntityId>) = CastSpell(
        playerId = caster,
        cardId = spell,
        targets = listOf(ChosenTarget.Permanent(target)),
        paymentStrategy = PaymentStrategy.AutoPay,
        additionalCostPayment = AdditionalCostPayment(counterPlacementTargets = chosen),
    )

    fun Board.resolve() {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    fun Board.minusCounters(id: EntityId) =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    test("the cast offers only creatures the caster controls, on the PutCounters picker") {
        val b = board()
        val mine = List(2) { b.driver.putCreatureOnBattlefield(b.caster, "Grizzly Bears") }
        b.driver.putCreatureOnBattlefield(b.driver.getOpponent(b.caster), "Hill Giant")
        val info = b.driver.legalActions(b.caster)
            .single { (it.action as? CastSpell)?.cardId == b.spell && it.actionType == "CastSpell" }
            .additionalCostInfo!!
        info.costType shouldBe "PutCounters"
        info.description shouldBe "creature to put a -1/-1 counter on"
        info.validCounterPlacementTargets shouldContainExactlyInAnyOrder mine
    }

    test("with no creature to take the counter the spell can't be cast") {
        val b = board()
        val theirs = b.driver.putCreatureOnBattlefield(b.driver.getOpponent(b.caster), "Hill Giant")
        b.driver.legalActions(b.caster).none { (it.action as? CastSpell)?.cardId == b.spell } shouldBe true
        b.driver.submitExpectFailure(b.castAction(theirs, listOf(theirs)))
    }

    test("paying puts the counter on the chosen creature and the spell resolves, without counting as blight") {
        val b = board()
        val bear = b.driver.putCreatureOnBattlefield(b.caster, "Grizzly Bears")
        val giant = b.driver.putCreatureOnBattlefield(b.driver.getOpponent(b.caster), "Hill Giant")
        val handBefore = b.driver.getHand(b.caster).size
        b.driver.submitSuccess(b.castAction(giant, listOf(bear)))
        b.minusCounters(bear) shouldBe 1
        b.resolve()
        (giant in b.driver.state.getBattlefield()) shouldBe false
        (bear in b.driver.state.getBattlefield()) shouldBe true
        // The spell left the hand; BlightWasPaid stayed false, so nothing was drawn.
        b.driver.getHand(b.caster).size shouldBe handBefore - 1
    }

    test("the counter may go on the creature the spell targets") {
        val b = board()
        val bear = b.driver.putCreatureOnBattlefield(b.caster, "Grizzly Bears")
        b.driver.submitSuccess(b.castAction(bear, listOf(bear)))
        b.minusCounters(bear) shouldBe 1
        b.resolve()
        (bear in b.driver.state.getBattlefield()) shouldBe false
    }

    test("an opponent's creature, or no choice at all, can't pay the cost") {
        val b = board()
        val bear = b.driver.putCreatureOnBattlefield(b.caster, "Grizzly Bears")
        val giant = b.driver.putCreatureOnBattlefield(b.driver.getOpponent(b.caster), "Hill Giant")
        b.driver.submitExpectFailure(b.castAction(giant, listOf(giant)))
        b.driver.submitExpectFailure(b.castAction(giant, emptyList()))
        b.minusCounters(giant) shouldBe 0
        b.minusCounters(bear) shouldBe 0
    }
})
