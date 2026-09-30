package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.layers.AffectsFilter
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectData
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.Modification
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ControlHistoryTest : FunSpec({
    val owner = EntityId.generate()
    val other = EntityId.generate()
    val id = EntityId.generate()
    val evaluator = PredicateEvaluator(cardRegistry = null)
    val filter = GameObjectFilter.Any.controlledSinceTurnBegan()
    fun board(): GameState = GameState(activePlayerId = owner, turnOrder = listOf(owner, other), turnNumber = 4)
        .withEntity(owner, ComponentContainer.EMPTY).withEntity(other, ComponentContainer.EMPTY)
        .withEntity(id, ComponentContainer.of(
            CardComponent("History permanent", "History permanent", ManaCost(emptyList()),
                TypeLine(cardTypes = setOf(CardType.ARTIFACT)), ownerId = owner, baseKeywords = setOf(Keyword.HASTE)),
            OwnerComponent(owner), ControllerComponent(owner)))
        .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
    fun matches(state: GameState) = evaluator.matches(state, state.projectedState, id, filter, PredicateContext(owner))

    test("snapshot is independent of creature type and haste") {
        val state = ControlHistory.beginTurn(board())
        matches(state) shouldBe true
        val changed = state.updateEntity(id) { it.with(ControllerComponent(other)) }
        matches(changed) shouldBe false
        matches(ControlHistory.record(changed, emptyList())) shouldBe false
        matches(state) shouldBe true
    }

    test("ordinary tap instructions preserve history while a base control change interrupts it") {
        val state = ControlHistory.beginTurn(board())
        val (tapped, event) = tap(state, id)
        val recorded = ControlHistory.record(tapped, listOfNotNull(event))
        recorded.controlAtTurnStart shouldBe state.controlAtTurnStart
        matches(recorded) shouldBe true
        val changed = recorded.updateEntity(id) { it.with(ControllerComponent(other)) }
        val interrupted = ControlHistory.record(changed, emptyList())
        interrupted.controlAtTurnStart?.containsKey(id) shouldBe false
        matches(interrupted.updateEntity(id) { it.with(ControllerComponent(owner)) }) shouldBe false
    }

    test("floating control changes interrupt history without an explicit control event") {
        val state = ControlHistory.beginTurn(board())
        val stolen = state.addFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(other),
            affectedEntities = setOf(id),
            duration = Duration.EndOfTurn,
            context = EffectContext(id, other)
        )
        stolen.getEntity(id)?.get<ControllerComponent>()?.playerId shouldBe owner
        stolen.projectedState.getController(id) shouldBe other
        val interrupted = ControlHistory.record(stolen, emptyList())
        interrupted.controlAtTurnStart?.containsKey(id) shouldBe false
        matches(interrupted.copy(floatingEffects = emptyList())) shouldBe false
    }

    test("static control changes and their departure use projected continuity") {
        val state = ControlHistory.beginTurn(board())
        val controlEffect = ContinuousEffectSourceComponent(listOf(ContinuousEffectData(
            modification = Modification.ChangeController(other),
            affectsFilter = AffectsFilter.Self
        )))
        val stolen = state.updateEntity(id) { it.with(controlEffect) }
        stolen.projectedState.getController(id) shouldBe other
        val interrupted = ControlHistory.record(stolen, emptyList())
        interrupted.controlAtTurnStart?.containsKey(id) shouldBe false
        matches(interrupted.updateEntity(id) { it.without<ContinuousEffectSourceComponent>() }) shouldBe false

        val controlledAtStart = ControlHistory.beginTurn(stolen)
        matches(ControlHistory.record(controlledAtStart, emptyList())) shouldBe true
        val released = controlledAtStart.updateEntity(id) { it.without<ContinuousEffectSourceComponent>() }
        ControlHistory.record(released, emptyList()).controlAtTurnStart?.containsKey(id) shouldBe false
    }

    test("phased controller survives removal of a control effect while phased out") {
        val stolen = board().addFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(other),
            affectedEntities = setOf(id),
            duration = Duration.EndOfTurn,
            context = EffectContext(id, other)
        )
        val state = ControlHistory.beginTurn(stolen)
        val phased = state.updateEntity(id) { it.with(PhasedOutComponent(other)) }
            .copy(floatingEffects = emptyList())
        val recorded = ControlHistory.record(phased, emptyList())
        recorded.controlAtTurnStart shouldBe state.controlAtTurnStart
        val back = recorded.updateEntity(id) { it.without<PhasedOutComponent>() }
        ControlHistory.record(back, emptyList()).controlAtTurnStart?.containsKey(id) shouldBe false
    }

    test("losing and regaining control never restores continuity even in one event batch") {
        val state = ControlHistory.beginTurn(board())
        val interrupted = ControlHistory.record(state, listOf(
            ControlChangedEvent(id, "History permanent", owner, other),
            ControlChangedEvent(id, "History permanent", other, owner)))
        matches(interrupted) shouldBe false
        matches(ControlHistory.beginTurn(interrupted.copy(turnNumber = 5))) shouldBe true
    }

    test("a redundant controller assignment does not interrupt control") {
        val state = ControlHistory.beginTurn(board())
        matches(ControlHistory.record(state, listOf(ControlChangedEvent(id, "History permanent", owner, owner)))) shouldBe true
    }

    test("entry and blink invalidate the recorded battlefield visit") {
        val state = ControlHistory.beginTurn(board())
        val blinked = state.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
            .addToZone(ZoneKey(owner, Zone.EXILE), id)
            .removeFromZone(ZoneKey(owner, Zone.EXILE), id)
            .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
        matches(blinked) shouldBe false
        matches(ControlHistory.record(blinked, emptyList())) shouldBe false
        val hand = state.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
            .addToZone(ZoneKey(owner, Zone.HAND), id)
        matches(hand) shouldBe false
    }

    test("imported boards distinguish existing permanents from this-turn entrants") {
        matches(ControlHistory.initialize(board())) shouldBe true
        val entered = board().updateEntity(id) { it.with(EnteredThisTurnComponent) }
        matches(entered) shouldBe false
        matches(ControlHistory.initialize(entered)) shouldBe false
    }

    test("phasing preserves the visit and its projected controller") {
        val state = ControlHistory.beginTurn(board().updateEntity(id) { it.with(ControllerComponent(other)) })
        val phased = state.updateEntity(id) { it.with(PhasedOutComponent(other)) }
        matches(ControlHistory.record(phased, emptyList())) shouldBe true
        val back = phased.updateEntity(id) { it.without<PhasedOutComponent>() }
        matches(ControlHistory.record(back, emptyList())) shouldBe true
    }

    test("attack history remains true after control changes") {
        val state = board().updateEntity(owner) { it.with(PlayerAttackersThisTurnComponent(attackerIds = setOf(id))) }
            .updateEntity(id) { it.with(ControllerComponent(other)) }
        evaluator.matches(state, state.projectedState, id, GameObjectFilter.Any.attackedThisTurn(), PredicateContext(owner)) shouldBe true
    }

    test("the production turn boundary resets history even when untap is skipped") {
        val state = ControlHistory.record(ControlHistory.beginTurn(board()),
            listOf(ControlChangedEvent(id, "History permanent", owner, other)))
            .updateEntity(other) { it.with(SkipNextUntapStepComponent()) }
        matches(state) shouldBe false
        val started = EngineServices(CardRegistry()).turnManager.startTurn(state, other)
        started.error shouldBe null
        started.state.turnNumber shouldBe 5
        matches(started.state) shouldBe true
    }

    test("serialized history retains control interruptions and object identities") {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        val state = ControlHistory.beginTurn(board())
        val restored = json.decodeFromString<GameState>(json.encodeToString(state))
        restored.controlAtTurnStart shouldBe state.controlAtTurnStart
        matches(restored) shouldBe true
    }
})
