package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CostManaValueSnapshotTest : FunSpec({
    val amounts = PredicateEvaluator(cardRegistry = null).amounts
    val json = Json { serializersModule = engineSerializersModule; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40))
    }

    test("captures mana value before a cost changes the card's characteristics") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val snapshot = captureEntitySnapshots(listOf(id), d.state).single()
        snapshot.manaValue shouldBe 2
        d.moveToGraveyard(id)
        d.replaceState(d.state.updateEntity(id) { c ->
            c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.parse("{9}")))
        })
        val ctx = EffectContext(null, d.player1, sacrificedPermanents = listOf(snapshot))
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(), ctx) shouldBe 2
        val restored = json.decodeFromString<EffectContext>(json.encodeToString(ctx))
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(), restored) shouldBe 2
    }
    test("a removed token still supplies its frozen mana value") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val snapshot = captureEntitySnapshots(listOf(id), d.state).single()
        val state = d.state.removeEntity(id)
        val ctx = EffectContext(null, d.player1, sacrificedPermanents = listOf(snapshot))
        amounts.evaluate(state, DynamicAmounts.sacrificedManaValue(), ctx) shouldBe 2
    }
    test("face-down capture stays zero after the card is revealed") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addComponent(id, FaceDownComponent)
        val snapshot = captureEntitySnapshots(listOf(id), d.state).single()
        snapshot.manaValue shouldBe 0
        d.moveToGraveyard(id)
        val ctx = EffectContext(null, d.player1, sacrificedPermanents = listOf(snapshot))
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(), ctx) shouldBe 0
    }
    test("X in a battlefield mana cost counts zero and transformed overrides survive capture") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(id) { c -> c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.parse("{X}{X}{B}"))) })
        captureEntitySnapshots(listOf(id), d.state).single().manaValue shouldBe 1
        d.replaceState(d.state.updateEntity(id) { c -> c.with(c.get<CardComponent>()!!.copy(manaValueOverride = 7)) })
        captureEntitySnapshots(listOf(id), d.state).single().manaValue shouldBe 7
    }
    test("a re-entered object does not replace the original cost snapshot") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val snapshot = captureEntitySnapshots(listOf(id), d.state).single()
        d.moveToGraveyard(id)
        d.replaceState(d.state.moveToZone(id, ZoneKey(d.player1, Zone.GRAVEYARD), ZoneKey(d.player1, Zone.BATTLEFIELD)))
        d.replaceState(d.state.updateEntity(id) { c -> c
            .with(BattlefieldEntryTimestampComponent(999999))
            .with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.parse("{8}"))) })
        val ctx = EffectContext(null, d.player1, sacrificedPermanents = listOf(snapshot))
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(), ctx) shouldBe 2
    }
    test("indexed costs keep their own values and missing slots read zero") {
        val d = setup()
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val snapshots = captureEntitySnapshots(listOf(first, second), d.state)
        d.moveToGraveyard(first)
        d.moveToGraveyard(second)
        val ctx = EffectContext(null, d.player1, sacrificedPermanents = snapshots)
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(0), ctx) shouldBe 2
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(1), ctx) shouldBe 4
        amounts.evaluate(d.state, DynamicAmounts.sacrificedManaValue(2), ctx) shouldBe 0
    }
    for (reference in listOf(EffectTarget.Self, EffectTarget.TappedAsCost(0), EffectTarget.PipelineTarget("chosen"))) {
        test("mana-value last-known reads also work for $reference") {
            val d = setup()
            val id = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val snapshot = captureEntitySnapshots(listOf(id), d.state).single()
            d.moveToGraveyard(id)
            d.replaceState(d.state.updateEntity(id) { c -> c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.parse("{9}"))) })
            val ctx = EffectContext(id, d.player1, lastKnownSourceSnapshot = snapshot,
                tappedPermanents = listOf(id), tappedEntitySnapshots = listOf(snapshot),
                chosenEntitySnapshots = listOf(snapshot), pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id))))
            amounts.evaluate(d.state, DynamicAmount.EntityProperty(reference, EntityNumericProperty.ManaValue), ctx) shouldBe 2
        }
    }

    test("activated sacrifice costs retain mana value across a resolution decision") {
        val altar = card("Cost Value Altar") {
            manaCost = "{2}"
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Sacrifice(GameObjectFilter.Creature)
                effect = Effects.May(Effects.AddMana(Color.BLACK, DynamicAmounts.sacrificedManaValue()))
            }
        }
        val d = setup()
        d.registerCard(altar)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, altar.name)
        val victim = d.putCreatureOnBattlefield(p, "Hill Giant")
        d.submit(ActivateAbility(p, source, altar.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim)))).error shouldBe null
        d.bothPass()
        d.pendingDecision shouldNotBe null
        d.replaceState(d.state.updateEntity(victim) { c -> c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.ZERO)) })
        d.submitYesNo(p, true).error shouldBe null
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.black shouldBe 4
    }
    test("a self-sacrificing source uses the same frozen mana-value read") {
        val sourceCard = card("Self Value Altar") {
            manaCost = "{3}"
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.SacrificeSelf
                effect = Effects.May(Effects.AddMana(Color.BLACK, DynamicAmounts.sourceManaValue()))
            }
        }
        val d = setup()
        d.registerCard(sourceCard)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, sourceCard.name)
        d.submit(ActivateAbility(p, source, sourceCard.activatedAbilities.single().id)).error shouldBe null
        d.bothPass()
        d.replaceState(d.state.updateEntity(source) { c -> c.with(c.get<CardComponent>()!!.copy(manaCost = ManaCost.ZERO)) })
        d.submitYesNo(p, true).error shouldBe null
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.black shouldBe 3
    }

    for (paymentKind in listOf("exact", "duplicate", "excess")) {
        test("fixed multiple sacrifice cost validates $paymentKind payment") {
            val spell = card("Two Victim Spell") {
                manaCost = "{0}"
                typeLine = "Sorcery"
                additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature, count = 2))
                spell { effect = Effects.GainLife(1) }
            }
            val d = setup()
            d.registerCard(spell)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val p = d.activePlayer!!
            val first = d.putCreatureOnBattlefield(p, "Grizzly Bears")
            val second = d.putCreatureOnBattlefield(p, "Hill Giant")
            val third = d.putCreatureOnBattlefield(p, "Grizzly Bears")
            val spellId = d.putCardInHand(p, spell.name)
            val selected = when (paymentKind) {
                "exact" -> listOf(first, second)
                "duplicate" -> listOf(first, first)
                else -> listOf(first, second, third)
            }
            val before = d.state
            val result = d.submit(CastSpell(p, spellId,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = selected)))
            if (paymentKind == "exact") {
                result.error shouldBe null
                d.state.getGraveyard(p).containsAll(selected) shouldBe true
            } else {
                result.error shouldNotBe null
                d.state shouldBe before
            }
        }
    }
    test("legacy snapshots without mana value still decode") {
        json.decodeFromString<EntitySnapshot>("""{"entityId":"legacy"}""").manaValue shouldBe null
    }
})
