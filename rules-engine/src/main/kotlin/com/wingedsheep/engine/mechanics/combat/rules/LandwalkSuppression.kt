package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Single source of truth for "which of this attacker's landwalk keywords are being ignored for
 * blocking?" — the [SuppressLandwalkForGroup] statics ("Creatures with swampwalk can be blocked as
 * though they didn't have swampwalk", Quagmire / Ur-Drago).
 *
 * Every permanent on the battlefield carrying such a static is considered: self scope covers the
 * permanent itself, attached scope the permanent it's attached to, battlefield scope any attacker
 * its group filter matches (with that permanent as predicate source and its controller as "you",
 * against the current projection). A face-down permanent (CR 708.2) or one that has lost all
 * abilities contributes nothing. Runtime grants in `GameState.grantedStaticAbilities` count while
 * their duration holds, with the grant's controller as "you".
 *
 * Read by the engine's [LandwalkRule] and by the AI's combat math, so the blockers the AI
 * predicts agree with the ones the engine allows.
 */
object LandwalkSuppression {

    /** The landwalk keywords of [attackerId] that can be blocked as though it didn't have them. */
    fun suppressedLandwalks(
        state: GameState,
        attackerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Set<Keyword> {
        val projected = state.projectedState
        var suppressed: MutableSet<Keyword>? = null
        fun consider(holderId: EntityId, you: EntityId, ability: SuppressLandwalkForGroup) {
            if (suppressed?.contains(ability.landwalk) == true) return
            if (covers(state, holderId, you, attackerId, ability, predicateEvaluator)) {
                (suppressed ?: mutableSetOf<Keyword>().also { suppressed = it }).add(ability.landwalk)
            }
        }
        for (permanentId in state.getBattlefield()) {
            val permanent = state.getEntity(permanentId) ?: continue
            if (permanent.has<FaceDownComponent>()) continue
            val permCard = permanent.get<CardComponent>() ?: continue
            val abilities = cardRegistry.getCard(permCard.cardDefinitionId)?.staticAbilities ?: continue
            if (abilities.none { it is SuppressLandwalkForGroup }) continue
            if (projected.hasLostAllAbilities(permanentId)) continue
            val you = projected.getController(permanentId) ?: continue
            for (ability in abilities) if (ability is SuppressLandwalkForGroup) consider(permanentId, you, ability)
        }
        // Runtime grants — "target creature can be blocked as though it didn't have swampwalk this
        // turn" is Effects.GrantStaticAbility(SuppressLandwalkForGroup(kw, GroupFilter.source()), t).
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability as? SuppressLandwalkForGroup ?: continue
            if (!state.getBattlefield().contains(grant.entityId)) continue
            if (!GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) continue
            val you = grant.controllerId ?: projected.getController(grant.entityId) ?: continue
            consider(grant.entityId, you, ability)
        }
        return suppressed ?: emptySet()
    }

    private fun covers(
        state: GameState,
        holderId: EntityId,
        you: EntityId,
        attackerId: EntityId,
        ability: SuppressLandwalkForGroup,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = when (val scope = ability.filter.scope) {
        is Scope.Self -> holderId == attackerId
        is Scope.Specific -> scope.entityId == attackerId
        is Scope.AttachedTo -> state.getEntity(holderId)?.get<AttachedToComponent>()?.targetId == attackerId
        is Scope.SoulbondPair -> SoulbondPairing.isInPairOf(state, holderId, attackerId)
        is Scope.ChosenCreature -> state.chosenCreatureOf(holderId) == attackerId
        is Scope.Battlefield ->
            !(ability.filter.excludeSelf && holderId == attackerId) &&
                predicateEvaluator.matches(
                    state, state.projectedState, attackerId, ability.filter.baseFilter,
                    PredicateContext(controllerId = you, sourceId = holderId)
                )
    }
}
