package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * The combat statics that are enforced at declaration rather than projected ([GoadRules],
 * [DirectedAttackRequirements]) read the board the same way: every live static of a kind on a
 * battlefield permanent — not face down, not ability-stripped, text changes applied, conditions
 * evaluated — plus the grants of that kind still in force.
 */
internal object CombatStaticScan {

    /** One static of the kind asked for: its [holder] and the player it acts for. */
    data class Entry<T : StaticAbility>(val holder: EntityId, val controller: EntityId, val ability: T)

    fun <T : StaticAbility> collect(
        state: GameState,
        registry: CardRegistry,
        predicates: PredicateEvaluator,
        pick: (StaticAbility) -> T?,
    ): List<Entry<T>> = buildList {
        val projected = state.projectedState
        fun visit(holder: EntityId, controller: EntityId, ability: StaticAbility) {
            when (ability) {
                is ConditionalStaticAbility -> if (predicates.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = holder, controllerId = controller))) {
                    visit(holder, controller, ability.ability)
                }
                is CompositeStaticAbility -> ability.abilities.forEach { visit(holder, controller, it) }
                else -> pick(ability)?.let { add(Entry(holder, controller, it)) }
            }
        }
        val battlefield = state.getBattlefield()
        for (holder in battlefield) {
            val container = state.getEntity(holder) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(holder)) continue
            val card = container.get<CardComponent>() ?: continue
            val definition = registry.getCard(card.cardDefinitionId) ?: continue
            val controller = projected.getController(holder) ?: continue
            val text = TextChanges.of(state, holder)
            for (ability in RoomFaceStatics.activeStaticAbilities(container, definition)) {
                visit(holder, controller, if (text == null) ability else ability.applyTextReplacement(text))
            }
        }
        for (grant in state.grantedStaticAbilities) {
            if (grant.entityId !in battlefield) continue
            if (!GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) continue
            val controller = grant.controllerId ?: projected.getController(grant.entityId) ?: continue
            visit(grant.entityId, controller, grant.ability)
        }
    }

    /** Whether [filter], read from [holder] for [controller], reaches the creature [creatureId]. */
    fun covers(
        state: GameState,
        predicates: PredicateEvaluator,
        holder: EntityId,
        controller: EntityId,
        filter: GroupFilter,
        creatureId: EntityId,
    ): Boolean {
        val projected = state.projectedState
        val inScope = when (val scope = filter.scope) {
            Scope.Self -> holder == creatureId
            Scope.Battlefield -> true
            Scope.AttachedTo -> state.getEntity(holder)?.get<AttachedToComponent>()?.targetId == creatureId
            Scope.SoulbondPair -> SoulbondPairing.isInPairOf(state, holder, creatureId)
            Scope.ChosenCreature -> state.chosenCreatureOf(holder) == creatureId
            is Scope.Specific -> scope.entityId == creatureId
        }
        return inScope && !(filter.excludeSelf && holder == creatureId) &&
            projected.isCreature(creatureId) &&
            predicates.matches(state, projected, creatureId, filter.baseFilter,
                PredicateContext(sourceId = holder, controllerId = controller))
    }
}
