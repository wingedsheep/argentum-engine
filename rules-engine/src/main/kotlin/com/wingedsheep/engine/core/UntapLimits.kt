package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

/** Restrictions on the simultaneous untap, evaluated before any permanent untaps. */
internal fun untapLimitChoices(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    wouldUntap: List<EntityId>,
): List<UntapLimitChoice> {
    val projected = state.projectedState
    val byController = wouldUntap.groupBy { projected.getController(it) }
    val choices = LinkedHashMap<Set<EntityId>, UntapLimitChoice>()

    fun collect(ability: StaticAbility, source: EntityId, controller: EntityId) {
        when (ability) {
            is ConditionalStaticAbility -> {
                if (predicates.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = source, controllerId = controller))) {
                    collect(ability.ability, source, controller)
                }
            }
            is CompositeStaticAbility -> ability.abilities.forEach { collect(it, source, controller) }
            is UntapLimitPerStep -> {
                val context = PredicateContext(controllerId = controller, sourceId = source)
                // The cap is per player, including when teammates untap in a shared turn.
                for (permanents in byController.values) {
                    val matching = permanents.filter {
                        predicates.matches(state, projected, it, ability.filter, context)
                    }
                    if (matching.size <= ability.max) continue
                    val key = matching.toSet()
                    val old = choices[key]
                    if (old == null || ability.max < old.max) {
                        choices[key] = UntapLimitChoice(matching, ability.max)
                    }
                }
            }
            else -> Unit
        }
    }

    val battlefield = state.getBattlefield()
    for (source in battlefield) {
        val entity = state.getEntity(source) ?: continue
        if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(source)) continue
        val controller = projected.getController(source) ?: continue
        val definition = entity.get<CardComponent>()?.let { registry.getCard(it.cardDefinitionId) } ?: continue
        val text = TextChanges.of(state, source)
        for (ability in RoomFaceStatics.activeStaticAbilities(entity, definition)) {
            collect(if (text == null) ability else ability.applyTextReplacement(text), source, controller)
        }
    }
    for (grant in state.grantedStaticAbilities) {
        val source = grant.entityId
        val player = source in state.turnOrder
        if (!player && (source !in battlefield || projected.hasLostAllAbilities(source))) continue
        if (!GrantDurationGate.holds(state, source, grant.sourceId, grant.duration)) continue
        val controller = if (player) source else projected.getController(source) ?: continue
        // Text changes affect printed rules text, not externally granted abilities.
        collect(grant.ability, source, controller)
    }
    return choices.values.toList()
}
