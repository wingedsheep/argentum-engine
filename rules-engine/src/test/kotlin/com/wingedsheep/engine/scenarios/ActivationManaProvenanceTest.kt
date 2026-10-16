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

    test("distinct colors spent to activate read the ability's own payment, not the source's cast") {
        fun reader(payment: ManaPayment) = card("Color reader ${payment.name}") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Mana("{3}")
                effect = Effects.GainLife(DynamicAmounts.colorsOfManaSpent(payment))
            }
        }
        val activation = reader(ManaPayment.ACTIVATION)
        val cast = reader(ManaPayment.CAST)
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(activation, cast))
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        fun activate(def: com.wingedsheep.sdk.model.CardDefinition, vararg colors: Color?): Int {
            val permanent = d.putPermanentOnBattlefield(player, def.name)
            colors.forEach { if (it == null) d.giveColorlessMana(player, 1) else d.giveMana(player, it) }
            val life = d.getLifeTotal(player)
            d.submit(ActivateAbility(player, permanent, def.script.activatedAbilities.single().id)).error shouldBe null
            d.bothPass()
            return d.getLifeTotal(player) - life
        }
        activate(activation, Color.WHITE, Color.BLUE, Color.BLACK) shouldBe 3
        activate(activation, Color.RED, Color.RED, null) shouldBe 1
        activate(activation, null, null, null) shouldBe 0
        // CAST still reads the (absent) cast record of a permanent that was never cast.
        activate(cast, Color.WHITE, Color.BLUE, Color.BLACK) shouldBe 0
    }

    test("a departed source's snow snapshot marks floating mana and preserves its payment provenance") {
        val player = EntityId("snow-player")
        val before = com.wingedsheep.engine.state.GameState().withEntity(player,
            com.wingedsheep.engine.state.ComponentContainer().with(ManaPoolComponent()))
        val after = before.updateEntity(player) { it.with(ManaPoolComponent(white = 1)) }
        val context = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = source, controllerId = player,
            lastKnownSourceSnapshot = com.wingedsheep.engine.state.components.stack.EntitySnapshot(
                entityId = source, subtypes = setOf("Treasure"), supertypes = setOf("SNOW"),
                typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)),
            ),
        )
        val tracker = com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker
        val tagged = tracker.tagAddedMana(after, player, source, 1, Color.WHITE, context)
        val marked = tracker.markSnowProduction(before, tagged, source, player)
        val component = marked.getEntity(player)!!.get<ManaPoolComponent>()!!
        component.snowMana shouldBe mapOf(Color.WHITE to 1)
        val pool = ManaPool(white = component.white, snowMana = component.snowMana,
            manaUnits = component.manaUnits)
        val paid = pool.pay(ManaCost.parse("{1}"))!!
        SpentMana.between(pool, paid).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1)
        paid.manaUnits shouldBe emptyList()
    }
    test("a departed source's non-snow snapshot takes precedence over printed snow") {
        val player = EntityId("plain-player")
        val source = EntityId("printed-snow-source")
        val before = com.wingedsheep.engine.state.GameState()
            .withEntity(player, com.wingedsheep.engine.state.ComponentContainer().with(ManaPoolComponent()))
            .withEntity(source, com.wingedsheep.engine.state.ComponentContainer().with(
                com.wingedsheep.engine.state.components.identity.CardComponent(
                    cardDefinitionId = "snow-source", name = "Snow source", ownerId = player,
                    manaCost = ManaCost.parse("{0}"),
                    typeLine = TypeLine(supertypes = setOf(Supertype.SNOW), cardTypes = setOf(CardType.ARTIFACT)),
                )))
        val after = before.updateEntity(player) { it.with(ManaPoolComponent(white = 1)) }
        val context = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = source, controllerId = player,
            lastKnownSourceSnapshot = com.wingedsheep.engine.state.components.stack.EntitySnapshot(
                entityId = source, subtypes = setOf("Treasure"),
                typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)),
            ),
        )
        val tracker = com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker
        val tagged = tracker.tagAddedMana(after, player, source, 1, Color.WHITE, context)
        val marked = tracker.markSnowProduction(before, tagged, source, player)
        val component = marked.getEntity(player)!!.get<ManaPoolComponent>()!!
        component.snowMana shouldBe emptyMap()
        val pool = ManaPool(white = component.white, snowMana = component.snowMana,
            manaUnits = component.manaUnits)
        val paid = pool.pay(ManaCost.parse("{1}"))!!
        SpentMana.between(pool, paid).provenance.bySubtype shouldBe mapOf(Subtype.TREASURE to 1)
        paid.manaUnits shouldBe emptyList()
    }
    test("an off-stack mana ability can read its own Treasure payment") {
        val reader = card("Mana payment reader") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Mana("{1}")
                effect = Effects.AddMana(Color.BLUE,
                    DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION))
                manaAbility = true
            }
        }
        val producer = card("Payment Treasure source") {
            typeLine = "Artifact — Treasure"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.WHITE, 1); manaAbility = true }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(reader, producer))
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(player, "Payment Treasure source")
        val r = d.putPermanentOnBattlefield(player, "Mana payment reader")
        d.submit(ActivateAbility(player, r, reader.script.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.Explicit(listOf(source)))).outcome shouldBe Outcome.Done
        d.state.getEntity(player)!!.get<ManaPoolComponent>()!!.blue shouldBe 1
        d.state.stack.isEmpty() shouldBe true
    }

})
