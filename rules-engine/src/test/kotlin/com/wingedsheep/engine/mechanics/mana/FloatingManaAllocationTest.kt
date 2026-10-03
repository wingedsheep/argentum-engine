package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class FloatingManaAllocationTest : FunSpec({
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun entry(color: Color?, id: String, snow: Boolean = false) = RestrictedManaEntry(
        color, ManaRestriction.AnySpend, source = ManaSourceTag(EntityId("source-$id"), isSnow = snow),
        obligationIds = setOf(id))

    test("colored payment reserves tagged snow while using ordinary mana of that color") {
        val pool = ManaPool(green = 1, restrictedMana = listOf(entry(Color.GREEN, "snow", true)))
        for (cost in listOf("{G}{S}", "{S}{G}")) {
            pool.canPay(ManaCost.parse(cost), context) shouldBe true
            val paid = pool.pay(ManaCost.parse(cost), context)!!
            paid.isEmpty() shouldBe true
            paid.dischargedObligations shouldBe setOf("snow")
            pool.restrictedMana.size shouldBe 1
        }
    }
    test("hybrid reassignment lets both activations contribute despite generic competition") {
        val pool = ManaPool(restrictedMana = listOf(entry(Color.BLUE, "blue"),
            entry(Color.BLUE, "blue"), entry(Color.GREEN, "green")))
        val paid = pool.pay(ManaCost.parse("{G/U}{U}"), context)!!
        paid.dischargedObligations shouldBe setOf("blue", "green")
        paid.restrictedMana.single().obligationIds shouldBe setOf("blue")
    }
    test("mono hybrid may choose generic to discharge two activations") {
        val pool = ManaPool(restrictedMana = listOf(entry(Color.GREEN, "a"), entry(Color.BLUE, "b")))
        val paid = pool.pay(ManaCost.parse("{2/G}"), context)!!
        paid.dischargedObligations shouldBe setOf("a", "b")
        paid.isEmpty() shouldBe true
    }
    test("one mono hybrid prefers its cheaper colored alternative when coverage is equal") {
        val pool = ManaPool(restrictedMana = listOf(entry(Color.GREEN, "a"), entry(Color.GREEN, "a")))
        val paid = pool.pay(ManaCost.parse("{2/G}"), context)!!
        paid.restrictedMana.size shouldBe 1
    }
    test("snow bucket deductions preserve unused unrestricted snow exactly") {
        val pool = ManaPool(green = 2, snowMana = mapOf(Color.GREEN to 1),
            restrictedMana = listOf(entry(Color.BLUE, "blue")))
        val paid = pool.pay(ManaCost.parse("{G}{U}"), context)!!
        paid.green shouldBe 1
        paid.snowMana shouldBe mapOf(Color.GREEN to 1)
        val snowPaid = paid.allocateFloating(ManaCost.parse("{S}"), context)!!.pool
        snowPaid.green shouldBe 0
        snowPaid.snowMana shouldBe emptyMap()
    }
    test("restricted X competes with fixed substituted and snow pips using actual colors") {
        val pool = ManaPool(blue = 1, restrictedMana = listOf(entry(Color.RED, "red"),
            entry(Color.GREEN, "green", true)), spendingColors = mapOf(Color.BLUE to setOf(Color.RED)))
        val allocation = pool.allocateFloating(ManaCost.parse("{U}{S}{X}"), context, 1, setOf(Color.BLUE))!!
        allocation.pool.isEmpty() shouldBe true
        allocation.pool.dischargedObligations shouldBe setOf("red", "green")
        allocation.xSpentByColor shouldBe mapOf(Color.BLUE to 1)
        allocation.spent shouldBe ManaPool(blue = 1, red = 1, green = 1)
    }
    test("restrictions and colorless requirements remain strict") {
        val pool = ManaPool(restrictedMana = listOf(entry(Color.GREEN, "a"),
            entry(null, "b").copy(restriction = ManaRestriction.CreatureSpellsOnly)))
        pool.allocateFloating(ManaCost.parse("{C}"), context) shouldBe null
        pool.allocateFloating(ManaCost.parse("{G}{G}"), context) shouldBe null
    }
    test("colorless as any color matches colored and snow while X remains color restricted") {
        val pool = ManaPool(restrictedMana = listOf(entry(null, "a"), entry(null, "b", true)))
        pool.allocateFloating(ManaCost.parse("{U}{S}"), context.copy(colorlessAsAnyColor = true))!!
            .pool.dischargedObligations shouldBe setOf("a", "b")
        pool.allocateFloating(ManaCost.parse("{X}"), context.copy(colorlessAsAnyColor = true),
            1, setOf(Color.BLUE)) shouldBe null
    }
    test("previously discharged identities do not displace outstanding contributions") {
        val pool = ManaPool(restrictedMana = listOf(entry(Color.BLUE, "old"), entry(Color.BLUE, "new")),
            dischargedObligations = setOf("old"))
        pool.pay(ManaCost.parse("{U}"), context)!!.restrictedMana.single().obligationIds shouldBe setOf("old")
    }
    test("maximum contribution matching agrees with exhaustive small-pool payments") {
        // Independent enumeration catches residual reassignment and reward/tie-order mistakes.
        val random = Random(410042)
        val costs = listOf("{G}{S}", "{G/U}{U}", "{2}{G}", "{S}{S}", "{C}{G/U}", "{1}{U}{S}")
        repeat(600) {
            val entries = List(random.nextInt(1, 7)) { index ->
                val color = listOf(null, Color.GREEN, Color.BLUE)[random.nextInt(3)]
                entry(color, "activation-${index / 2}", random.nextBoolean())
            }
            val cost = ManaCost.parse(costs[random.nextInt(costs.size)])
            val symbols = cost.symbols.flatMap { if (it is ManaSymbol.Generic)
                List(it.amount) { ManaSymbol.Generic(1) } else listOf(it) }
            var best = -1
            fun enumerate(pip: Int, used: Set<Int>, ids: Set<String>) {
                if (pip == symbols.size) { best = maxOf(best, ids.size); return }
                entries.forEachIndexed { index, unit ->
                    if (index in used) return@forEachIndexed
                    val matches = when (val symbol = symbols[pip]) {
                        is ManaSymbol.Colored -> unit.color == symbol.color
                        is ManaSymbol.Colorless -> unit.color == null
                        is ManaSymbol.Hybrid -> unit.color == symbol.color1 || unit.color == symbol.color2
                        ManaSymbol.Snow -> unit.source!!.isSnow
                        is ManaSymbol.Generic -> true
                        else -> error("Unexpected test pip")
                    }
                    if (matches) enumerate(pip + 1, used + index, ids + unit.obligationIds)
                }
            }
            enumerate(0, emptySet(), emptySet())
            val allocation = ManaPool(restrictedMana = entries).allocateFloating(cost, context)
            (allocation?.pool?.dischargedObligations?.size ?: -1) shouldBe best
        }
    }
})
