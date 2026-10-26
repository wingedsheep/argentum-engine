package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EffectSuppliedEntryCountersTest : FunSpec({
    val kind = CounterType.PLUS_ONE_PLUS_ONE
    val doubler = card("Entry Counter Doubler") {
        typeLine = "Enchantment"
        replacementEffect(ScaleCounterPlacement(
            appliesTo = EventPattern.CounterPlacementEvent(recipient = Recipient.AnyPermanent), placer = Player.You))
    }
    val blocker = card("Entry Counter Prevention") {
        typeLine = "Enchantment"
        staticAbility { ability = CantReceiveCounters(GroupFilter(GameObjectFilter.Creature)) }
    }
    val selfBlocker = card("Entry Self Counter Prevention") {
        typeLine = "Creature"; power = 1; toughness = 1
        staticAbility { ability = CantReceiveCounters(GroupFilter.source()) }
    }
    val redirect = card("Entry Redirect") {
        typeLine = "Enchantment"
        replacementEffect(RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD, filter = GameObjectFilter.Creature)))
    }
    val entryLock = card("Entry Locked") {
        typeLine = "Enchantment"
        staticAbility { ability = CantEnterTheBattlefield(GameObjectFilter.Creature) }
    }
    val chooser = card("Entry Counter Chooser") {
        typeLine = "Creature"; power = 1; toughness = 1
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
    }
    val aura = card("Entry Counter Aura") {
        typeLine = "Enchantment — Aura"
        auraTarget = com.wingedsheep.sdk.scripting.targets.TargetObject(
            filter = com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
    }
    val auraLock = card("Entry Aura Lock") {
        typeLine = "Enchantment"
        staticAbility { ability = CantEnterTheBattlefield(GameObjectFilter.Enchantment) }
    }
    val auraRedirect = card("Entry Aura Redirect") {
        typeLine = "Enchantment"
        replacementEffect(RedirectZoneChange(newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD, filter = GameObjectFilter.Enchantment)))
    }
    val walker = card("Entry Counter Walker") {
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
    }
    val creatureWalker = card("Entry Counter Creature Walker") {
        typeLine = "Legendary Creature Planeswalker — Test"
        power = 1; toughness = 1
        startingLoyalty = 2
    }
    val entrant = card("Entry Counter Recipient") {
        typeLine = "Creature"; power = 0; toughness = 0
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(doubler, blocker, selfBlocker, redirect, entrant, entryLock, chooser, aura, auraLock, auraRedirect, walker, creatureWalker))
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
    }
    // The same contract must hold for a single move and a pipeline batch, including face-down entry.
    for (collection in listOf(false, true)) {
        fun move(d: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId, ownerControl: Boolean = false,
                 faceDown: Boolean = false): com.wingedsheep.engine.core.EffectResult {
            val effect = if (collection) MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = kind, underOwnersControl = ownerControl,
                faceDown = if (faceDown) FaceDownMode.MANIFEST else null)
            else Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD,
                controllerOverride = if (ownerControl) null else EffectTarget.PlayerRef(Player.You),
                addCounterType = kind, faceDown = if (faceDown) FaceDownMode.MANIFEST else null)
            return d.services.effectExecutorRegistry.execute(d.state, effect,
                EffectContext(sourceId = null, controllerId = d.player1,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(id)))))
        }
        test("$collection: a zero-toughness creature survives settling because counters are part of entry") {
            val d = driver()
            val returning = card("Entry Counter Return") {
                manaCost = "{B}"; typeLine = "Sorcery"
                spell {
                    val creature = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.CreatureInYourGraveyard)
                    effect = if (collection) Effects.Pipeline {
                        val cards = gather(CardSource.ChosenTargets)
                        move(cards, CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = kind)
                    } else Effects.Move(creature, Zone.BATTLEFIELD, addCounterType = kind)
                }
            }
            d.registerCards(listOf(returning))
            d.passPriorityUntil(com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)
            val id = d.putCardInGraveyard(d.player1, entrant.name)
            val spell = d.putCardInHand(d.player1, returning.name)
            d.giveMana(d.player1, Color.BLACK)
            d.castSpellWithTargets(d.player1, spell, listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD)
            )).error shouldBe null
            d.bothPass()
            (id in d.state.getBattlefield()) shouldBe true
            d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
        }
        test("$collection: replacements apply and entry counters precede the zone event without mutating input") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, doubler.name)
            val id = d.putCardInGraveyard(d.player1, entrant.name)
            val before = d.state
            val result = move(d, id)
            (id in result.state.getBattlefield()) shouldBe true
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 2
            before.getEntity(id)!!.get<CountersComponent>() shouldBe null
            val counter = result.events.filterIsInstance<CountersAddedEvent>().single()
            counter.amount shouldBe 2
            counter.placedBy shouldBe d.player1
            counter.firstThisTurn shouldBe true
            (result.events.indexOf(counter) < result.events.indexOfFirst { it is ZoneChangeEvent && it.entityId == id }) shouldBe true
            result.state.projectedState.getPower(id) shouldBe 2
        }
        test("$collection: external prevention and entrant's own prospective prevention suppress placement/events") {
            for (own in listOf(false, true)) {
                val d = driver()
                if (!own) d.putPermanentOnBattlefield(d.player1, blocker.name)
                val id = d.putCardInGraveyard(d.player1, if (own) selfBlocker.name else "Grizzly Bears")
                val result = move(d, id)
                (id in result.state.getBattlefield()) shouldBe true
                (result.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe 0
                result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
            }
        }
        test("$collection: entering controller is placer even when another player resolves the move") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, doubler.name)
            val id = d.putCardInGraveyard(d.player2, "Grizzly Bears")
            val result = move(d, id, ownerControl = true)
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
            result.events.filterIsInstance<CountersAddedEvent>().single().placedBy shouldBe d.player2
        }
        test("$collection: redirected entries receive no counter and emit no placement event") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, redirect.name)
            val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
            val result = move(d, id)
            (id in result.state.getExile(d.player1)) shouldBe true
            (result.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe 0
            result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
        }
        test("$collection: prohibited entries stay in the graveyard without counters") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, entryLock.name)
            val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
            val result = move(d, id)
            (id in result.state.getGraveyard(d.player1)) shouldBe true
            result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
            (result.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe 0
        }
        test("$collection: replay after an entry choice retains the counter instruction") {
            val d = driver()
            val id = d.putCardInGraveyard(d.player1, chooser.name)
            val paused = move(d, id)
            (id in paused.state.getGraveyard(d.player1)) shouldBe true
            d.replaceState(paused.state)
            val question = d.pendingDecision as ChooseColorDecision
            d.submitDecision(d.player1, ColorChosenResponse(question.id, Color.RED)).error shouldBe null
            (id in d.state.getBattlefield()) shouldBe true
            d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
        }
        test("$collection: face-down entries keep effect-supplied counters") {
            val d = driver()
            val id = d.putCardInGraveyard(d.player1, selfBlocker.name)
            val result = move(d, id, faceDown = true)
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
            result.state.projectedState.getPower(id) shouldBe 3
        }
    }
    test("an entering broad counter doubler does not modify its own effect-supplied entry counter") {
        for (collection in listOf(false, true)) {
            val d = driver()
            val id = d.putCardInGraveyard(d.player1, doubler.name)
            val effect = if (collection) MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = kind)
            else Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, addCounterType = kind)
            val result = d.services.effectExecutorRegistry.execute(d.state, effect,
                EffectContext(sourceId = null, controllerId = d.player1,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(id)))))
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
            result.events.filterIsInstance<CountersAddedEvent>().single().amount shouldBe 1
        }
    }
    test("simultaneous entrants do not supply broad replacement or prevention effects to each other") {
        for (first in listOf(true, false)) {
            for (sourceCard in listOf(doubler, blocker)) {
                val d = driver()
                val source = d.putCardInGraveyard(d.player1, sourceCard.name)
                val recipient = d.putCardInGraveyard(d.player1, "Grizzly Bears")
                val ids = if (first) listOf(source, recipient) else listOf(recipient, source)
                val result = d.services.effectExecutorRegistry.execute(d.state,
                    MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = kind),
                    EffectContext(sourceId = null, controllerId = d.player1,
                        pipeline = PipelineState(storedCollections = mapOf("cards" to ids))))
                result.state.getEntity(recipient)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
                result.events.filterIsInstance<CountersAddedEvent>().single { it.entityId == recipient }.amount shouldBe 1
            }
        }
    }
    test("printed and supplied counters of one kind are one placement under an additive replacement") {
        val printed = card("Entry Printed Counter") {
            typeLine = "Creature"; power = 0; toughness = 0
            replacementEffect(EntersWithCounters(counterType = kind, count = 1, selfOnly = true))
        }
        val scales = card("Entry Additive Counter Replacement") {
            typeLine = "Enchantment"
            replacementEffect(ModifyCounterPlacement(modifier = 1,
                appliesTo = EventPattern.CounterPlacementEvent(kind, Recipient.AnyPermanent)))
        }
        for (collection in listOf(false, true)) {
            val d = driver()
            d.registerCards(listOf(printed, scales))
            d.putPermanentOnBattlefield(d.player1, scales.name)
            val id = d.putCardInGraveyard(d.player1, printed.name)
            val effect = if (collection) MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = kind)
            else Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, addCounterType = kind)
            val result = d.services.effectExecutorRegistry.execute(d.state, effect,
                EffectContext(sourceId = null, controllerId = d.player1,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(id)))))
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 3
            result.events.filterIsInstance<CountersAddedEvent>().single().amount shouldBe 3
        }
    }
    test("intrinsic loyalty defense and Saga lore combine with a supplied counter before placement replacements") {
        val scales = card("Entry All Counter Additive Replacement") {
            typeLine = "Enchantment"
            replacementEffect(ModifyCounterPlacement(modifier = 1,
                appliesTo = EventPattern.CounterPlacementEvent(recipient = Recipient.AnyPermanent)))
        }
        val walker = card("Entry Counter Walker") {
            typeLine = "Planeswalker — Tester"; startingLoyalty = 3
        }
        val battle = card("Entry Counter Battle") {
            typeLine = "Battle"; startingDefense = 3
        }
        val saga = card("Entry Counter Saga") { typeLine = "Enchantment — Saga" }
        for ((definition, counterType, expected) in listOf(
            Triple(walker, CounterType.LOYALTY, 5),
            Triple(battle, CounterType.DEFENSE, 5),
            Triple(saga, CounterType.LORE, 3)
        )) {
            val d = driver()
            d.registerCards(listOf(scales, definition))
            d.putPermanentOnBattlefield(d.player1, scales.name)
            val id = d.putCardInGraveyard(d.player1, definition.name)
            val result = d.services.effectExecutorRegistry.execute(d.state,
                Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, addCounterType = counterType),
                EffectContext(sourceId = null, controllerId = d.player1))
            result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(counterType) shouldBe expected
            result.events.filterIsInstance<CountersAddedEvent>().single().amount shouldBe expected
        }
    }
    test("explicitly attached Aura enters through movement replacements with its supplied counters") {
        for (interference in listOf("none", "redirect", "lock")) {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, doubler.name)
            val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            if (interference != "none") d.putPermanentOnBattlefield(d.player1,
                if (interference == "redirect") auraRedirect.name else auraLock.name)
            val id = d.putCardInGraveyard(d.player1, aura.name)
            val result = d.services.effectExecutorRegistry.execute(d.state,
                MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = kind,
                    attachTo = EffectTarget.SpecificEntity(host)),
                EffectContext(sourceId = null, controllerId = d.player1,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(id)))))
            when (interference) {
                "none" -> {
                    (id in result.state.getBattlefield()) shouldBe true
                    result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 2
                    result.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()!!.targetId shouldBe host
                }
                "redirect" -> (id in result.state.getExile(d.player1)) shouldBe true
                "lock" -> (id in result.state.getGraveyard(d.player1)) shouldBe true
            }
            result.events.filterIsInstance<CountersAddedEvent>().size shouldBe if (interference == "none") 1 else 0
        }
    }
    test("ordinary Aura attachment-choice replay retains collection entry counters") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val id = d.putCardInGraveyard(d.player1, aura.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            MoveCollectionEffect("cards", CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = kind),
            EffectContext(sourceId = null, controllerId = d.player1,
                pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(id)))))
        d.replaceState(result.state)
        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 1
    }
    test("single move's conditional counter filter reads the arriving permanent") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD,
                addCounterType = kind, addCounterIf = GameObjectFilter.Artifact),
            EffectContext(sourceId = null, controllerId = d.player1))
        (result.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe 0
        result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
    }
    // Semester's End: "an additional +1/+1 counter on it if it's a creature and an additional
    // loyalty counter on it if it's a planeswalker" — each counter gated on its own filter.
    val typedCounters = listOf(
        EntryCounter(CounterType.PLUS_ONE_PLUS_ONE, GameObjectFilter.Creature),
        EntryCounter(CounterType.LOYALTY, GameObjectFilter.Planeswalker))
    test("several conditional entry counters: a creature gets only the counter its filter allows") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, entryCounters = typedCounters),
            EffectContext(sourceId = null, controllerId = d.player1))
        val counters = result.state.getEntity(id)!!.get<CountersComponent>()!!
        counters.getCount(kind) shouldBe 1
        counters.getCount(CounterType.LOYALTY) shouldBe 0
    }
    test("several conditional entry counters: a planeswalker's extra loyalty joins its printed loyalty") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, walker.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, entryCounters = typedCounters),
            EffectContext(sourceId = null, controllerId = d.player1))
        (id in result.state.getBattlefield()) shouldBe true
        val counters = result.state.getEntity(id)!!.get<CountersComponent>()!!
        counters.getCount(CounterType.LOYALTY) shouldBe 4
        counters.getCount(kind) shouldBe 0
    }
    test("several conditional entry counters: a creature planeswalker gets both") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, creatureWalker.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, entryCounters = typedCounters),
            EffectContext(sourceId = null, controllerId = d.player1))
        val counters = result.state.getEntity(id)!!.get<CountersComponent>()!!
        counters.getCount(kind) shouldBe 1
        counters.getCount(CounterType.LOYALTY) shouldBe 3
    }
    test("several conditional entry counters are one entry event for counter replacements") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, doubler.name)
        val id = d.putCardInGraveyard(d.player1, walker.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, entryCounters = typedCounters),
            EffectContext(sourceId = null, controllerId = d.player1))
        // Printed 3 + the additional 1 enter together, then the doubler doubles that placement.
        result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 8
    }
    test("several entry counters annotate a card moved to a non-battlefield zone, each gated on its filter") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.EXILE, entryCounters = listOf(
                EntryCounter(CounterType.STASH), EntryCounter(CounterType.FINALITY, GameObjectFilter.Artifact))),
            EffectContext(sourceId = null, controllerId = d.player1))
        val counters = result.state.getEntity(id)!!.get<CountersComponent>()!!
        counters.getCount(CounterType.STASH) shouldBe 1
        counters.getCount(CounterType.FINALITY) shouldBe 0
    }
    // Cosima, God of the Voyage: "return Cosima to the battlefield with X +1/+1 counters on it, where X
    // is the number of voyage counters on it" — the count is read off the card in exile, before the
    // move strips its counters (CR 122.2).
    fun exiledWithVoyage(d: GameTestDriver, voyage: Int): com.wingedsheep.sdk.model.EntityId {
        val id = d.putCardInExile(d.player1, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.VOYAGE, voyage)) })
        return id
    }
    fun returnWithVoyageCount(id: com.wingedsheep.sdk.model.EntityId) = Effects.Move(
        EffectTarget.SpecificEntity(id), Zone.BATTLEFIELD, fromZone = Zone.EXILE,
        entryCounters = listOf(EntryCounter(kind, count = com.wingedsheep.sdk.dsl.DynamicAmounts.countersOn(
            EffectTarget.SpecificEntity(id), CounterType.VOYAGE))))
    test("a dynamic entry-counter count is read off the card before it leaves exile") {
        val d = driver()
        val id = exiledWithVoyage(d, 3)
        val result = d.services.effectExecutorRegistry.execute(d.state, returnWithVoyageCount(id),
            EffectContext(sourceId = null, controllerId = d.player1))
        (id in result.state.getBattlefield()) shouldBe true
        val counters = result.state.getEntity(id)!!.get<CountersComponent>()!!
        counters.getCount(kind) shouldBe 3
        counters.getCount(CounterType.VOYAGE) shouldBe 0
        result.events.filterIsInstance<CountersAddedEvent>().sumOf { it.amount } shouldBe 3
    }
    test("a dynamic entry-counter count is one placement for counter replacements") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, doubler.name)
        val id = exiledWithVoyage(d, 2)
        val result = d.services.effectExecutorRegistry.execute(d.state, returnWithVoyageCount(id),
            EffectContext(sourceId = null, controllerId = d.player1))
        result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(kind) shouldBe 4
    }
    test("a dynamic entry-counter count of zero places no counters") {
        val d = driver()
        val id = exiledWithVoyage(d, 0)
        val result = d.services.effectExecutorRegistry.execute(d.state, returnWithVoyageCount(id),
            EffectContext(sourceId = null, controllerId = d.player1))
        (id in result.state.getBattlefield()) shouldBe true
        (result.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe 0
        result.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()
    }
    test("a dynamic entry-counter count annotates a card moved to a non-battlefield zone") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.Move(EffectTarget.SpecificEntity(id), Zone.EXILE,
                entryCounters = listOf(EntryCounter(CounterType.STASH, count = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2)))),
            EffectContext(sourceId = null, controllerId = d.player1))
        result.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.STASH) shouldBe 2
    }
})
