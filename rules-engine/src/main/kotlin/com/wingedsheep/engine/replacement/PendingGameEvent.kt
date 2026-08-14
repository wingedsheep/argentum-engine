package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.references.Player
import kotlinx.serialization.Serializable

/**
 * Describes a game event that *would* happen, before it occurs.
 *
 * These are constructed by effect executors before performing their action,
 * then passed to [ReplacementEffectProcessor] which checks all active
 * replacement effects against this event. The processor returns an outcome
 * that either modifies the event, replaces it with a different effect, or
 * consumes it entirely.
 *
 * This is deliberately distinct from [com.wingedsheep.engine.core.GameEvent]
 * (which records what *did* happen) — pending events describe hypothetical
 * future events that may never occur if replacement effects consume them.
 *
 * Each domain (draw, damage, life, token creation, zone change, etc.)
 * defines its own subtype and implements the polymorphic methods that
 * the domain-agnostic [ReplacementEffectProcessor] calls:
 * - [matches] — check if an [EventPattern] describes this event
 * - [applyReplacement] — apply a [ReplacementEffect] to produce an outcome
 * - [createOptionalPrompt] — build a yes/no prompt + continuation for
 *   optional replacement effects (most domains return null = mandatory-only)
 */
@Serializable
sealed interface PendingGameEvent {

    /**
     * The player most affected by this event — used to determine who chooses
     * between multiple competing replacement effects (CR 616.1).
     */
    val affectedPlayerId: EntityId

    /**
     * Check whether the given [pattern] describes this event.
     *
     * @param pattern The [EventPattern] from a replacement effect's `appliesTo`
     * @param sourceControllerId The controller of the permanent granting the replacement
     * @param state Current game state (for condition evaluation)
     * @param context Optional execution context (for condition evaluation)
     * @return true if this event matches the pattern
     */
    fun matches(
        pattern: EventPattern,
        sourceControllerId: EntityId,
        state: GameState,
        context: EffectContext?
    ): Boolean

    /**
     * Apply a [ReplacementEffect] to this event and produce a [ReplacementOutcome].
     *
     * @param effect The replacement effect to apply
     * @param state Current game state
     * @return The outcome (Modified, Replaced, or Consumed)
     */
    fun applyReplacement(effect: ReplacementEffect, state: GameState): ReplacementOutcome

    /**
     * Build a yes/no prompt and continuation for an optional replacement effect.
     *
     * Most event domains return null (no optional replacement support), causing the
     * processor to treat the effect as mandatory via [applyReplacement].
     *
     * @param decisionId Unique ID for the decision
     * @param gathered The matched replacement effect
     * @param state Current game state
     * @param context Execution context
     * @return An [OptionalPromptResult] with the decision and continuation, or null
     */
    fun createOptionalPrompt(
        decisionId: String,
        gathered: GatheredReplacement,
        state: GameState,
        context: EffectContext?
    ): OptionalPromptResult? = null

    /**
     * Return a continuation frame for any remaining work after a replacement
     * effect has been applied to this event, or null if none is needed.
     *
     * For [DrawPending] with remaining draws, this returns a
     * [DrawReplacementRemainingDrawsContinuation] so the draw loop can
     * continue after an optional or competing replacement resolves
     * (CR 614.11a — complete the replacement, then resume the sequence).
     * Most event domains return null (no remainder concept).
     */
    fun remainderContinuation(state: GameState): ContinuationFrame? = null

    /**
     * Return a continuation frame that **performs this event** once every
     * replacement has been applied to it, or null if the caller performs it
     * itself.
     *
     * Only reached on the paused path: when applying a replacement needed a
     * player decision, the call site that would have performed the event has
     * already returned, so the (modified) event has to be carried forward on
     * the continuation stack instead. A [ReplacementOutcome.Modified] leaves
     * the event still to happen — unlike `Replaced`/`Consumed`, where the
     * replacement *is* what happens — so without this the modified event is
     * silently dropped.
     *
     * Called on the **modified** event, so implementations read their own
     * post-replacement fields.
     */
    fun performContinuation(state: GameState): ContinuationFrame? = null

