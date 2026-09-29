package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CopyAuraEntryTest : FunSpec({
    val copier = card("Aura Entry Copier") {
        manaCost = "{U}"; typeLine = "Enchantment"
        replacementEffect(EntersAsCopy(copyFilter = GameObjectFilter.Enchantment))
    }
    val aura = card("White Entry Aura") {
        manaCost = "{W}"; typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = ModifyStats(1, 2) }
        replacementEffect(EntersWithCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, selfOnly = true))
    }
    val ownAura = card("Own Creature Entry Aura") {
        manaCost = "{W}"; typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
    }
    val playerAura = card("Player Entry Aura") {
        manaCost = "{B}"; typeLine = "Enchantment — Aura"
        auraTarget = Targets.Player
    }
    val shrouded = card("Shrouded Host") {
        manaCost = "{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
        keywords(Keyword.SHROUD)
    }
    val protected = card("Protected Host") {
        manaCost = "{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
        keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))
    }
    val returnOne = card("Return Aura Copier") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { val t = target(TargetFilter.PermanentInYourGraveyard)
            effect = Effects.PutOntoBattlefield(t, tapped = true).then(Effects.GainLife(1)) }
    }
    val returnAll = card("Return Aura Copiers") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { effect = GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD), "entries")
            .then(MoveCollectionEffect("entries", CardDestination.ToZone(Zone.BATTLEFIELD), storeMovedAs = "arrived"))
            .then(Effects.GainLife(DynamicAmounts.distinctEntitiesIn("arrived"))) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, aura, ownAura, playerAura, shrouded, protected, returnOne, returnAll))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.putAura(name: String, controller: EntityId, host: EntityId): EntityId {
        val id = putPermanentOnBattlefield(controller, name)
        replaceState(AttachmentMover.attach(state, id, host, controller).first)
        return id
    }
    fun GameTestDriver.cast(name: String, target: EntityId? = null): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        val result = if (target == null) castSpell(player1, id) else
            castSpellWithTargets(player1, id, listOf(ChosenTarget.Card(target, player1, Zone.GRAVEYARD)))
        result.error shouldBe null
        bothPass()
        return id
    }
    fun GameTestDriver.roundTrip() {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        replaceState(json.decodeFromString<GameState>(json.encodeToString(state)))
    }
    test("spell copy chooses a non-targeted host using copied colors and enters attached") {
        val d = driver()
        val originalHost = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putAura(aura.name, d.player2, originalHost)
        val hidden = d.putPermanentOnBattlefield(d.player2, shrouded.name)
        val whiteProtected = d.putPermanentOnBattlefield(d.player2, protected.name)
        val id = d.cast(copier.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        val choice = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (hidden in choice.legalTargets.getValue(0)) shouldBe true
        (whiteProtected in choice.legalTargets.getValue(0)) shouldBe false
        (id in d.state.getBattlefield()) shouldBe false
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
        d.roundTrip()
        val entered = d.submitTargetSelection(d.player1, listOf(hidden))
        entered.error shouldBe null
        entered.events.filterIsInstance<PermanentAttachedEvent>().count { it.attachmentId == id } shouldBe 1
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe hidden
        d.state.projectedState.getPower(hidden) shouldBe 3
        d.state.projectedState.getToughness(hidden) shouldBe 4
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
        (id in d.state.getEntity(hidden)!!.get<AttachmentsComponent>()?.attachedIds.orEmpty()) shouldBe false
    }
    test("spell with no legal copied Aura host goes to graveyard without entering") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putAura(ownAura.name, d.player2, host)
        val id = d.cast(copier.name)
        val result = d.submitCardSelection(d.player1, listOf(source))
        result.error shouldBe null
        (id in d.state.getGraveyard(d.player1)) shouldBe true
        result.events.filterIsInstance<ZoneChangeEvent>().any { it.entityId == id && it.toZone == Zone.BATTLEFIELD } shouldBe false
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
    }
    test("single effect preserves tapped rider and sibling effects after copied Aura attachment") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = d.putAura(aura.name, d.player1, host)
        val id = d.putCardInGraveyard(d.player1, copier.name)
        d.cast(returnOne.name, id)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        (id in d.state.getGraveyard(d.player1)) shouldBe true
        d.roundTrip()
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe host
        d.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.getLifeTotal(d.player1) shouldBe 21
    }
    test("effect with no legal copied Aura host leaves original card in graveyard") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putAura(ownAura.name, d.player2, host)
        val id = d.putCardInGraveyard(d.player1, copier.name)
        d.cast(returnOne.name, id)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        (id in d.state.getGraveyard(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
        d.state.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 21
    }
    test("simultaneous copied Auras choose before movement and count only actual arrivals") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putAura(aura.name, d.player2, host)
        val restricted = d.putAura(ownAura.name, d.player2, host)
        val first = d.putCardInGraveyard(d.player1, copier.name)
        val second = d.putCardInGraveyard(d.player1, copier.name)
        val third = d.putCardInGraveyard(d.player1, copier.name)
        d.cast(returnAll.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitCardSelection(d.player1, listOf(restricted)).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        listOf(first, second, third).all { it in d.state.getGraveyard(d.player1) } shouldBe true
        d.roundTrip()
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        listOf(first, second).all { d.state.getEntity(it)!!.get<AttachedToComponent>()?.targetId == host } shouldBe true
        (third in d.state.getGraveyard(d.player1)) shouldBe true
        d.getLifeTotal(d.player1) shouldBe 22
    }
    test("copied enchant-player Aura offers players and enters attached to the chosen one") {
        val d = driver()
        val source = d.putAura(playerAura.name, d.player2, d.player1)
        val id = d.cast(copier.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets.getValue(0).toSet() shouldBe
            setOf(d.player1, d.player2)
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe d.player2
    }
    test("declining the copy enters the printed non-Aura without an attachment choice") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.putAura(aura.name, d.player1, host)
        val id = d.cast(copier.name)
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        d.state.pendingDecision shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.has<AttachedToComponent>() shouldBe false
    }
    for (legalHost in listOf(true, false)) test("explicit collection attachment ${if (legalHost) "attaches" else "declines an illegal host"} without another choice") {
        val d = driver()
        val sourceHost = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = d.putAura(aura.name, d.player1, sourceHost)
        val host = d.putPermanentOnBattlefield(d.player2, if (legalHost) "Grizzly Bears" else protected.name)
        val entry = d.putCardInGraveyard(d.player1, copier.name)
        val spell = card("Return Attached Copier") {
            manaCost = "{U}"; typeLine = "Sorcery"
            spell {
                val target = target(TargetFilter.Creature)
                effect = GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD), "entries")
                    .then(MoveCollectionEffect("entries", CardDestination.ToZone(Zone.BATTLEFIELD), attachTo = target))
            }
        }
        d.registerCards(listOf(spell))
        val id = d.putCardInHand(d.player1, spell.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, id, listOf(ChosenTarget.Permanent(host))).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.state.pendingDecision shouldBe null
        (entry in d.state.getBattlefield()) shouldBe legalHost
        d.state.getEntity(entry)!!.get<AttachedToComponent>()?.targetId shouldBe if (legalHost) host else null
    }
    test("a creature arriving in the same collection cannot host a copied Aura") {
        val d = driver()
        val originalHost = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putAura(ownAura.name, d.player2, originalHost)
        val entry = d.putCardInGraveyard(d.player1, copier.name)
        val newHost = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        d.cast(returnAll.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        (entry in d.state.getGraveyard(d.player1)) shouldBe true
        (newHost in d.state.getBattlefield()) shouldBe true
        d.getLifeTotal(d.player1) shouldBe 21
    }
    test("effect-driven enchant-player copy preserves its attachment and continuation") {
        val d = driver()
        val source = d.putAura(playerAura.name, d.player2, d.player1)
        val entry = d.putCardInGraveyard(d.player1, copier.name)
        d.cast(returnOne.name, entry)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.roundTrip()
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.state.getEntity(entry)!!.get<AttachedToComponent>()!!.targetId shouldBe d.player2
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("copied player Aura uses its copied color for protection and dies when its host gains protection") {
        val d = driver()
        val source = d.putAura(playerAura.name, d.player1, d.player1)
        d.replaceState(d.state.updateEntity(d.player2) { it.with(
            com.wingedsheep.engine.state.components.player.PlayerProtectionComponent(listOf(ProtectionScope.Color(Color.BLACK)))) })
        val id = d.cast(copier.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets.getValue(0) shouldBe listOf(d.player1)
        d.submitTargetSelection(d.player1, listOf(d.player1)).error shouldBe null
        d.replaceState(d.state.updateEntity(d.player1) { it.with(
            com.wingedsheep.engine.state.components.player.PlayerProtectionComponent(listOf(ProtectionScope.Everything))) })
        d.bothPass()
        (id in d.state.getGraveyard(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
    }

})
