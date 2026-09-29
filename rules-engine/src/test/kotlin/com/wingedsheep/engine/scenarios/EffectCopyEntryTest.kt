package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.encodeToString
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.core.engineSerializersModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EffectCopyEntryTest : FunSpec({
    val copier = card("Entry Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 1; toughness = 1
        replacementEffect(EntersAsCopy(tappedIfCopied = true, additionalCounters = DynamicAmount.Fixed(1)))
    }
    val subject = card("Entry Subject") {
        manaCost = "{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
        replacementEffect(EntersWithCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, selfOnly = true))
        triggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(3) }
    }
    val returnOne = card("Entry Return One") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(t).then(Effects.GainLife(1))
        }
    }
    val returnAll = card("Entry Return All") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            effect = GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD), "entries")
                .then(MoveCollectionEffect("entries", CardDestination.ToZone(Zone.BATTLEFIELD), storeMovedAs = "arrived"))
                .then(Effects.GainLife(1))
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, subject, returnOne, returnAll))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castReturn(name: String, target: com.wingedsheep.sdk.model.EntityId? = null) {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        if (target == null) castSpell(player1, spell).error shouldBe null
        else castSpellWithTargets(player1, spell, listOf(ChosenTarget.Card(target, player1, Zone.GRAVEYARD))).error shouldBe null
        bothPass()
    }
    test("single reanimation copies before entry counters and ETB triggers and resumes siblings") {
        val d = driver()
        val target = d.putPermanentOnBattlefield(d.player2, subject.name)
        val id = d.putCardInGraveyard(d.player1, copier.name)
        d.castReturn(returnOne.name, id)
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        (id in d.state.getGraveyard(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
        d.submitCardSelection(d.player1, listOf(target)).error shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe subject.name
        d.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        d.getLifeTotal(d.player1) shouldBe 21
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 24
    }
    test("simultaneous entrants cannot copy each other and nothing moves between choices") {
        val d = driver()
        val target = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val first = d.putCardInGraveyard(d.player1, copier.name)
        val second = d.putCardInGraveyard(d.player1, copier.name)
        val arriving = d.putCardInGraveyard(d.player1, subject.name)
        d.castReturn(returnAll.name)
        val one = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        one.options shouldBe listOf(target)
        d.submitCardSelection(d.player1, listOf(target)).error shouldBe null
        val json = kotlinx.serialization.json.Json {
            serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true
        }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        val two = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        two.options shouldBe listOf(target)
        listOf(first, second, arriving).all { it in d.state.getGraveyard(d.player1) } shouldBe true
        d.submitCardSelection(d.player1, listOf(target)).error shouldBe null
        listOf(first, second, arriving).all { it in d.state.getBattlefield() } shouldBe true
        d.getLifeTotal(d.player1) shouldBe 21
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 24
    }
    test("declining preserves original identity without copy riders") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.putCardInGraveyard(d.player1, copier.name)
        d.castReturn(returnOne.name, id)
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
        d.state.getEntity(id)!!.has<TappedComponent>() shouldBe false
        (d.state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
    test("no pre-existing copy candidate means no decision even with another simultaneous entrant") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, copier.name)
        d.putCardInGraveyard(d.player1, subject.name)
        d.castReturn(returnAll.name)
        d.state.pendingDecision shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
    }
    test("effect controller chooses when an opponent-owned copier enters under their control") {
        val d = driver()
        val target = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.putCardInGraveyard(d.player2, copier.name)
        val theft = card("Entry Theft") {
            manaCost = "{U}"; typeLine = "Sorcery"
            spell {
                val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.CreatureInGraveyard)
                effect = MoveToZoneEffect(t, Zone.BATTLEFIELD,
                    controllerOverride = com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
            }
        }
        d.registerCards(listOf(theft))
        val spell = d.putCardInHand(d.player1, theft.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(id, d.player2, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe d.player1
        d.submitCardSelection(d.player1, listOf(target)).error shouldBe null
        d.state.projectedState.getController(id) shouldBe d.player1
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        (id in d.state.getHand(d.player2)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
    }
    test("face-down collection entry offers no copy choice and applies no copy riders") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.putCardInGraveyard(d.player1, copier.name)
        val hidden = card("Entry Hidden") {
            manaCost = "{U}"; typeLine = "Sorcery"
            spell {
                effect = GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD), "entries")
                    .then(MoveCollectionEffect("entries", CardDestination.ToZone(Zone.BATTLEFIELD),
                        faceDown = FaceDownMode.MANIFEST))
            }
        }
        d.registerCards(listOf(hidden))
        d.castReturn(hidden.name)
        d.state.pendingDecision shouldBe null
        d.state.getEntity(id)!!.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() shouldBe true
        d.state.getEntity(id)!!.has<TappedComponent>() shouldBe false
    }
})
