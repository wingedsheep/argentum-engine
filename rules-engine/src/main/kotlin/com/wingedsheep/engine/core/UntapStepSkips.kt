package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Standing skip restrictions are read before phasing or any other untap-step action. */
internal fun skipsUntapStep(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    player: EntityId,
): Boolean = hasStandingSkip(state, registry, predicates, player, TurnPart.UNTAP_STEP)

/**
 * Whether a standing [SkipStepOrPhase] for [part] applies to [player]: any face-up battlefield
 * permanent that hasn't lost its abilities (Room faces only while unlocked), or any live granted
 * static, whose `player` scope — resolved from that source's controller — includes [player].
 * [ConditionalStaticAbility] and [CompositeStaticAbility] wrappers are unwrapped.
 */
internal fun hasStandingSkip(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    player: EntityId,
    part: TurnPart,
): Boolean {
    val projected = state.projectedState
    fun matches(ability: StaticAbility, source: EntityId, controller: EntityId): Boolean = when (ability) {
        // Reject unrelated statics before evaluating their potentially expensive conditions.
        is ConditionalStaticAbility -> matches(ability.ability, source, controller) &&
            predicates.conditions.evaluate(state, ability.condition,
                EffectContext(sourceId = source, controllerId = controller))
        is CompositeStaticAbility -> ability.abilities.any { matches(it, source, controller) }
        is SkipStepOrPhase -> ability.part == part && player in TargetResolutionUtils.resolvePlayerTargets(
            EffectTarget.PlayerRef(ability.player), state, EffectContext(sourceId = source, controllerId = controller))
        else -> false
    }
    val battlefield = state.getBattlefield()
    for (source in battlefield) {
        val entity = state.getEntity(source) ?: continue
        if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(source)) continue
        val controller = projected.getController(source) ?: continue
        val definition = entity.get<CardComponent>()?.let { registry.getCard(it.cardDefinitionId) } ?: continue
        val text = TextChanges.of(state, source)
        if (RoomFaceStatics.activeStaticAbilities(entity, definition).any {
                matches(if (text == null) it else it.applyTextReplacement(text), source, controller)
            }) return true
    }
    for (grant in state.grantedStaticAbilities) {
        val source = grant.entityId
        val isPlayer = source in state.turnOrder
        if (!isPlayer && (source !in battlefield || projected.hasLostAllAbilities(source))) continue
        if (!GrantDurationGate.holds(state, source, grant.sourceId, grant.duration)) continue
        val controller = if (isPlayer) source else projected.getController(source) ?: continue
        if (matches(grant.ability, source, controller)) return true
    }
    return false
}
