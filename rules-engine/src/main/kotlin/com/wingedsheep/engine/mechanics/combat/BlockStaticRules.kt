package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/** Rule-changing block statics are read after projection, with the same source on every path. */
class BlockStaticRules(
    private val state: GameState,
    registry: CardRegistry,
    private val predicates: PredicateEvaluator,
) {
    private data class Entry(val holder: EntityId, val ability: StaticAbility)
    private val projected = state.projectedState
    private val battlefield = state.getBattlefield().toSet()
    private val entries: List<Entry> = buildList {
        fun containsRule(ability: StaticAbility): Boolean = when (ability) {
            is ConditionalStaticAbility -> containsRule(ability.ability)
            is CompositeStaticAbility -> ability.abilities.any(::containsRule)
            is CanBlockAnyNumber, is MustBlockEachAttacker -> true
            else -> false
        }
        fun collect(holder: EntityId, ability: StaticAbility) {
            when (ability) {
                is ConditionalStaticAbility -> if (containsRule(ability.ability) && predicates.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = holder, controllerId = projected.getController(holder) ?: holder))) {
                    collect(holder, ability.ability)
                }
                is CompositeStaticAbility -> ability.abilities.forEach { collect(holder, it) }
                is CanBlockAnyNumber, is MustBlockEachAttacker -> add(Entry(holder, ability))
                else -> Unit
            }
        }
        for (holder in battlefield) {
            val container = state.getEntity(holder) ?: continue
            if (projected.hasLostAllAbilities(holder)) continue
            if (!container.has<FaceDownComponent>()) {
                val card = container.get<CardComponent>() ?: continue
                val definition = registry.getCard(card.cardDefinitionId) ?: continue
                val text = TextChanges.of(state, holder)
                for (ability in RoomFaceStatics.activeStaticAbilities(container, definition)) {
                    collect(holder, if (text == null) ability else ability.applyTextReplacement(text))
                }
            }
        }
        // These resolved grants change combat rules rather than the holder's characteristics.
        // Removing its abilities does not end a fixed-duration rule permission/requirement.
        for (grant in state.grantedStaticAbilities) {
            if (grant.entityId !in battlefield && grant.entityId !in state.turnOrder) continue
            if (GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) {
                collect(grant.entityId, grant.ability)
            }
        }
    }

    private fun covers(holder: EntityId, filter: GroupFilter, blocker: EntityId): Boolean {
        val inScope = when (val scope = filter.scope) {
            Scope.Self -> holder == blocker
            Scope.Battlefield -> true
            Scope.AttachedTo -> state.getEntity(holder)?.get<AttachedToComponent>()?.targetId == blocker
            Scope.SoulbondPair -> SoulbondPairing.isInPairOf(state, holder, blocker)
            is Scope.Specific -> scope.entityId == blocker
        }
        return inScope && !(filter.excludeSelf && holder == blocker) &&
            predicates.matches(state, projected, blocker, filter.baseFilter,
                PredicateContext(sourceId = holder, controllerId = projected.getController(holder) ?: holder))
    }

    fun maxBlocks(blocker: EntityId): Int =
        if (entries.any { it.ability is CanBlockAnyNumber && covers(it.holder, it.ability.filter, blocker) })
            Int.MAX_VALUE
        else (1L + projected.getAdditionalBlockCount(blocker)).coerceIn(1, Int.MAX_VALUE.toLong()).toInt()

    fun mustBlockEach(blocker: EntityId): Boolean = eachRequirementCount(blocker) > 0

    fun eachRequirementCount(blocker: EntityId): Int = entries.count {
        it.ability is MustBlockEachAttacker && covers(it.holder, it.ability.filter, blocker)
    }
}
