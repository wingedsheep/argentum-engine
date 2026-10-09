package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ManaPoolChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ColorlessManaExpiryTest : FunSpec({
    val player = EntityId("player")
    val source = EntityId("source")
    val amounts = PredicateEvaluator(null).conditions.amounts
    val executor = AddColorlessManaExecutor(amounts)
    val context = EffectContext(sourceId = source, controllerId = player)
    fun produce(effect: AddColorlessManaEffect, initial: ManaPoolComponent = ManaPoolComponent()): ManaPoolComponent {
        val state = GameState(entities = mapOf(player to ComponentContainer().with(initial)))
        val result = executor.execute(state, effect, context)
        result.events shouldBe if (amounts.evaluate(state, effect.amount, context) > 0) listOf(ManaPoolChangedEvent(player)) else emptyList()
        state.getEntity(player)!!.get<ManaPoolComponent>() shouldBe initial
        return result.newState.getEntity(player)!!.get<ManaPoolComponent>()!!
    }
    fun kept(n: Int) = AddColorlessManaEffect(n, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN)

    test("only the tagged production survives boundaries including end of combat") {
        val pool = produce(kept(8), ManaPoolComponent(colorless = 3, red = 1))
        val after = pool.emptyAtBoundary(null, emptySet()).clearExpired(ManaExpiry.END_OF_COMBAT)
        after.total shouldBe 8
        after.restrictedMana.all { it.color == null && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
        after.restrictedMana.all { it.source?.sourceId == source } shouldBe true
        after.expireTurnKeptMana().emptyAtBoundary(null, emptySet()).isEmpty shouldBe true
    }
    test("ordinary colourless production keeps its old behaviour") {
        val pool = produce(AddColorlessManaEffect(2))
        pool.colorless shouldBe 2
        pool.restrictedMana shouldBe emptyList()
        pool.emptyAtBoundary(null, emptySet()).isEmpty shouldBe true
    }
    test("combat expiry is independent from turn expiry") {
        val pool = produce(AddColorlessManaEffect(3, expiry = ManaExpiry.END_OF_COMBAT))
        pool.emptyAtBoundary(null, emptySet()).total shouldBe 3
        pool.clearExpired(ManaExpiry.END_OF_COMBAT).isEmpty shouldBe true
    }
    test("retained colourless mana pays explicit colourless and generic costs with partial retention") {
        val pool = produce(kept(8))
        val paid = ManaPool(restrictedMana = pool.restrictedMana).pay(ManaCost.parse("{2}{C}{C}"), SpellPaymentContext())!!
        paid.restrictedMana.size shouldBe 4
        paid.restrictedMana.all { it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
        ManaPool(restrictedMana = pool.restrictedMana).canPay(ManaCost.parse("{R}"), SpellPaymentContext()) shouldBe false
    }
    test("restrictions riders and expiry coexist through payment") {
        val riders = setOf(ManaSpellRider.MakesSpellUncounterable())
        val pool = produce(AddColorlessManaEffect(DynamicAmount.Fixed(3), ManaRestriction.CreatureSpellsOnly,
            riders, ManaExpiry.KEPT_UNTIL_END_OF_TURN))
        pool.restrictedMana.all { it.riders == riders && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
        val payable = ManaPool(restrictedMana = pool.restrictedMana)
        payable.canPay(ManaCost.parse("{C}"), SpellPaymentContext(isInstantOrSorcery = true)) shouldBe false
        payable.pay(ManaCost.parse("{C}"), SpellPaymentContext(isCreature = true))!!.restrictedMana.size shouldBe 2
    }
    test("contextless costs spend retained colourless mana and preserve spell-only entries") {
        val ineligible = produce(AddColorlessManaEffect(2, ManaRestriction.CreatureSpellsOnly)).restrictedMana
        val retained = produce(kept(5)).restrictedMana
        val pool = ManaPool(restrictedMana = ineligible + retained)
        val cost = ManaCost.parse("{2}{C}{C}")

        pool.canPay(cost) shouldBe true
        val paid = pool.pay(cost)!!
        paid.restrictedMana.filter { it.restriction == ManaRestriction.CreatureSpellsOnly } shouldBe ineligible
        paid.restrictedMana.filter { it.restriction == ManaRestriction.AnySpend } shouldBe retained.take(1)
        pool.canPay(ManaCost.parse("{4}{C}{C}")) shouldBe false
        pool.pay(ManaCost.parse("{4}{C}{C}")) shouldBe null
        pool.restrictedMana shouldBe ineligible + retained
    }
    test("contextless partial payment reports unpaid generic mana without consuming spell-only mana") {
        val ineligible = produce(AddColorlessManaEffect(2, ManaRestriction.CreatureSpellsOnly)).restrictedMana
        val pool = ManaPool(restrictedMana = ineligible + produce(kept(3)).restrictedMana)
        val paid = pool.payPartial(ManaCost.parse("{3}{C}{C}"))

        paid.remainingCost shouldBe ManaCost.parse("{2}")
        paid.manaSpent.colorless shouldBe 3
        paid.newPool.restrictedMana shouldBe ineligible
        paid.newPool.canPay(ManaCost.parse("{C}")) shouldBe false
        paid.newPool.payPartial(ManaCost.parse("{C}")).remainingCost shouldBe ManaCost.parse("{C}")
    }
    test("contextless coloured and generic costs also spend retained AnySpend entries") {
        val retained = produce(kept(3)).restrictedMana.map { it.copy(color = Color.RED) }
        val pool = ManaPool(restrictedMana = retained)
        val cost = ManaCost.parse("{1}{R}")

        pool.canPay(cost) shouldBe true
        pool.pay(cost)!!.restrictedMana shouldBe retained.take(1)
        val partial = pool.payPartial(cost)
        partial.remainingCost.isEmpty() shouldBe true
        partial.manaSpent.red shouldBe 2
        partial.newPool.restrictedMana shouldBe retained.take(1)
        pool.canPay(ManaCost.parse("{C}")) shouldBe false
    }
    test("contextless X coverage counts only eligible mana of an allowed colour") {
        val retained = produce(kept(3)).restrictedMana
        val red = retained.take(2).map { it.copy(color = Color.RED) }
        val ineligible = produce(AddColorlessManaEffect(4, ManaRestriction.CreatureSpellsOnly)).restrictedMana
        val pool = ManaPool(restrictedMana = retained + red + ineligible)

        pool.xCoverage(10, emptySet(), null) shouldBe 5
        pool.xCoverage(4, emptySet(), null) shouldBe 4
        pool.xCoverage(10, setOf(Color.RED), null) shouldBe 2
        pool.xCoverage(10, setOf(Color.GREEN), null) shouldBe 0
    }
    test("zero and negative amounts do not tag or change existing mana") {
        val initial = ManaPoolComponent(colorless = 2)
        produce(kept(0), initial) shouldBe initial
        produce(AddColorlessManaEffect(
            DynamicAmount.Subtract(DynamicAmount.Fixed(0), DynamicAmount.Fixed(1)),
            expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN
        ), initial) shouldBe initial
    }
    test("loss replacement cannot convert kept mana before its retention expires") {
        val pool = produce(kept(2), ManaPoolComponent(colorless = 1))
        val after = pool.emptyAtBoundary(Color.BLACK, emptySet())
        after.black shouldBe 1
        after.restrictedMana.size shouldBe 2
        val cleanup = after.expireTurnKeptMana().emptyAtBoundary(Color.BLACK, emptySet())
        cleanup.total shouldBe 3
        cleanup.restrictedMana.all { it.color == Color.BLACK && it.expiry == ManaExpiry.END_OF_TURN } shouldBe true
    }
})
