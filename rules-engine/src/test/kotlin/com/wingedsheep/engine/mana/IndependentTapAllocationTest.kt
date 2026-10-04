package com.wingedsheep.engine.mana

import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IndependentTapAllocationTest : FunSpec({
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun source(id: String, color: Color, amount: Int = 1) = ManaSource(
        EntityId(id), id, setOf(color), manaAmount = amount, exactIndependentTap = true)
    fun pays(cost: String, sources: List<ManaSource>, pool: ManaPool = ManaPool(), pending: Set<String> = emptySet(),
             x: Int = 0, xColors: Set<Color> = emptySet()) =
        canPayWithIndependentTaps(pool, pending, sources, ManaCost.parse(cost), context, x, xColors)

    test("a source selects one production kind instead of producing its aggregate colors") {
        val dual = source("dual", Color.GREEN).copy(producesColors = setOf(Color.GREEN, Color.BLUE))
        pays("{G}{U}", listOf(dual)) shouldBe false
        pays("{G}{U}", listOf(dual, source("green", Color.GREEN))) shouldBe true
    }
    test("wrong-color base production cannot contribute using its triggered bonus") {
        val bonus = source("bonus", Color.RED).copy(bonusManaPerTap = 1, bonusManaColor = Color.BLUE)
        pays("{U}", listOf(bonus)) shouldBe false
    }
    test("floating bonus mana can combine with a contributing independent activation") {
        pays("{R}{U}", listOf(source("red", Color.RED)), ManaPool(blue = 1)) shouldBe true
    }
    test("two independent multi-mana sources may contribute one each and retain excess") {
        pays("{G}{U}", listOf(source("g", Color.GREEN, 2), source("u", Color.BLUE, 2))) shouldBe true
    }
    test("snow and colored pips share one allocation across pool and sources") {
        val snow = source("snow", Color.GREEN).copy(isSnow = true)
        pays("{G}{S}", listOf(snow), ManaPool(green = 1)) shouldBe true
        pays("{G}{S}", listOf(source("plain", Color.GREEN)), ManaPool(green = 1)) shouldBe false
    }
    test("actual X colors and fixed hybrid costs compete together") {
        val pool = ManaPool(restrictedMana = listOf(
            RestrictedManaEntry(Color.BLUE, ManaRestriction.AnySpend, obligationIds = setOf("blue"))))
        pays("{G/U}{X}", listOf(source("green", Color.GREEN)), pool, setOf("blue"), 1, setOf(Color.BLUE)) shouldBe true
        pays("{G/U}{X}", listOf(source("red", Color.RED)), pool, setOf("blue"), 1, setOf(Color.BLUE)) shouldBe false
    }
    test("mono hybrid can use the larger alternative for two contributions") {
        val pool = ManaPool(restrictedMana = listOf(
            RestrictedManaEntry(Color.BLUE, ManaRestriction.AnySpend, obligationIds = setOf("blue"))))
        pays("{2/G}", listOf(source("green", Color.GREEN)), pool, setOf("blue")) shouldBe true
    }
    test("source restriction survives planned production") {
        val restricted = source("creature", Color.GREEN).copy(restriction = ManaRestriction.CreatureSpellsOnly)
        pays("{G}", listOf(restricted)) shouldBe false
    }
    test("unproved sources cannot enter an exact plan") {
        pays("{G}", listOf(source("unknown", Color.GREEN).copy(exactIndependentTap = false))) shouldBe false
    }
    test("large impossible-color boards fail without subset expansion") {
        pays("{U}{20}", List(30) { source("g$it", Color.GREEN) }) shouldBe false
    }
    test("selected sources share a canonical memo shape without merging their identities") {
        val sources = List(12) { source("g$it", Color.GREEN, 2) }
        pays("{12}", sources) shouldBe true
        pays("{25}", sources) shouldBe false
    }
    test("several identities on one unit do not consume several payment slots") {
        val pool = ManaPool(restrictedMana = listOf(RestrictedManaEntry(Color.BLUE,
            ManaRestriction.AnySpend, obligationIds = setOf("one", "two"))))
        pays("{G}{U}", listOf(source("green", Color.GREEN)), pool, setOf("one", "two")) shouldBe true
    }
    test("many flexible sources prune kinds without a remaining contribution slot") {
        val flexible = List(25) { source("five$it", Color.GREEN).copy(producesColors = Color.entries.toSet()) }
        pays("{G}".repeat(13) + "{U}".repeat(12), flexible) shouldBe true
    }

})
