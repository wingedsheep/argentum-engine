package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.replacement.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** The shared life primitive queues effect work where damage arithmetic cannot pause. */
object LifeGainReplacements {
    fun applies(state: GameState, playerId: EntityId, amount: Int, predicates: PredicateEvaluator): Boolean {
        if (amount <= 0) return false
        return ActiveReplacements.all(state).any { active ->
            val printed = active.effect as? ReplaceLifeGainWith ?: return@any false
            val text = if (active.granted) null else
                com.wingedsheep.engine.state.components.identity.TextChanges.of(state, active.sourceId)
            val replacement = if (text == null) printed else printed.applyTextReplacement(text) as ReplaceLifeGainWith
            if (!active.granted && (state.projectedState.hasLostAllAbilities(active.sourceId) || state.projectedState.isFaceDown(active.sourceId))) return@any false
            val context = EffectContext(sourceId = active.sourceId, controllerId = active.controllerId,
                triggeringPlayerId = playerId)
            PendingGameEvent.LifeGainPending(playerId, amount).matches(replacement.appliesTo,
                active.controllerId, state, context) &&
                replacement.restrictions.all { predicates.conditions.evaluate(state, it, context) }
        }
    }

    fun queue(state: GameState, playerId: EntityId, amount: Int): GameState =
        state.copy(pendingReplacementRiders = state.pendingReplacementRiders + PendingReplacementRider(
            effect = Effects.GainLife(amount, EffectTarget.SpecificEntity(playerId)),
            hostId = playerId, controllerId = playerId, subjectId = playerId,
            replacementChain = state.activeReplacementChain))

    fun resolve(state: GameState, playerId: EntityId, amount: Int, context: EffectContext,
        processor: ReplacementEffectProcessor,
        execute: (GameState, Effect, EffectContext) -> EffectResult): EffectResult {
        if (amount <= 0 || DamageUtils.isLifeGainPrevented(state, playerId)) return EffectResult.success(state)
        val previous = state.activeReplacementChain
        val boundary = RestoreReplacementChainContinuation(previous)
        val prepared = state.pushContinuation(boundary)
        val processed = processor.process(prepared, PendingGameEvent.LifeGainPending(playerId, amount), context)
        val result = when (processed) {
            is ProcessorResult.Paused -> return EffectResult.propagatePause(processed.state, processed.events)
            ProcessorResult.Pass -> {
                val (gained, event) = DamageUtils.gainLifePrimitive(prepared, playerId, amount)
                EffectResult.success(gained, listOfNotNull(event))
            }
            is ProcessorResult.Resolved -> when (val outcome = processed.outcome) {
                ReplacementOutcome.Consumed -> EffectResult.success(processed.state)
                is ReplacementOutcome.Modified -> {
                    val gain = outcome.modifiedEvent as PendingGameEvent.LifeGainPending
                    val (gained, event) = DamageUtils.gainLifePrimitive(processed.state, playerId, gain.amount)
                    EffectResult.success(gained, listOfNotNull(event))
                }
                is ReplacementOutcome.Replaced -> execute(processed.state, outcome.newEffect,
                    requireNotNull(processed.executionContext))
            }
        }
        if (result.outcome is Outcome.Paused) return result
        check(result.state.peekContinuation() is RestoreReplacementChainContinuation)
        return result.copy(state = result.state.popContinuation().second.copy(activeReplacementChain = previous))
    }
}
