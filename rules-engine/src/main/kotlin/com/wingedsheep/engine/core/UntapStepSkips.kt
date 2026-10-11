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
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Standing skip restrictions are read before phasing or any other untap-step action. */
internal fun skipsUntapStep(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    player: EntityId,
): Boolean = hasStandingSkip(state, registry, predicates, player, TurnPart.UNTAP_STEP)

/**
 * Whether a standing [SkipStepOrPhase] for [part] applies to [player], whose `player` scope is
 * resolved from the source's controller. Sources are read by [standingStatics].
 */
internal fun hasStandingSkip(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    player: EntityId,
    part: TurnPart,
): Boolean = standingStatics(state, registry, predicates, SkipStepOrPhase::class) { ability, source, controller ->
    ability.part == part && player in TargetResolutionUtils.resolvePlayerTargets(
        EffectTarget.PlayerRef(ability.player), state, EffectContext(sourceId = source, controllerId = controller))
}.isNotEmpty()

/**
 * Every functioning standing static of [type] that [applies] accepts: one on any face-up
 * battlefield permanent that hasn't lost its abilities (Room faces only while unlocked, text
 * changes applied), or any live granted static (a player-held grant is controlled by that player).
 * [ConditionalStaticAbility] and [CompositeStaticAbility] wrappers are unwrapped; a condition is
 * evaluated against its own source, and only once the wrapped static has already been accepted.
 *
 * The shared reader for player-scoped restrictions that a turn-based action or a game event
 * consults directly rather than through projection — skipped steps, per-turn draw caps.
 */
internal fun <T : StaticAbility> standingStatics(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    type: KClass<T>,
    applies: (ability: T, source: EntityId, controller: EntityId) -> Boolean,
): List<T> {
    val projected = state.projectedState
    val found = mutableListOf<T>()
    fun collect(ability: StaticAbility, source: EntityId, controller: EntityId, into: MutableList<T>) {
        when {
            // Reject unrelated statics before evaluating their potentially expensive conditions.
            ability is ConditionalStaticAbility -> {
                val inner = mutableListOf<T>()
                collect(ability.ability, source, controller, inner)
                if (inner.isNotEmpty() && predicates.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = source, controllerId = controller))) into.addAll(inner)
            }
            ability is CompositeStaticAbility -> ability.abilities.forEach { collect(it, source, controller, into) }
            type.isInstance(ability) -> {
                val typed = type.cast(ability)
                if (applies(typed, source, controller)) into.add(typed)
            }
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
            collect(if (text == null) ability else ability.applyTextReplacement(text), source, controller, found)
        }
    }
    for (grant in state.grantedStaticAbilities) {
        val source = grant.entityId
        val isPlayer = source in state.turnOrder
        if (!isPlayer && (source !in battlefield || projected.hasLostAllAbilities(source))) continue
        if (!GrantDurationGate.holds(state, source, grant.sourceId, grant.duration)) continue
        val controller = if (isPlayer) source else projected.getController(source) ?: continue
        collect(grant.ability, source, controller, found)
    }
    return found
}
