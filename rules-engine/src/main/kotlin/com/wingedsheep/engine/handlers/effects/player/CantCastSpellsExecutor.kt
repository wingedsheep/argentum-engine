package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PlayerActionPermissionsChangedEvent
import com.wingedsheep.engine.state.components.player.SpellCastRestriction
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import kotlin.reflect.KClass

/**
 * Executor for CantCastSpellsEffect.
 * Adds CantCastSpellsComponent to the target player, preventing them from casting spells.
 *
 * The ban is read at every later cast, long after this resolution's context is gone, so a
 * dynamic mana-value bound in its filter is locked in now (CR 608.2h): Living Breakthrough's
 * "spells with the same mana value as that spell" fixes the triggering spell's mana value when
 * the trigger resolves.
 */
class CantCastSpellsExecutor(
    private val amounts: DynamicAmountEvaluator
) : EffectExecutor<CantCastSpellsEffect> {

    override val effectType: KClass<CantCastSpellsEffect> = CantCastSpellsEffect::class

    override fun execute(
        state: GameState,
        effect: CantCastSpellsEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { state.turnOrder.contains(it) }
        if (targetIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for can't cast spells effect")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            is Duration.EndOfYourNextTurn -> PlayerEffectRemoval.EndOfYourNextTurn
            is Duration.UntilYourNextTurn -> PlayerEffectRemoval.UntilYourNextTurn
            else -> PlayerEffectRemoval.EndOfTurn
        }
        val expiresAfterTurn =
            if (removeOn == PlayerEffectRemoval.EndOfYourNextTurn) state.turnNumber + 1 else null

        val spellFilter = lockIn(effect.spellFilter, state, context)
        val newState = targetIds.fold(state) { acc, targetId ->
            acc.updateEntity(targetId) { container ->
                val existing = container.get<CantCastSpellsComponent>()?.restrictions.orEmpty()
                container.with(CantCastSpellsComponent(existing + SpellCastRestriction(
                    spellFilter = spellFilter,
                    removeOn = removeOn,
                    sourceId = context.sourceId,
                    controllerId = context.controllerId,
                    expiresAfterTurn = expiresAfterTurn
                )))
            }
        }

        return EffectResult.success(newState, targetIds.map(::PlayerActionPermissionsChangedEvent))
    }

    private fun lockIn(filter: GameObjectFilter, state: GameState, context: EffectContext): GameObjectFilter {
        val predicates = filter.cardPredicates.map { lockIn(it, state, context) }
        val anyOf = filter.anyOf.map { lockIn(it, state, context) }
        return if (predicates == filter.cardPredicates && anyOf == filter.anyOf) filter
        else filter.copy(cardPredicates = predicates, anyOf = anyOf)
    }

    private fun lockIn(predicate: CardPredicate, state: GameState, context: EffectContext): CardPredicate =
        when (predicate) {
            is CardPredicate.ManaValueEqualsDynamic ->
                CardPredicate.ManaValueEquals(amounts.evaluate(state, predicate.amount, context))
            is CardPredicate.ManaValueAtMostDynamic ->
                CardPredicate.ManaValueAtMost(amounts.evaluate(state, predicate.amount, context))
            is CardPredicate.And -> CardPredicate.And(predicate.predicates.map { lockIn(it, state, context) })
            is CardPredicate.Or -> CardPredicate.Or(predicate.predicates.map { lockIn(it, state, context) })
            is CardPredicate.Not -> CardPredicate.Not(lockIn(predicate.predicate, state, context))
            else -> predicate
        }
}
