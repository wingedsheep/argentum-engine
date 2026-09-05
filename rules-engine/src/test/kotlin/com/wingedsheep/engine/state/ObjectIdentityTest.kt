package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ZoneTransitionCause
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.LibraryPlacement
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.zones.MoveToZoneEffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ObjectIdentityTest : FunSpec({
    val owner = EntityId.generate()
    val other = EntityId.generate()
    val cardId = EntityId.generate()
    val secondId = EntityId.generate()
    val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        encodeDefaults = true
    }

    fun card(keywords: Set<Keyword> = emptySet()) = ComponentContainer.of(
        CardComponent("Identity Bear", "Identity Bear", ManaCost(emptyList()),
            TypeLine(cardTypes = setOf(CardType.CREATURE)), ownerId = owner, baseKeywords = keywords),
        OwnerComponent(owner), ControllerComponent(owner)
    )
    fun inZone(zone: Zone, keywords: Set<Keyword> = emptySet()): GameState {
        val state = GameState(turnOrder = listOf(owner, other))
            .withEntity(owner, ComponentContainer.EMPTY)
            .withEntity(other, ComponentContainer.EMPTY)
            .withEntity(cardId, card(keywords))
        return if (zone == Zone.STACK) state.pushToStack(cardId)
            else state.addToZone(ZoneKey(owner, zone), cardId)
    }

    for (origin in Zone.entries) for (destination in Zone.entries) {
        if (origin == destination) continue
        test("$origin to $destination allocates exactly one new object") {
            val initial = inZone(origin)
            val ref = initial.objectRef(cardId)!!
            val removed = if (origin == Zone.STACK) initial.popFromStack().second
                else initial.removeFromZone(ZoneKey(owner, origin), cardId)
            removed.objectRef(cardId) shouldBe ref
            val moved = if (destination == Zone.STACK) removed.pushToStack(cardId)
                else removed.addToZone(ZoneKey(owner, destination), cardId)
            moved.objectRef(cardId) shouldBe ObjectRef(cardId, initial.nextObjectGeneration)
            moved.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
            moved.isCurrentObject(ref) shouldBe false
            initial.isCurrentObject(ref) shouldBe true
        }
    }

    test("actual exile to exile creates another object but duplicate addition does not") {
        val initial = inZone(Zone.EXILE)
        initial.addToZone(ZoneKey(owner, Zone.EXILE), cardId) shouldBe initial
        val result = ZoneTransitionService.moveToZone(initial, cardId, Zone.EXILE)
        result.state.objectRef(cardId) shouldNotBe initial.objectRef(cardId)
        result.state.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
        result.events.filterIsInstance<ZoneChangeEvent>().single().let {
            it.oldObject shouldBe initial.objectRef(cardId)
            it.newObject shouldBe result.state.objectRef(cardId)
        }
    }

    test("library ordering and storage reconstruction preserve visits and allocator") {
        val initial = inZone(Zone.LIBRARY).withEntity(secondId, card())
            .addToZone(ZoneKey(owner, Zone.LIBRARY), secondId)
        val key = ZoneKey(owner, Zone.LIBRARY)
        val ordered = initial.reorderZone(key, listOf(secondId, cardId))
        val reinserted = ordered.removeFromZone(key, cardId).insertIntoZone(key, cardId, 0)
        reinserted.objectIdentities shouldBe initial.objectIdentities
        reinserted.nextObjectGeneration shouldBe initial.nextObjectGeneration
        reinserted.initializeObjectIdentities() shouldBe reinserted
        val serviceReorder = ZoneTransitionService.moveToZone(initial, cardId, Zone.LIBRARY,
            ZoneEntryOptions(libraryPlacement = LibraryPlacement.Bottom))
        serviceReorder.state.getZone(key) shouldBe listOf(secondId, cardId)
        serviceReorder.state.objectRef(cardId) shouldBe initial.objectRef(cardId)
        serviceReorder.transitions shouldBe emptyList()
        serviceReorder.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("same battlefield relocation control phasing and characteristics preserve identity") {
        val initial = inZone(Zone.BATTLEFIELD)
        val relocated = initial.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), cardId)
            .addToZone(ZoneKey(other, Zone.BATTLEFIELD), cardId)
            .updateEntity(cardId) { it.with(ControllerComponent(other)).with(PhasedOutComponent(other)) }
        relocated.objectRef(cardId) shouldBe initial.objectRef(cardId)
        val changed = relocated.updateEntity(cardId) {
            it.with(it.get<CardComponent>()!!.copy(name = "Transformed characteristics"))
                .without<PhasedOutComponent>()
        }
        changed.objectRef(cardId) shouldBe initial.objectRef(cardId)
        changed.nextObjectGeneration shouldBe initial.nextObjectGeneration
    }

    test("private zone owner changes allocate a new visit") {
        for (zone in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.SIDEBOARD)) {
            val initial = inZone(zone)
            val moved = initial.moveToZone(cardId, ZoneKey(owner, zone), ZoneKey(other, zone))
            moved.isCurrentObject(initial.objectRef(cardId)!!) shouldBe false
        }
    }

    test("popped resolving spell remains the stack object until its real destination") {
        val initial = inZone(Zone.STACK)
        val resolving = initial.popFromStack().second.updateEntity(cardId) { card() }
        resolving.objectRef(cardId) shouldBe initial.objectRef(cardId)
        resolving.logicalZone(cardId)?.zoneType shouldBe Zone.STACK
        val result = ZoneTransitionService.moveToZone(resolving, cardId, Zone.GRAVEYARD)
        val event = result.events.filterIsInstance<ZoneChangeEvent>().single()
        event.fromZone shouldBe Zone.STACK
        event.oldObject shouldBe initial.objectRef(cardId)
        event.newObject shouldBe result.state.objectRef(cardId)
        result.state.stack shouldBe emptyList()
        result.state.isCurrentObject(event.oldObject!!) shouldBe false
    }

    test("new stack abilities and copied spells acquire first identities") {
        val initial = GameState().withEntity(cardId, ComponentContainer.EMPTY)
            .withEntity(secondId, card())
        val pushed = initial.pushToStack(cardId).pushToStack(secondId)
        pushed.objectRef(cardId) shouldNotBe null
        pushed.objectRef(secondId) shouldNotBe null
        pushed.objectRef(cardId)!!.generation shouldNotBe pushed.objectRef(secondId)!!.generation
        pushed.pushToStack(secondId) shouldBe pushed
    }

    test("deletion invalidates refs even if an entity ID is reused") {
        val initial = inZone(Zone.EXILE)
        val ref = initial.objectRef(cardId)!!
        val deleted = initial.removeEntity(cardId)
        deleted.isCurrentObject(ref) shouldBe false
        val recreated = deleted.withEntity(cardId, card()).addToZone(ZoneKey(owner, Zone.EXILE), cardId)
        recreated.isCurrentObject(ref) shouldBe false
        recreated.objectRef(cardId)!!.generation shouldBe initial.nextObjectGeneration
        initial.withoutEntity(cardId).isCurrentObject(ref) shouldBe false
    }

    test("each event retains its own before and after refs across later round trips") {
        val initial = inZone(Zone.BATTLEFIELD)
        val death = ZoneTransitionService.moveToZone(initial, cardId, Zone.GRAVEYARD)
        val exile = ZoneTransitionService.moveToZone(death.state, cardId, Zone.EXILE)
        val returnToYard = ZoneTransitionService.moveToZone(exile.state, cardId, Zone.GRAVEYARD)
        val first = death.events.filterIsInstance<ZoneChangeEvent>().single()
        val second = exile.events.filterIsInstance<ZoneChangeEvent>().single()
        first.oldObject shouldBe initial.objectRef(cardId)
        first.newObject shouldBe second.oldObject
        first.newObject shouldNotBe returnToYard.state.objectRef(cardId)
        returnToYard.state.isCurrentObject(first.newObject!!) shouldBe false
        val restoredEvent = json.decodeFromString(ZoneChangeEvent.serializer(),
            json.encodeToString(ZoneChangeEvent.serializer(), first))
        restoredEvent shouldBe first
    }

    test("replacement destination is primary and no fictional graveyard identity is allocated") {
        val replacementId = EntityId.generate()
        val initial = inZone(Zone.BATTLEFIELD).withEntity(replacementId,
            card().with(ReplacementEffectSourceComponent(listOf(RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
            ))))).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), replacementId)
        val result = ZoneTransitionService.moveToZone(initial, cardId, Zone.GRAVEYARD)
        result.actualDestination shouldBe Zone.EXILE
        result.events.filterIsInstance<ZoneChangeEvent>().single().requestedDestination shouldBe Zone.GRAVEYARD
        result.state.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
        result.transitions.single().let {
            it.requestedDestination shouldBe Zone.GRAVEYARD
            it.toZone shouldBe Zone.EXILE
            it.cause shouldBe ZoneTransitionCause.PRIMARY
            it.newObject shouldBe result.state.objectRef(cardId)
        }
    }

    test("replacement retaining an object does not clean it up or emit a zone change") {
        val replacementId = EntityId.generate()
        val initial = inZone(Zone.BATTLEFIELD).updateEntity(cardId) { it.with(TappedComponent) }
            .withEntity(replacementId, card().with(ReplacementEffectSourceComponent(listOf(RedirectZoneChange(
                newDestination = Zone.BATTLEFIELD,
                appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
            ))))).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), replacementId)
        val result = ZoneTransitionService.moveToZone(initial, cardId, Zone.GRAVEYARD)
        result.state shouldBe initial
        result.transitions shouldBe emptyList()
        result.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("replacement additional token creation is separately attributed") {
        val replacementId = EntityId.generate()
        val initial = inZone(Zone.BATTLEFIELD).withEntity(replacementId,
            card().with(ReplacementEffectSourceComponent(listOf(RedirectZoneChangeWithEffect(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
                additionalEffect = CreateTokenEffect(power = 1, toughness = 1,
                    colors = setOf(Color.GREEN), creatureTypes = setOf("Saproling"))
            ))))).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), replacementId)
        val result = ZoneTransitionService.moveToZone(initial, cardId, Zone.GRAVEYARD)
        result.transitions.count { it.cause == ZoneTransitionCause.PRIMARY } shouldBe 1
        result.transitions.single { it.cause == ZoneTransitionCause.PRIMARY }.newObject shouldBe
            result.state.objectRef(cardId)
        val token = result.transitions.single { it.cause == ZoneTransitionCause.REPLACEMENT_ADDITIONAL }
        token.oldObject shouldBe null
        token.newObject shouldNotBe null
        result.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId != cardId }
            .transitionCause shouldBe ZoneTransitionCause.REPLACEMENT_ADDITIONAL
    }

    test("prevented destruction allocates no destination object") {
        val initial = inZone(Zone.BATTLEFIELD, setOf(Keyword.INDESTRUCTIBLE))
        val executor = MoveToZoneEffectExecutor(CardRegistry(), effectExecutor = { _, _, _ ->
            error("unexpected entry effect")
        })
        val result = executor.execute(initial,
            MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true),
            EffectContext(sourceId = null, controllerId = owner, targets = listOf(ChosenTarget.Permanent(cardId))))
        result.state.objectRef(cardId) shouldBe initial.objectRef(cardId)
        result.state.nextObjectGeneration shouldBe initial.nextObjectGeneration
        result.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("library placement strategies stamp once and batch outcomes retain every move") {
        for (placement in listOf(LibraryPlacement.Top, LibraryPlacement.Bottom,
            LibraryPlacement.NthFromTop(1), LibraryPlacement.Shuffled)) {
            val initial = inZone(Zone.HAND).withEntity(secondId, card())
                .addToZone(ZoneKey(owner, Zone.LIBRARY), secondId)
            val result = ZoneTransitionService.moveToZone(initial, cardId, Zone.LIBRARY,
                ZoneEntryOptions(libraryPlacement = placement))
            result.state.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
            result.state.objectRef(secondId) shouldBe initial.objectRef(secondId)
        }
        val initial = inZone(Zone.HAND).withEntity(secondId, card())
            .addToZone(ZoneKey(owner, Zone.HAND), secondId)
        val batch = ZoneTransitionService.moveToZoneBatch(initial, listOf(cardId, secondId), Zone.GRAVEYARD)
        batch.transitions.map { it.oldObject }.toSet() shouldBe
            setOf(initial.objectRef(cardId), initial.objectRef(secondId))
        batch.transitions.size shouldBe 2
    }

    test("raw fixtures and legacy JSON initialize once and full JSON preserves exact generations") {
        val raw = GameState(entities = mapOf(cardId to card(), secondId to card()),
            zones = mapOf(ZoneKey(owner, Zone.EXILE) to listOf(cardId)), stack = listOf(secondId))
        raw.objectRef(cardId) shouldNotBe null
        raw.objectRef(secondId) shouldNotBe null
        val evolved = raw.moveToZone(cardId, ZoneKey(owner, Zone.EXILE), ZoneKey(owner, Zone.GRAVEYARD))
        val encoded = json.encodeToString(GameState.serializer(), evolved)
        val decoded = json.decodeFromString(GameState.serializer(), encoded)
        decoded.objectIdentities shouldBe evolved.objectIdentities
        decoded.nextObjectGeneration shouldBe evolved.nextObjectGeneration
        decoded.isCurrentObject(raw.objectRef(cardId)!!) shouldBe false
        val legacy = JsonObject(json.parseToJsonElement(encoded).jsonObject - "objectIdentities" - "nextObjectGeneration")
        val imported = json.decodeFromString(GameState.serializer(), legacy.toString())
        imported.objectRef(cardId) shouldNotBe null
        imported.objectRef(secondId) shouldNotBe null
        imported.initializeObjectIdentities() shouldBe imported
        val copiedFixture = GameState().copy(entities = raw.entities, zones = raw.zones, stack = raw.stack)
            .initializeObjectIdentities()
        copiedFixture.objectIdentities shouldBe raw.objectIdentities
    }
    test("draw emits the exact library and hand objects and empty draws allocate nothing") {
        val primitive = com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive(CardRegistry())
        val initial = inZone(Zone.LIBRARY)
        val drawn = primitive.drawOne(initial, owner)
        val event = drawn.events.filterIsInstance<ZoneChangeEvent>().single()
        event.oldObject shouldBe initial.objectRef(cardId)
        event.newObject shouldBe drawn.state.objectRef(cardId)
        drawn.state.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
        val failed = primitive.drawOne(drawn.state, owner)
        failed.state.nextObjectGeneration shouldBe drawn.state.nextObjectGeneration
        failed.events.filterIsInstance<ZoneChangeEvent>() shouldBe emptyList()
    }

    test("mulligan retains both successive event identities and London bottom stamps again") {
        // A one-card library after returning the hand guarantees that this card is redrawn.
        val initial = inZone(Zone.HAND)
        val handler = com.wingedsheep.engine.handlers.MulliganHandler()
        val mulligan = handler.handleTakeMulligan(initial, com.wingedsheep.engine.core.TakeMulligan(owner))
        val events = mulligan.events.filterIsInstance<ZoneChangeEvent>()
        events.size shouldBe 2
        events[0].oldObject shouldBe initial.objectRef(cardId)
        events[0].newObject shouldBe events[1].oldObject
        events[1].newObject shouldBe mulligan.state.objectRef(cardId)
        events[0].newObject shouldNotBe mulligan.state.objectRef(cardId)
        val kept = mulligan.state.updateEntity(owner) {
            it.with(com.wingedsheep.engine.state.components.player.MulliganStateComponent(
                mulligansTaken = 1, hasKept = true))
        }
        val bottom = handler.handleBottomCards(kept, com.wingedsheep.engine.core.BottomCards(owner, listOf(cardId)))
        val bottomEvent = bottom.events.filterIsInstance<ZoneChangeEvent>().single()
        bottomEvent.oldObject shouldBe kept.objectRef(cardId)
        bottomEvent.newObject shouldBe bottom.state.objectRef(cardId)
        bottom.state.nextObjectGeneration shouldBe kept.nextObjectGeneration + 1
    }

    test("ordered library continuation stamps arrivals but preserves library members") {
        val services = com.wingedsheep.engine.core.EngineServices(CardRegistry())
        val resumer = com.wingedsheep.engine.handlers.continuations.LibraryAndZoneContinuationResumer(services)
        for (origin in listOf(Zone.HAND, Zone.STACK, Zone.BATTLEFIELD)) {
        var initial = inZone(origin).withEntity(secondId, card())
            .addToZone(ZoneKey(owner, Zone.LIBRARY), secondId)
        if (origin == Zone.BATTLEFIELD) initial = initial
            .moveToZone(cardId, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(other, Zone.BATTLEFIELD))
        for (placement in listOf(com.wingedsheep.sdk.scripting.effects.ZonePlacement.Top,
            com.wingedsheep.sdk.scripting.effects.ZonePlacement.Bottom)) {
            val frame = com.wingedsheep.engine.core.MoveCollectionOrderContinuation(
                decisionId = "identity-order", playerId = owner, sourceId = null, sourceName = null,
                cards = listOf(cardId, secondId), destinationZone = Zone.LIBRARY,
                destinationPlayerId = owner, placement = placement)
            val result = resumer.resumeMoveCollectionOrder(initial, frame,
                com.wingedsheep.engine.core.OrderedResponse("identity-order", listOf(secondId, cardId))) { next, events ->
                com.wingedsheep.engine.core.ExecutionResult.success(next, events)
            }
            result.state.objectRef(secondId) shouldBe initial.objectRef(secondId)
            result.state.nextObjectGeneration shouldBe initial.nextObjectGeneration + 1
            val event = result.events.filterIsInstance<ZoneChangeEvent>().single()
            event.oldObject shouldBe initial.objectRef(cardId)
            event.newObject shouldBe result.state.objectRef(cardId)
        }
        }
    }

    test("opening hand initialization and hand smoothing stamp every destination") {
        val definition = com.wingedsheep.sdk.model.CardDefinition.creature(
            name = "Initialization Bear", manaCost = ManaCost.parse("{1}{G}"),
            subtypes = emptySet(), power = 2, toughness = 2)
        val registry = CardRegistry().also { it.register(definition) }
        for (smoothed in listOf(false, true)) {
            val deck = com.wingedsheep.sdk.model.Deck(cards = List(40) { definition.name })
            val result = com.wingedsheep.engine.core.GameInitializer(registry).initializeGame(
                com.wingedsheep.engine.core.GameConfig(players = listOf(
                    com.wingedsheep.engine.core.PlayerConfig("One", deck),
                    com.wingedsheep.engine.core.PlayerConfig("Two", deck)), useHandSmoother = smoothed))
            for ((key, ids) in result.state.zones) for (id in ids) {
                result.state.objectRef(id) shouldNotBe null
                result.state.logicalZone(id) shouldBe key
            }
            val drawEvents = result.events.filterIsInstance<ZoneChangeEvent>()
            drawEvents.size shouldBe 14
            for (event in drawEvents) {
                event.oldObject shouldNotBe null
                event.newObject shouldBe result.state.objectRef(event.entityId)
                event.newObject shouldNotBe event.oldObject
            }
        }
    }

    test("insertion refuses simultaneous membership in different exile buckets") {
        val initial = inZone(Zone.EXILE)
        io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            initial.addToZone(ZoneKey(other, Zone.EXILE), cardId)
        }
        initial.nextObjectGeneration shouldBe inZone(Zone.EXILE).nextObjectGeneration
    }

    test("as-enters choice freezes entry refs through serialization and another visit") {
        val initial = inZone(Zone.HAND)
        val entered = ZoneTransitionService.moveToZone(initial, cardId, Zone.BATTLEFIELD).state
        val paused = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements.pauseForEntersWithChoice(
            state = entered, entityId = cardId, controllerId = owner,
            cardComponent = entered.getEntity(cardId)!!.get<CardComponent>()!!,
            choice = com.wingedsheep.sdk.scripting.EntersWithChoice(com.wingedsheep.sdk.scripting.ChoiceType.COLOR),
            fromZone = Zone.HAND, entryOldObject = initial.objectRef(cardId), entryNewObject = entered.objectRef(cardId)
        )!!
        val decoded = json.decodeFromString(GameState.serializer(),
            json.encodeToString(GameState.serializer(), paused.state))
        val movedAgain = ZoneTransitionService.moveToZone(decoded, cardId, Zone.EXILE).state
        val frame = movedAgain.continuationStack.last() as com.wingedsheep.engine.core.EntersWithChoiceOnBattlefieldContinuation
        frame.entryOldObject shouldBe initial.objectRef(cardId)
        frame.entryNewObject shouldBe entered.objectRef(cardId)
        frame.entryNewObject shouldNotBe movedAgain.objectRef(cardId)
    }

    test("entry effect execution roots bind the entered object with an independent scope") {
        val definition = com.wingedsheep.sdk.model.CardDefinition.creature(
            name = "Entry Identity Root", manaCost = ManaCost.parse("{1}"), subtypes = emptySet(),
            power = 1, toughness = 1,
            script = com.wingedsheep.sdk.model.CardScript(replacementEffects = listOf(
                com.wingedsheep.sdk.scripting.OnEnterRunEffect(com.wingedsheep.sdk.dsl.Effects.GainLife(1))
            )))
        val registry = CardRegistry().also { it.register(definition) }
        val initial = inZone(Zone.BATTLEFIELD).withEntity(cardId,
            com.wingedsheep.engine.core.CardEntityFactory.create(definition, owner))
        var captured: EffectContext? = null
        com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements.runOnEnterRunEffect(
            initial, cardId, owner, registry, effectExecutor = { next, _, context ->
                captured = context
                com.wingedsheep.engine.core.EffectResult.success(next)
            })
        captured!!.objectReferences.captured shouldBe true
        captured!!.objectReferences.source shouldBe initial.objectRef(cardId)
        captured!!.objectReferences.origin shouldBe initial.objectRef(cardId)
        captured!!.objectReferences.resolutionKey shouldBe "entry:$cardId:${initial.objectRef(cardId)!!.generation}"
        val later = ZoneTransitionService.moveToZone(initial, cardId, Zone.EXILE).state
        captured!!.withCurrentObjectReferences(later).sourceReferenceLost shouldBe true

        val landDefinition = definition.copy(name = "Entry Identity Land", typeLine = TypeLine(cardTypes = setOf(CardType.LAND)))
        registry.register(landDefinition)
        val services = com.wingedsheep.engine.core.EngineServices(registry)
        val handler = com.wingedsheep.engine.handlers.actions.land.PlayLandHandler(
            registry, services.triggerDetector, services.triggerProcessor, services.conditionEvaluator,
            effectExecutor = { next, _, context ->
                captured = context
                com.wingedsheep.engine.core.EffectResult.success(next)
            }, sbaChecker = services.sbaChecker)
        val hand = inZone(Zone.HAND).withEntity(cardId,
            com.wingedsheep.engine.core.CardEntityFactory.create(landDefinition, owner))
            .copy(activePlayerId = owner, priorityPlayerId = owner,
                phase = com.wingedsheep.sdk.core.Phase.PRECOMBAT_MAIN,
                step = com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)
        captured = null
        val played = handler.execute(hand, com.wingedsheep.engine.core.PlayLand(owner, cardId))
        captured!!.objectReferences.captured shouldBe true
        captured!!.objectReferences.source shouldBe played.state.objectRef(cardId)
        captured!!.objectReferences.source shouldNotBe hand.objectRef(cardId)
        captured!!.objectReferences.origin shouldBe played.state.objectRef(cardId)
    }

})
