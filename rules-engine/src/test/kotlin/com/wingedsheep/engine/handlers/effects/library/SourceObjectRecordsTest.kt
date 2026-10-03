package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.SourceObjectsRecordedEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SourceObjectRecordsTest : FunSpec({
    val owner = EntityId.generate()
    val source = EntityId.generate()
    val otherSource = EntityId.generate()
    val target = EntityId.generate()
    val otherTarget = EntityId.generate()
    val recorder = RecordSourceObjectsExecutor()
    val evaluator = PredicateEvaluator(cardRegistry = null)
    val gatherer = GatherCardsExecutor(evaluator)
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    fun initial(): GameState {
        var state = GameState(turnOrder = listOf(owner)).withEntity(owner, ComponentContainer.EMPTY)
        for ((index, id) in listOf(source, otherSource, target, otherTarget).withIndex()) {
            state = state.withEntity(id, ComponentContainer.of(
                CardComponent("Land $index", "Land $index", ManaCost(emptyList()),
                    TypeLine(cardTypes = setOf(CardType.LAND)), ownerId = owner),
                OwnerComponent(owner), ControllerComponent(owner), BattlefieldEntryTimestampComponent(index.toLong()),
            )).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
        }
        return state
    }
    fun context(state: GameState, id: EntityId = source, ids: List<EntityId> = listOf(target)) = EffectContext(
        sourceId = id, controllerId = owner,
        objectReferences = ObjectReferenceEnvironment(captured = true, origin = state.objectRef(id), source = state.objectRef(id)),
        sourceBattlefieldTimestamp = state.getEntity(id)?.get<BattlefieldEntryTimestampComponent>()?.timestamp,
        pipeline = PipelineState(storedCollections = mapOf("input" to ids)),
    )
    fun record(state: GameState, ctx: EffectContext, key: String = "marked") =
        recorder.execute(state, RecordSourceObjectsEffect("input", key), ctx)
    fun gather(state: GameState, ctx: EffectContext, excluding: String? = null): List<EntityId>? {
        val marked = gatherer.execute(state, GatherCardsEffect(CardSource.SourceLinkedBattlefield("marked"), "out"), ctx)
            .updatedCollections["out"] ?: return null
        if (excluding == null) return marked
        val omitted = gatherer.execute(state, GatherCardsEffect(CardSource.SourceLinkedBattlefield(excluding), "omitted"), ctx)
            .updatedCollections["omitted"].orEmpty()
        return FilterCollectionExecutor(evaluator).execute(state, FilterCollectionEffect(
            from = "out", collectionFilter = CollectionFilter.ExcludeOtherCollection("omitted"), storeMatching = "remaining"
        ), ctx.copy(pipeline = PipelineState(storedCollections = mapOf("out" to marked, "omitted" to omitted))))
            .updatedCollections["remaining"]
    }
    fun blink(state: GameState, id: EntityId): GameState = state
        .moveToZone(id, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.EXILE))
        .moveToZone(id, ZoneKey(owner, Zone.EXILE), ZoneKey(owner, Zone.BATTLEFIELD))

    test("records are immutable ordered distinct per source and emit only actual additions") {
        val state = initial()
        val ctx = context(state, ids = listOf(target, target, otherTarget))
        val result = record(state, ctx)
        state.sourceObjectRecords shouldBe emptyMap()
        (result.events.single() as SourceObjectsRecordedEvent).objectIds shouldBe listOf(target, otherTarget)
        gather(result.state, ctx) shouldBe listOf(target, otherTarget)
        record(result.state, ctx).events shouldBe emptyList()
        gather(result.state, context(state, otherSource)) shouldBe emptyList()
        val second = record(result.state, context(state, otherSource, listOf(otherTarget))).state
        gather(second, context(state, otherSource)) shouldBe listOf(otherTarget)
        gather(second, ctx) shouldBe listOf(target, otherTarget)
    }
    test("source blink creates a new history while old delayed context still reads its history") {
        val state = initial()
        val old = context(state)
        val marked = record(state, old).state
        val returned = blink(marked, source)
        gather(returned, context(returned).copy(sourceBattlefieldTimestamp = null)) shouldBe emptyList()
        gather(returned, old) shouldBe listOf(target)
        val late = record(returned, old.copy(pipeline = PipelineState(storedCollections = mapOf("input" to listOf(otherTarget))))).state
        gather(late, old) shouldBe listOf(target, otherTarget)
        gather(late, context(returned).copy(sourceBattlefieldTimestamp = null)) shouldBe emptyList()
    }
    test("entry and departure triggers share the entered visit even when entry resolves after departure") {
        val definition = com.wingedsheep.sdk.dsl.card("Land 0") {
            typeLine = "Land"
            triggeredAbility {
                trigger = com.wingedsheep.sdk.dsl.Triggers.self.enters()
                effect = com.wingedsheep.sdk.dsl.Effects.DrawCards(1)
            }
            triggeredAbility {
                trigger = com.wingedsheep.sdk.dsl.Triggers.self.dies()
                effect = com.wingedsheep.sdk.dsl.Effects.DrawCards(1)
            }
        }
        val registry = com.wingedsheep.engine.registry.CardRegistry().also { it.register(definition) }
        val detector = com.wingedsheep.engine.event.TriggerDetector(registry,
            predicateEvaluator = evaluator, conditionEvaluator = evaluator.conditions)
        val before = initial().moveToZone(source, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.HAND))
        val entered = before.moveToZone(source, ZoneKey(owner, Zone.HAND), ZoneKey(owner, Zone.BATTLEFIELD))
            .updateEntity(source) { it.with(BattlefieldEntryTimestampComponent(100)) }
        val entry = com.wingedsheep.engine.core.ZoneChangeEvent(source, definition.name, Zone.HAND,
            Zone.BATTLEFIELD, owner, oldObject = before.objectRef(source), newObject = entered.objectRef(source),
            enteredBattlefieldTimestamp = 100)
        val entryTrigger = detector.detectTriggers(entered, listOf(entry)).single()
        entryTrigger.objectReferences.origin shouldBe entered.objectRef(source)
        val entryContext = EffectContext(sourceId = source, controllerId = owner,
            sourceBattlefieldTimestamp = entryTrigger.sourceBattlefieldTimestamp,
            objectReferences = entryTrigger.objectReferences,
            pipeline = PipelineState(storedCollections = mapOf("input" to listOf(target))))
        val markedOnEntry = record(entered, entryContext).state
        gather(markedOnEntry, context(markedOnEntry)) shouldBe listOf(target)
        val departed = markedOnEntry.moveToZone(source, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.GRAVEYARD))
        val death = com.wingedsheep.engine.core.ZoneChangeEvent(source, definition.name, Zone.BATTLEFIELD,
            Zone.GRAVEYARD, owner, oldObject = entered.objectRef(source), newObject = departed.objectRef(source),
            lastKnown = com.wingedsheep.engine.state.components.stack.EntitySnapshot.fromProjection(source, markedOnEntry)
                .copy(cardDefinitionId = definition.name))
        val deathTrigger = detector.detectTriggers(departed, listOf(death)).single()
        val deathContext = EffectContext(sourceId = source, controllerId = owner,
            sourceBattlefieldTimestamp = deathTrigger.sourceBattlefieldTimestamp,
            objectReferences = deathTrigger.objectReferences)
        gather(departed, deathContext) shouldBe listOf(target)
        val late = record(departed, entryContext.copy(pipeline = PipelineState(
            storedCollections = mapOf("input" to listOf(otherTarget))))).state
        gather(late, deathContext) shouldBe listOf(target, otherTarget)
        val delayed = com.wingedsheep.engine.handlers.effects.composite.CreateDelayedTriggerExecutor(evaluator.amounts)
            .execute(late, CreateDelayedTriggerEffect(step = com.wingedsheep.sdk.core.Step.UPKEEP,
                effect = GatherCardsEffect(CardSource.SourceLinkedBattlefield("marked"), "remembered")), entryContext).state
        val delayedTrigger = detector.detectDelayedTriggers(delayed.copy(turnNumber = 2), com.wingedsheep.sdk.core.Step.UPKEEP)
            .first.single()
        gather(delayed, EffectContext(sourceId = source, controllerId = owner,
            objectReferences = delayedTrigger.objectReferences)) shouldBe listOf(target, otherTarget)
    }
    test("source and target token disappearance do not lose source history or resurrect target") {
        val state = initial()
        val ctx = context(state)
        val marked = record(state, ctx).state
        val sourceGone = marked.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), source).withoutEntity(source)
        gather(sourceGone, ctx.copy(sourceBattlefieldTimestamp = null)) shouldBe listOf(target)
        val targetGone = sourceGone.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), target).withoutEntity(target)
        gather(targetGone, ctx) shouldBe emptyList()
    }
    test("departed and returned targets never match their old recorded object") {
        val state = initial()
        val ctx = context(state)
        val marked = record(state, ctx).state
        val returned = blink(marked, target)
        gather(returned, ctx) shouldBe emptyList()
        val newRecord = record(returned, ctx).state
        gather(newRecord, ctx) shouldBe listOf(target)
    }
    test("phasing temporarily suppresses objects without erasing history") {
        val state = initial()
        val ctx = context(state)
        val marked = record(state, ctx).state
        val phased = marked.updateEntity(target) { it.with(PhasedOutComponent(owner)) }
        gather(phased, ctx) shouldBe emptyList()
        gather(phased.updateEntity(target) { it.without<PhasedOutComponent>() }, ctx) shouldBe listOf(target)
        record(phased, ctx, "cleaned").state shouldBe phased
    }
    test("exclusion history is slot-local and identity-safe") {
        val state = initial()
        val ctx = context(state, ids = listOf(target, otherTarget))
        val marked = record(state, ctx).state
        val cleaned = record(marked, context(state), "cleaned").state
        gather(cleaned, ctx, "cleaned") shouldBe listOf(otherTarget)
        val returned = blink(cleaned, target)
        val remarked = record(returned, ctx).state
        gather(remarked, ctx, "cleaned") shouldBe listOf(otherTarget, target)
        gather(remarked, ctx, "unknown") shouldBe listOf(otherTarget, target)
    }
    test("empty missing off-battlefield collections and missing sources do nothing") {
        val state = initial()
        record(state, context(state, ids = emptyList())).state shouldBe state
        record(state, context(state).copy(pipeline = PipelineState.EMPTY)).state shouldBe state
        record(state, context(state).copy(sourceId = null)).state shouldBe state
        val away = state.moveToZone(target, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.GRAVEYARD))
        record(away, context(state)).state shouldBe away
    }
    test("repeating delayed triggers retain departed source visits across detection and resolution") {
        val state = initial()
        val ctx = context(state)
        val marked = record(state, ctx).state
        val delayed = com.wingedsheep.engine.handlers.effects.composite.CreateDelayedTriggerExecutor(evaluator.amounts)
            .execute(marked, CreateDelayedTriggerEffect(
                step = com.wingedsheep.sdk.core.Step.UPKEEP,
                effect = GatherCardsEffect(CardSource.SourceLinkedBattlefield("marked"), "remembered"),
                repeatAtEachMatchingStep = true, expiry = DelayedTriggerExpiry.Never,
            ), ctx).state
        val gone = delayed.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), source).withoutEntity(source)
        val detector = com.wingedsheep.engine.event.TriggerDetector(
            com.wingedsheep.engine.registry.CardRegistry(), predicateEvaluator = evaluator, conditionEvaluator = evaluator.conditions)
        for (turn in listOf(2, 4)) {
            val (triggers, consumed) = detector.detectDelayedTriggers(gone.copy(turnNumber = turn), com.wingedsheep.sdk.core.Step.UPKEEP)
            consumed shouldBe emptySet()
            val trigger = triggers.single()
            trigger.objectReferences.origin shouldBe ctx.objectReferences.origin
            val resolving = EffectContext(sourceId = trigger.sourceId, controllerId = trigger.controllerId,
                objectReferences = trigger.objectReferences)
            val result = gatherer.execute(gone, trigger.ability.effect as GatherCardsEffect, resolving)
            result.updatedCollections["remembered"] shouldBe listOf(target)
        }
    }
    test("serialized state context event and SDK AST retain original visit identities") {
        val state = initial()
        val ctx = context(state)
        val result = record(state, ctx)
        val restored = json.decodeFromString<GameState>(json.encodeToString(result.state))
        gather(restored, json.decodeFromString<EffectContext>(json.encodeToString(ctx))) shouldBe listOf(target)
        val effect: Effect = RecordSourceObjectsEffect("input", "marked")
        json.decodeFromString<Effect>(json.encodeToString(effect)) shouldBe effect
        val source: CardSource = CardSource.SourceLinkedBattlefield("marked")
        json.decodeFromString<CardSource>(json.encodeToString(source)) shouldBe source
        val event = result.events.single()
        json.decodeFromString<com.wingedsheep.engine.core.GameEvent>(json.encodeToString(event)) shouldBe event
    }
    test("collection filter reads projected characteristics of remembered objects") {
        val state = initial()
        val ctx = context(state)
        val marked = record(state, ctx).state.copy(floatingEffects = listOf(ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(layer = Layer.TYPE,
                modification = SerializableModification.AddType(CardType.CREATURE.name), affectedEntities = setOf(target)),
            duration = Duration.EndOfTurn, sourceId = source, controllerId = owner, timestamp = 100,
        )))
        val gathered = gather(marked, ctx)!!
        val filtered = FilterCollectionExecutor(evaluator).execute(marked,
            FilterCollectionEffect(from = "input", filter = GameObjectFilter.Creature, storeMatching = "creatures"),
            ctx.copy(pipeline = PipelineState(storedCollections = mapOf("input" to gathered))))
        filtered.updatedCollections["creatures"] shouldBe listOf(target)
    }
})
