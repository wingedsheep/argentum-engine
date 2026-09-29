package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyKeywordAction
import com.wingedsheep.sdk.scripting.RepeatKeywordAction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The keyword actions (CR 701) that a [ModifyKeywordAction] replacement can prefix. Each maps to
 * the [EventPattern] its `appliesTo` must use.
 */
enum class ReplaceableKeywordAction {
    /** CR 701.44 — `ExploreEffect` / Twists and Turns. */
    EXPLORE,

    /** CR 701.50 — `ConniveEffect` / Leader, Super-Genius. */
    CONNIVE,

    /** CR 701.34 — untargeted `ProliferateEffect` / Tekuthal, Inquiry Dominus. Player-performed. */
    PROLIFERATE,
}

/**
 * Shared lookup behind the "if a permanent you control would &lt;keyword action&gt;, instead …"
 * replacements (CR 614). Neither explore nor connive is dispatched through the generic replaceable
 * event pipeline, so each action's executor consults the battlefield here at action time — see
 * [ModifyKeywordAction].
 */
object KeywordActionReplacements {

    /**
     * The ordered prefix effects of every [ModifyKeywordAction] on the battlefield that applies to
     * [subjectId] performing [action]. The replacement's `appliesTo` filter is matched against the
     * subject using the *replacement source's* controller as "you", so "a creature you control
     * would connive" only fires for that player's creatures.
     *
     * Battlefield order is used for the multi-source case — a faithful APNAP ordering (CR 616)
     * would let the acting player order simultaneous applicable replacements, but no printed card
     * stacks two modifiers on the same keyword action today. Two copies of the same modifier do
     * each apply, and in battlefield order.
     */
    fun collectPrefixes(
        state: GameState,
        subjectId: EntityId,
        action: ReplaceableKeywordAction,
        predicateEvaluator: PredicateEvaluator
    ): List<Effect> {
        val prefixes = mutableListOf<Effect>()
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue
            for (replacement in replacementComponent.replacementEffects) {
                if (replacement !is ModifyKeywordAction) continue
                val filter = subjectFilterFor(replacement.appliesTo, action) ?: continue
                // A null filter inside the pattern means "any permanent performing this action".
                if (filter is SubjectFilter.Restricted) {
                    val matches = predicateEvaluator.matches(
                        state,
                        state.projectedState,
                        subjectId,
                        filter.filter,
                        PredicateContext(controllerId = sourceControllerId, sourceId = permanentId)
                    )
                    if (!matches) continue
                }
                prefixes.add(replacement.prefixEffect)
            }
        }
        return prefixes
    }

    /**
     * How many times [playerId] performs [action] once every [RepeatKeywordAction] on the
     * battlefield has applied — `1` when none does. Applicable instances multiply (each one applies
     * to every action the previous ones produced), so two "proliferate twice" sources give four.
     * The pattern's `player` is matched with the *replacement source's* controller as "you".
     */
    fun repetitions(
        state: GameState,
        playerId: EntityId,
        action: ReplaceableKeywordAction
    ): Int {
        var times = 1
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = state.projectedState.getController(permanentId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue
            for (replacement in replacementComponent.replacementEffects) {
                if (replacement !is RepeatKeywordAction) continue
                if (!repeats(state, replacement.appliesTo, action, playerId, sourceControllerId)) continue
                times *= replacement.times
            }
        }
        return times
    }

    private fun repeats(
        state: GameState,
        pattern: EventPattern,
        action: ReplaceableKeywordAction,
        playerId: EntityId,
        sourceControllerId: EntityId
    ): Boolean = when {
        action == ReplaceableKeywordAction.PROLIFERATE && pattern is EventPattern.ProliferatedEvent ->
            when (pattern.player) {
                Player.You -> playerId == sourceControllerId
                Player.Each -> true
                Player.EachOpponent -> state.isOpponentOf(playerId, sourceControllerId)
                // Fail closed: no other player reference has a meaning for "would proliferate".
                else -> false
            }

        else -> false
    }

    /**
     * The subject filter this pattern imposes, or `null` when the pattern is not [action] at all
     * (so the replacement doesn't apply). [SubjectFilter.Unrestricted] and a `null` return are deliberately
     * different answers: the first matches every subject, the second matches none.
     */
    private fun subjectFilterFor(
        pattern: EventPattern,
        action: ReplaceableKeywordAction
    ): SubjectFilter? = when {
        action == ReplaceableKeywordAction.EXPLORE && pattern is EventPattern.ExploredEvent ->
            SubjectFilter.of(pattern.filter)

        action == ReplaceableKeywordAction.CONNIVE && pattern is EventPattern.ConnivedEvent ->
            SubjectFilter.of(pattern.filter)

        else -> null
    }

    private sealed interface SubjectFilter {
        data object Unrestricted : SubjectFilter
        data class Restricted(val filter: GameObjectFilter) : SubjectFilter

        companion object {
            fun of(filter: GameObjectFilter?): SubjectFilter =
                if (filter == null) Unrestricted else Restricted(filter)
        }
    }
}
