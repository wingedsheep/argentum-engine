package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Goaded
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Who goads each creature (CR 701.15). Two sources feed the one set:
 *  - the [GoadedComponent] designation a resolved goad effect leaves, which expires on its own;
 *  - a [Goaded] static — printed on a battlefield permanent (Martial Impetus on the creature it
 *    enchants), whose goader is that permanent's projected controller, or granted by a resolving
 *    effect, whose goader is the grant's controller. A face-down or ability-stripped host
 *    contributes nothing.
 *
 * Read after projection, like [BlockStaticRules]: build once per declaration, ask per creature.
 */
class GoadRules(
    private val state: GameState,
    registry: CardRegistry,
    private val predicates: PredicateEvaluator,
) {
    private data class Entry(val holder: EntityId, val goader: EntityId, val ability: Goaded)
    private val projected = state.projectedState
    private val entries: List<Entry> = buildList {
        fun collect(holder: EntityId, goader: EntityId, ability: StaticAbility) {
            when (ability) {
                is ConditionalStaticAbility -> if (predicates.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = holder, controllerId = goader))) {
                    collect(holder, goader, ability.ability)
                }
                is CompositeStaticAbility -> ability.abilities.forEach { collect(holder, goader, it) }
                is Goaded -> add(Entry(holder, goader, ability))
                else -> Unit
            }
        }
        val battlefield = state.getBattlefield()
        for (holder in battlefield) {
            val container = state.getEntity(holder) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(holder)) continue
            val card = container.get<CardComponent>() ?: continue
            val definition = registry.getCard(card.cardDefinitionId) ?: continue
            val goader = projected.getController(holder) ?: continue
            val text = TextChanges.of(state, holder)
            for (ability in RoomFaceStatics.activeStaticAbilities(container, definition)) {
                collect(holder, goader, if (text == null) ability else ability.applyTextReplacement(text))
            }
        }
        for (grant in state.grantedStaticAbilities) {
            if (grant.entityId !in battlefield) continue
            if (!GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) continue
            val goader = grant.controllerId ?: projected.getController(grant.entityId) ?: continue
            collect(grant.entityId, goader, grant.ability)
        }
    }

    /** Every player goading [creatureId]; empty when it isn't goaded. */
    fun goadersOf(creatureId: EntityId): Set<EntityId> {
        val designated = state.getEntity(creatureId)?.get<GoadedComponent>()?.goaderIds.orEmpty()
        if (entries.isEmpty()) return designated
        val fromStatics = entries.filter { covers(it, creatureId) }.map { it.goader }
        return if (fromStatics.isEmpty()) designated else designated + fromStatics
    }

    fun isGoaded(creatureId: EntityId): Boolean = goadersOf(creatureId).isNotEmpty()

    private fun covers(entry: Entry, creatureId: EntityId): Boolean {
        val filter = entry.ability.filter
        val holder = entry.holder
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
                PredicateContext(sourceId = holder, controllerId = entry.goader))
    }
}
