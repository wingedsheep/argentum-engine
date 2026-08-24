package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseOpponentDeciderContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID

/**
 * The single place a [Chooser] becomes a concrete deciding player.
 *
 * Every executor whose effect carries a [Chooser] routes through [resolve] instead of
 * re-deriving the mapping, so the variants stay in lockstep (they used to be three
 * hand-copied `when` blocks that had already drifted — one silently returned `null` for
 * `ControllerOfSelection` / `ControllerOfTarget`).
 *
 * ## "An opponent" in multiplayer
 *
 * [Chooser.Opponent] means *one* opponent decides, and the controller of the spell or
 * ability picks which one. The Comprehensive Rules spell that out for choices made while a
 * spell is cast (CR 601.7a) or an ability activated (CR 602.3a); resolution-time choices
 * follow the same principle, and cards state it in their rulings — Curator of Destinies:
 * "You decide which opponent chooses the pile while resolving [its] last ability."
 *
 * So with several opponents [resolve] does not pick one: it reports
 * [Outcome.NeedsOpponentPick], and the executor pauses via [pauseForOpponentPick] to ask the
 * controller. The pick is stamped onto [EffectContext.opponentDeciderId] and the *same effect*
 * is re-executed, which then resolves straight through to that opponent. With a single
 * opponent (every two-player game) the choice is forced and nothing is asked, so nothing
 * about two-player play changes.
 *
 * The stamp is resolution-scoped, not durable: two separate "an opponent chooses" steps in
 * one resolution each get their own prompt, because the composite's remaining-steps
 * continuation carries the pre-pick context.
 */
object ChooserResolution {

    sealed interface Outcome {
        /** The deciding player is known. */
        data class Resolved(val playerId: EntityId) : Outcome

        /**
         * `Chooser.Opponent` with more than one opponent: the controller must first pick
         * which of [opponents] decides. Hand this to [pauseForOpponentPick].
         */
        data class NeedsOpponentPick(val opponents: List<EntityId>) : Outcome

        /** No deciding player exists (no opponent, missing target, source gone, …). */
        data class Unresolvable(val reason: String) : Outcome
    }

    /**
     * Map [chooser] onto a deciding player.
     *
     * @param selectionCards the cards being selected from, for
     *   [Chooser.ControllerOfSelection] — the controller of the first one decides. Pass the
     *   collection the effect reads; empty is fine for every other chooser.
     */
    fun resolve(
        state: GameState,
        chooser: Chooser,
        context: EffectContext,
        selectionCards: List<EntityId> = emptyList()
    ): Outcome = when (chooser) {
        Chooser.Controller -> Outcome.Resolved(context.controllerId)

        Chooser.Opponent -> {
            val opponents = state.getOpponents(context.controllerId)
            val alreadyPicked = context.opponentDeciderId
            when {
                opponents.isEmpty() -> Outcome.Unresolvable("No opponent to make the choice")
                alreadyPicked != null && alreadyPicked in opponents -> Outcome.Resolved(alreadyPicked)
                opponents.size == 1 -> Outcome.Resolved(opponents.single())
                else -> Outcome.NeedsOpponentPick(opponents)
            }
        }

        Chooser.TargetPlayer -> {
            val playerId = context.targets.firstOrNull()?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            }
            playerId?.let { Outcome.Resolved(it) }
                ?: Outcome.Unresolvable("No target player to make the choice")
        }

        Chooser.TriggeringPlayer -> context.triggeringEntityId?.let { Outcome.Resolved(it) }
            ?: Outcome.Unresolvable("No triggering player to make the choice")

        Chooser.SourceController -> {
            // [EffectContext.effectControllerId] is the authoritative answer whenever a
            // per-player iteration is running — it is captured for exactly this purpose and is
            // the *only* one that works for a resolving spell, whose stack entity carries a
            // caster but no ControllerComponent. Outside an iteration it is null and the
            // permanent lookup below (an activated/triggered ability's source) answers instead.
            val iterationSafe = context.effectControllerId
            val sourceId = context.sourceId
            val controller = iterationSafe
                ?: sourceId?.let { state.getEntity(it)?.get<ControllerComponent>()?.playerId }
            controller?.let { Outcome.Resolved(it) }
                ?: Outcome.Unresolvable("Source has no controller to make the choice")
        }

        Chooser.ControllerOfSelection -> {
            val deriveFrom = selectionCards.firstOrNull()
            val controller = deriveFrom?.let {
                state.projectedState.getController(it)
                    ?: state.getEntity(it)?.get<ControllerComponent>()?.playerId
            }
            controller?.let { Outcome.Resolved(it) }
                ?: Outcome.Unresolvable("No card to derive the selection's controller from")
        }

        // CR 802.2a — the player the source is attacking (or the controller/protector of the
        // planeswalker or battle it is attacking). Shares the resolution-time read with
        // `Player.DefendingPlayer`, removed-from-combat leg included, so a "defending player
        // discards" that follows a self-sacrifice still asks the right player.
        Chooser.DefendingPlayer ->
            TargetResolutionUtils.resolveDefendingPlayer(context, state)
                ?.let { Outcome.Resolved(it) }
                ?: Outcome.Unresolvable("No defending player to make the choice")

        Chooser.ControllerOfTarget -> {
            val targetId = context.targets.firstOrNull()?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            }
            // Fall back to the owner once the permanent has left the battlefield (e.g. it was
            // destroyed earlier in the same resolution) — CR 608.2h last-known information.
            val controller = targetId?.let {
                state.getEntity(it)?.get<ControllerComponent>()?.playerId
                    ?: state.getEntity(it)?.get<CardComponent>()?.ownerId
            }
            controller?.let { Outcome.Resolved(it) }
                ?: Outcome.Unresolvable("No target to derive its controller from")
        }
    }

    /**
     * Ask the controller which of [opponents] makes the pending choice, then re-run [effect].
     *
     * [prompt] should name what the chosen opponent is about to do ("Choose which opponent
     * chooses a pile"), since that is the only thing the deciding controller sees.
     */
    fun pauseForOpponentPick(
        state: GameState,
        opponents: List<EntityId>,
        effect: Effect,
        context: EffectContext,
        prompt: String
    ): EffectResult {
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = opponents.map { playerName(state, it) }
        )
        val continuation = ChooseOpponentDeciderContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            opponentIds = opponents,
            effect = effect,
            baseContext = context
        )
        return EffectResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = prompt
                )
            )
        )
    }

    private fun playerName(state: GameState, playerId: EntityId): String =
        state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player ${playerId.value}"
}
