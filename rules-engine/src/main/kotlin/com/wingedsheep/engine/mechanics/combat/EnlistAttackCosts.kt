package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/** Enlist choices are collected before mana payment, and each paid instance emits its own trigger. */
internal object EnlistAttackCosts {
    fun instances(state: GameState, attackers: Set<EntityId>, cards: CardRegistry): List<EntityId> = buildList {
        val projected = state.projectedState
        for (attacker in attackers) {
            val entity = state.getEntity(attacker) ?: continue
            if (projected.hasLostAllAbilities(attacker)) continue
            val card = entity.get<CardComponent>()?.let { cards.getCard(it.cardDefinitionId) }
            val printed = if (entity.has<FaceDownComponent>()) 0 else card?.staticAbilities?.count { it == Enlist } ?: 0
            val granted = state.grantedStaticAbilities.count {
                it.entityId == attacker && it.ability == Enlist &&
                    com.wingedsheep.engine.mechanics.durations.GrantDurationGate.holds(state, attacker, it.sourceId, it.duration)
            }
            repeat(printed + granted) { add(attacker) }
        }
    }

    fun eligible(state: GameState, attacker: EntityId, attackers: Set<EntityId>, chosen: List<EnlistPayment>): List<EntityId> {
        val projected = state.projectedState
        val controller = projected.getController(attacker)
        val reserved = chosen.mapTo(mutableSetOf()) { it.enlistedId }
        return state.getBattlefield().filter { id ->
            id !in attackers && id !in reserved &&
                state.getEntity(id)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == false && projected.isCreature(id) &&
                projected.getController(id) == controller && state.getEntity(id)?.has<TappedComponent>() == false &&
                (state.getEntity(id)?.has<SummoningSicknessComponent>() == false || projected.hasKeyword(id, Keyword.HASTE))
        }
    }

    fun pay(state: GameState, payments: List<EnlistPayment>): ExecutionResult {
        var next = state
        val events = mutableListOf<GameEvent>()
        for ((attacker, enlisted) in payments) {
            // Mana abilities may have changed control, haste, or the battlefield since the choice.
            if (enlisted !in eligible(next, attacker, setOf(attacker), emptyList())) {
                return ExecutionResult.error(state, "The chosen creature is no longer eligible to be enlisted")
            }
            val controller = state.projectedState.getController(attacker)
                ?: return ExecutionResult.error(state, "Enlisting creature has no controller")
            val attackerName = nameVisibleToAll(state, attacker, state.getEntity(attacker)?.get<CardComponent>()?.name ?: "Creature")
            val enlistedName = nameVisibleToAll(state, enlisted, state.getEntity(enlisted)?.get<CardComponent>()?.name ?: "Creature")
            val (tapped, tapEvents) = tap(next, enlisted, tappedById = controller)
            next = tapped
            events.addAll(tapEvents)
            events.add(EnlistedEvent(attacker, attackerName, enlisted, enlistedName, controller))
            events.add(ReflexiveAbilityTriggeredEvent(
                sourceId = attacker, sourceName = attackerName, controllerId = controller,
                reflexiveEffect = Effects.ModifyStats(
                    DynamicAmount.Max(DynamicAmount.Fixed(0),
                        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.Power)),
                    DynamicAmount.Fixed(0), EffectTarget.Self,
                ),
                descriptionOverride = "$attackerName gets +X/+0 until end of turn, where X is $enlistedName's power",
                carriedObjectReferences = ObjectReferenceEnvironment(
                    captured = true, origin = state.objectRef(attacker), source = state.objectRef(attacker),
                    triggering = state.objectRef(enlisted),
                ),
                carriedTriggerContext = TriggerContext(triggeringEntityId = enlisted,
                    triggeringPlayerId = controller, triggeringObject = state.objectRef(enlisted)),
            ))
        }
        return ExecutionResult.success(next, events)
    }
}
