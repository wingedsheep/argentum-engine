package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.*
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.ManaPayment
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ActivationManaProvenanceTest : FunSpec({
    val source = EntityId("treasure-source")
    val tag = ManaSourceTag(source, setOf(Subtype.TREASURE), setOf(CardType.ARTIFACT))
    test("a colored cost cannot consume provenance of another color") {
        val before = ManaPool(white = 2, red = 2, manaUnits = List(2) { ManaUnit(Color.RED, tag) })
        val after = before.pay(ManaCost.parse("{W}{W}"))!!
        SpentMana.between(before, after).provenance.bySubtype shouldBe emptyMap()
        after.manaUnits shouldBe before.manaUnits
    }
    test("generic payment consumes only the units used, leaving multi-mana excess") {
        val before = ManaPool(colorless = 3, manaUnits = List(3) { ManaUnit(null, tag) })
        val after = before.pay(ManaCost.parse("{2}"))!!
        SpentMana.between(before, after).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 2)
        after.manaUnits.size shouldBe 1
        SpentMana.between(after, after.spendColorless()!!).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1)
    }
    test("subtypes on one producing unit count independently without duplicating the mana") {
        val multi = tag.copy(subtypes = setOf(Subtype.TREASURE, Subtype.CAVE))
        val before = ManaPool(blue = 1, manaUnits = listOf(ManaUnit(Color.BLUE, multi)))
        val spent = SpentMana.between(before, before.spend(Color.BLUE)!!)
        spent.total shouldBe 1
        spent.provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1, Subtype.CAVE to 1)
    }
    test("restricted and unrestricted consumed units contribute exactly once") {
        val before = ManaPool(white = 1, manaUnits = listOf(ManaUnit(Color.WHITE, tag)),
            restrictedMana = listOf(RestrictedManaEntry(Color.RED, ManaRestriction.AnySpend, source = tag)))
        val spent = SpentMana.between(before, before.pay(ManaCost.parse("{2}"))!!)
        spent.total shouldBe 2
        spent.provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 2)
    }
    test("snow payments select the snow source and ordinary payments preserve it") {
        val snow = tag.copy(isSnow = true)
        val before = ManaPool(white = 2, snowMana = mapOf(Color.WHITE to 1),
            manaUnits = listOf(ManaUnit(Color.WHITE, snow), ManaUnit(Color.WHITE, tag.copy(subtypes = emptySet()))))
        val normal = before.spend(Color.WHITE)!!
        SpentMana.between(before, normal).provenance.bySubtype shouldBe emptyMap()
        val snowPaid = before.spendSnow()!!.first
        SpentMana.between(before, snowPaid).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1)
        snowPaid.manaUnits shouldBe listOf(ManaUnit(Color.WHITE, tag.copy(subtypes = emptySet())))
    }
    test("exact scoped allocation carries the same consumed snapshot as ordinary payment") {
        val before = ManaPool(red = 2, manaUnits = List(2) { ManaUnit(Color.RED, tag) })
        val allocation = before.allocateFloating(ManaCost.parse("{1}"), null)!!
        SpentMana.between(before, allocation.pool).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1)
    }
    test("serializing a payment and its remaining pool retains source and color provenance") {
        val before = ManaPool(red = 2, manaUnits = List(2) { ManaUnit(Color.RED, tag) })
        val after = before.spend(Color.RED)!!
        val spent = SpentMana.between(before, after)
        Json.decodeFromString<SpentMana>(Json.encodeToString(spent)) shouldBe spent
        Json.decodeFromString<ManaPool>(Json.encodeToString(after)) shouldBe after
    }
    test("retained and converted mana keeps source metadata; emptying deletes it") {
        val before = ManaPoolComponent(red = 2, manaUnits = List(2) { ManaUnit(Color.RED, tag) })
        before.emptyAtBoundary(null, setOf(Color.RED)).manaUnits shouldBe before.manaUnits
        before.emptyAtBoundary(Color.BLUE, emptySet()).manaUnits shouldBe List(2) { ManaUnit(Color.BLUE, tag) }
        before.empty().manaUnits shouldBe emptyList()
    }
    test("an X activation retains its payment across a resolution choice") {
        val reader = card("Payment reader") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Mana("{X}")
                effect = Effects.May(Effects.GainLife(0)) then Effects.GainLife(
                    DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION))
            }
        }
        val producer = card("Treasure mana source") {
            typeLine = "Artifact — Treasure"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.WHITE, 3); manaAbility = true }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(reader, producer))
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(player, "Treasure mana source")
        val r = d.putPermanentOnBattlefield(player, "Payment reader")
        d.submit(ActivateAbility(player, r, reader.script.activatedAbilities.single().id,
            xValue = 2, paymentStrategy = PaymentStrategy.Explicit(listOf(source)))).outcome shouldBe Outcome.Done
        d.bothPass().outcome::class shouldBe Outcome.Paused::class
        val life = d.getLifeTotal(player)
        d.submitYesNo(player, false).outcome shouldBe Outcome.Done
        d.getLifeTotal(player) shouldBe life + 2
        d.state.getEntity(player)!!.get<ManaPoolComponent>()!!.white shouldBe 1
    }

})
