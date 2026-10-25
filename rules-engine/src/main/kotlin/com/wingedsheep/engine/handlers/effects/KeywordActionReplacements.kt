package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordActionInsteadContinuation
import com.wingedsheep.engine.core.KeywordActionInsteadOffer
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyKeywordAction
import com.wingedsheep.sdk.scripting.RepeatKeywordAction
import com.wingedsheep.sdk.scripting.ReplaceKeywordAction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The keyword actions (CR 701) that a [ModifyKeywordAction] replacement can prefix. Each maps to
 * the [EventPattern] its `appliesTo` must use.
 */
@Serializable
enum class ReplaceableKeywordAction(
    /** The verb as a player-facing button label ("Learn"). */
    val verb: String,
    /** The verb in "instead of …ing" prompts. */
    val gerund: String
) {
    /** CR 701.44 — `ExploreEffect` / Twists and Turns. */
    EXPLORE("Explore", "exploring"),

    /** CR 701.50 — `ConniveEffect` / Leader, Super-Genius. */
    CONNIVE("Connive", "conniving"),

    /** CR 701.34 — untargeted `ProliferateEffect` / Tekuthal, Inquiry Dominus. Player-performed. */
    PROLIFERATE("Proliferate", "proliferating"),

    /** CR 701.48 — `LearnEffect` / Retriever Phoenix ([ReplaceKeywordAction]). Player-performed. */
    LEARN("Learn", "learning"),
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
     * [lastKnown] is the subject as it last existed when it performs the action off the
     * battlefield (CR 701.50b — a departed convoker conniving); the filter is matched against it.
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
        predicateEvaluator: PredicateEvaluator,
        lastKnown: EntitySnapshot? = null
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
                    val predicateContext = PredicateContext(controllerId = sourceControllerId, sourceId = permanentId)
                    val matches = if (lastKnown != null) {
                        predicateEvaluator.matchesSnapshot(state, lastKnown, filter.filter, predicateContext)
                    } else predicateEvaluator.matches(
                        state,
                        state.projectedState,
                        subjectId,
                        filter.filter,
                        predicateContext
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

    /**
     * Every [ReplaceKeywordAction] that would replace [playerId] performing [action], as offers in
     * the order they are put to the player: mandatory ones first, then optional ones.
     *
     * Two sweeps, split by the replacement's `activeZones` (CR 113.6), like
     * [EntersWithReplacements.applyGlobal]: battlefield permanents (their projected controller is
     * "you"), and every graveyard card whose definition prints a `{GRAVEYARD}` replacement — a
     * graveyard card carries no [ReplacementEffectSourceComponent], so its effects are read from
     * [cardRegistry], and its owner is "you" (CR 108.3).
     *
     * The source has to be in that zone at the moment the action would happen (the Retriever
     * Phoenix ruling), which a sweep at action time is.
     */
    fun collectInstead(
        state: GameState,
        playerId: EntityId,
        action: ReplaceableKeywordAction,
        cardRegistry: CardRegistry
    ): List<KeywordActionInsteadOffer> {
        val offers = mutableListOf<KeywordActionInsteadOffer>()
        fun consider(sourceId: EntityId, sourceControllerId: EntityId, effects: List<*>, zone: Zone) {
            for (replacement in effects) {
                if (replacement !is ReplaceKeywordAction || zone !in replacement.activeZones) continue
                if (!performedBy(state, replacement.appliesTo, action, playerId, sourceControllerId)) continue
                offers.add(
                    KeywordActionInsteadOffer(
                        sourceId = sourceId,
                        controllerId = sourceControllerId,
                        sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "",
                        replacementEffect = replacement.replacementEffect,
                        optional = replacement.optional
                    )
                )
            }
        }
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val controllerId = state.projectedState.getController(permanentId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue
            consider(permanentId, controllerId, replacementComponent.replacementEffects, Zone.BATTLEFIELD)
        }
        for (ownerId in state.turnOrder) {
            for (cardId in state.getGraveyard(ownerId)) {
                val definitionId = state.getEntity(cardId)?.get<CardComponent>()?.cardDefinitionId ?: continue
                val definition = cardRegistry.getCard(definitionId) ?: continue
                consider(cardId, ownerId, definition.script.replacementEffects, Zone.GRAVEYARD)
            }
        }
        return offers.sortedBy { it.optional }
    }

    /**
     * Put [offers] to the player one at a time, in sweep order with mandatory ones first. Once one
     * has applied the action is gone and the rest have nothing left to replace. This is not a
     * faithful CR 616.1 choice — the affected player doesn't order a mandatory against an optional
     * one — but no printed card puts two such replacements on the same action today.
     * A mandatory offer applies at once; an optional one asks its source's controller yes or no,
     * through a [KeywordActionInsteadContinuation] whose "no" comes back here with the rest. With no
     * offers left, [reissue] — the keyword action again, with its replacements marked applied —
     * runs in [actionContext].
     *
     * The replacement effect runs rooted at its own source, so `EffectTarget.Self` is the card that
     * printed it, not the spell or ability that said to perform the action.
     */
    fun offerInstead(
        state: GameState,
        action: ReplaceableKeywordAction,
        offers: List<KeywordActionInsteadOffer>,
        reissue: Effect,
        actionContext: EffectContext,
        execute: (GameState, Effect, EffectContext) -> EffectResult
    ): EffectResult {
        val offer = offers.firstOrNull() ?: return execute(state, reissue, actionContext)
        if (!offer.optional) return execute(state, offer.replacementEffect, replacementContext(state, offer))
        val question = { decisionId: String ->
            YesNoDecision(
                id = decisionId,
                playerId = offer.controllerId,
                prompt = "Instead of ${action.gerund}, " +
                    "${SourceReferenceText.clause(offer.replacementEffect, SourceReferenceText.referenceFor(state, offer.sourceId))}?",
                context = DecisionContext(
                    sourceId = offer.sourceId,
                    sourceName = offer.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = "Use ${offer.sourceName}",
                noText = action.verb
            )
        }
        return EffectResult.from(
            state.suspendForDecision(
                question,
                KeywordActionInsteadContinuation(
                    action = action,
                    offers = offers,
                    reissue = reissue,
                    actionContext = actionContext
                )
            )
        )
    }

    /** The context [offer]'s replacement effect runs in: rooted at the replacement's source. */
    fun replacementContext(state: GameState, offer: KeywordActionInsteadOffer): EffectContext {
        val sourceRef = state.objectRef(offer.sourceId)
        return EffectContext(
            controllerId = offer.controllerId,
            sourceId = offer.sourceId,
            objectReferences = ObjectReferenceEnvironment(captured = true, origin = sourceRef, source = sourceRef),
            targets = emptyList()
        )
    }

    private fun performedBy(
        state: GameState,
        pattern: EventPattern,
        action: ReplaceableKeywordAction,
        playerId: EntityId,
        sourceControllerId: EntityId
    ): Boolean = when {
        action == ReplaceableKeywordAction.LEARN && pattern is EventPattern.LearnEvent ->
            matchesPlayer(state, pattern.player, playerId, sourceControllerId)

        else -> false
    }

    private fun matchesPlayer(
        state: GameState,
        player: Player,
        playerId: EntityId,
        sourceControllerId: EntityId
    ): Boolean = when (player) {
        Player.You -> playerId == sourceControllerId
        Player.Each, Player.Any -> true
        Player.EachOpponent, Player.AnOpponent -> state.isOpponentOf(playerId, sourceControllerId)
        // Fail closed: no other player reference has a meaning for "would <act>".
        else -> false
    }

    private fun repeats(
        state: GameState,
        pattern: EventPattern,
        action: ReplaceableKeywordAction,
        playerId: EntityId,
        sourceControllerId: EntityId
    ): Boolean = when {
        action == ReplaceableKeywordAction.PROLIFERATE && pattern is EventPattern.ProliferatedEvent ->
            matchesPlayer(state, pattern.player, playerId, sourceControllerId)

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