    /**
     * Draw event: a player is about to draw cards from their library.
     */
    @Serializable
    data class DrawPending(
        val playerId: EntityId,
        val count: Int,
        val remainingDraws: Int = 0,
        val isDrawStep: Boolean = false,
        val drawnCardsSoFar: List<EntityId> = emptyList()
    ) : PendingGameEvent {
        override val affectedPlayerId: EntityId get() = playerId

        /** Total draws remaining including this one (derived from remainingDraws + 1). */
        val drawsLeft: Int get() = remainingDraws + 1

        override fun matches(
            pattern: EventPattern,
            sourceControllerId: EntityId,
            state: GameState,
            context: EffectContext?
        ): Boolean {
            val drawEvent = pattern as? EventPattern.DrawEvent ?: return false
            if (drawEvent.exceptFirstInDrawStep && drawnCardsSoFar.isEmpty()) return false
            return matchesPlayerFilter(drawEvent.player, playerId, sourceControllerId, state)
        }

        /**
         * [ModifyDrawAmount] is deliberately absent: it only ever applies to the
         * announcement (CR 121.2a), and its `appliesTo` is typed as
         * [EventPattern.DrawCardsEvent] so it can never match this per-card event.
         * Adjusting a draw *count* here would not terminate — the draw loop would
         * re-check an unchanged game state and re-match the same effect forever.
         */
        override fun applyReplacement(effect: ReplacementEffect, state: GameState): ReplacementOutcome {
            return when (effect) {
                is PreventDraw -> ReplacementOutcome.Consumed
                is ReplaceDrawWithEffect -> ReplacementOutcome.Replaced(effect.replacementEffect)
                else -> error("Unsupported replacement effect type '${effect::class.simpleName}' for ${this::class.simpleName}")
            }
        }

        override fun remainderContinuation(state: GameState): ContinuationFrame? {
            if (remainingDraws > 0) {
                return DrawReplacementRemainingDrawsContinuation(
                    drawingPlayerId = playerId,
                    remainingDraws = remainingDraws,
                    isDrawStep = isDrawStep,
                    // Part of an instruction that was announced before the per-card
                    // loop started — re-announcing would apply ModifyDrawAmount twice.
                    announcementApplied = true
                )
            }
            return null
        }

        override fun createOptionalPrompt(
            decisionId: String,
            gathered: GatheredReplacement,
            state: GameState,
            context: EffectContext?
        ): OptionalPromptResult? {
            val replaceEffect = gathered.effect as? ReplaceDrawWithEffect ?: return null
            val sourceEntityId = gathered.sourceEntityId(state)
            val sourceEntity = sourceEntityId?.let { state.getEntity(it) }
            val card = sourceEntity?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            val cardName = card?.name ?: "Unknown"
            val linkedExile = sourceEntity
                ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
            val pileCount = linkedExile?.exiledIds?.size

            val prompt = buildString {
                // The effect's own text, not gathered.description — the latter is already
                // prefixed with the card name, which this line supplies.
                append("Use $cardName? ${replaceEffect.description}")
                if (pileCount != null) {
                    append(" ($pileCount cards remaining)")
                }
            }

            val decision = YesNoDecision(
                id = decisionId,
                playerId = affectedPlayerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = sourceEntityId,
                    sourceName = cardName,
                    phase = DecisionPhase.RESOLUTION
                )
            )

            val continuation = StaticDrawReplacementContinuation(
                decisionId = decisionId,
                drawingPlayerId = playerId,
                sourceId = sourceEntityId ?: EntityId(""),
                sourceName = cardName,
                replacementEffect = replaceEffect.replacementEffect,
                drawCount = drawsLeft,
                isDrawStep = isDrawStep,
                drawnCardsSoFar = drawnCardsSoFar,
                declinedIdentity = gathered.identity
            )

            return OptionalPromptResult(
                decision = decision,
                continuation = continuation
            )
        }
    }

    /**
     * Draw announcement event: a draw instruction says a player will draw N cards.
     *
     * Created **once** per draw instruction (spell, ability, or draw-step),
     * **before** the per-card [DrawLoop] fires. This allows `ModifyDrawAmount`
     * replacement effects using [EventPattern.DrawCardsEvent] (e.g. "if you
     * would draw two or more cards") to adjust the total before any individual
     * card is drawn (CR 121.2a).
     *
     * This event **only** matches [EventPattern.DrawCardsEvent].
     */
    @Serializable
    data class DrawAmountPending(
        val playerId: EntityId,
        val totalCount: Int,
        val isDrawStep: Boolean = false
    ) : PendingGameEvent {
        override val affectedPlayerId: EntityId get() = playerId

        override fun matches(
            pattern: EventPattern,
            sourceControllerId: EntityId,
            state: GameState,
            context: EffectContext?
        ): Boolean {
            return pattern is EventPattern.DrawCardsEvent &&
                totalCount >= pattern.amount &&
                matchesPlayerFilter(pattern.player, playerId, sourceControllerId, state)
        }

        override fun applyReplacement(effect: ReplacementEffect, state: GameState): ReplacementOutcome {
            return when (effect) {
                is ModifyDrawAmount -> ReplacementOutcome.Modified(
                    copy(
                        totalCount = (totalCount * effect.multiplier + effect.modifier).coerceAtLeast(0)
                    )
                )
                is PreventDraw -> ReplacementOutcome.Consumed
                is ReplaceDrawWithEffect -> ReplacementOutcome.Replaced(effect.replacementEffect)
                else -> error("Unsupported replacement effect type '${effect::class.simpleName}' for ${this::class.simpleName}")
            }
        }

        /**
         * The announcement itself performs no draws — the per-card loop does. When a
         * competing-replacement choice paused the announcement, the executor that would
         * have run that loop has already returned, so the modified instruction is carried
         * forward as a draw of [totalCount] with the announcement marked as done.
         */
        override fun performContinuation(state: GameState): ContinuationFrame? {
            if (totalCount <= 0) return null
            return DrawReplacementRemainingDrawsContinuation(
                drawingPlayerId = playerId,
                remainingDraws = totalCount,
                isDrawStep = isDrawStep,
                announcementApplied = true
            )
        }
    }
}

/**
 * Result of [PendingGameEvent.createOptionalPrompt].
 *
 * @property decision The yes/no decision to present to the player
 * @property continuation The continuation frame to resume after the player answers
 */
data class OptionalPromptResult(
    val decision: PendingDecision,
    val continuation: ContinuationFrame
)

private fun matchesPlayerFilter(
    player: Player,
    affectedPlayerId: EntityId,
    sourceControllerId: EntityId,
    state: GameState
): Boolean {
    return when (player) {
        Player.Each, Player.Any -> true
        Player.You -> affectedPlayerId == sourceControllerId
        Player.EachOpponent, Player.AnOpponent -> affectedPlayerId in state.getOpponents(sourceControllerId)
        else -> error("Unsupported player filter '$player' in matchesPlayerFilter")
    }
}