package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.SacrificeImmunity
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceSacrificeEffect.
 *
 * Handles "target player sacrifices a creature" (edict effects) where an opponent
 * is forced to sacrifice permanents of their choice. Supports multi-player targets
 * like Player.EachOpponent.
 *
 * Examples:
 * - Cabal Executioner: "Whenever ~ deals combat damage to a player, that player sacrifices a creature."
 * - Chainer's Edict: "Target player sacrifices a creature."
 * - The Eldest Reborn: "Each opponent sacrifices a creature or planeswalker."
 */
class ForceSacrificeExecutor(
    private val zones: ZoneTransitionService,
    private val dynamicAmountEvaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ForceSacrificeEffect> {

    override val effectType: KClass<ForceSacrificeEffect> = ForceSacrificeEffect::class

    override fun execute(
        state: GameState,
        effect: ForceSacrificeEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid player for force sacrifice")
        }

        // A dynamic count ("sacrifices half the creatures they control, rounded up") is evaluated
        // once at resolution; otherwise fall back to the fixed edict count. The amount is resolved
        // against the same context so a per-target reference (e.g. Player.ContextPlayer / TargetOpponent)
        // counts the chosen player's permanents.
        val count = effect.dynamicCount
            ?.let { dynamicAmountEvaluator.evaluate(state, it, context) }
            ?: effect.count

        // Sigarda, Host of Herons: an edict is the canonical "a spell or ability your opponents
        // control causes you to sacrifice", so a protected player is dropped from the list before
        // anyone is prompted — they are never offered the choice (CR 101.2).
        val effectControllerId = context.effectControllerId ?: context.controllerId
        val eligiblePlayers = playerIds.filterNot {
            SacrificeImmunity.appliesTo(state, it, effectControllerId, predicateEvaluator = zones.predicateEvaluator)
        }

        return processPlayers(state, inApnapOrder(state, eligiblePlayers), effect.filter, count, context.sourceId)
    }

    /**
     * Collect each player's sacrifice choice in order, then sacrifice everything at once.
     *
     * "Each player sacrifices a creature" is one simultaneous event: each affected player chooses
     * in APNAP order, and only then are all the chosen permanents sacrificed together (CR 101.4),
     * so a "whenever a creature dies" trigger sees every one of them, and no player's sacrifice
     * happens before a later player has chosen. A player with no more candidates than [count]
     * has nothing to choose; their permanents join the batch without a prompt. When a player must
     * choose, the executor pauses with the remaining players and the choices made so far in the
     * continuation, and [SacrificeContinuation]'s resumer re-enters here.
     */
    fun processPlayers(
        state: GameState,
        playerIds: List<EntityId>,
        filter: GameObjectFilter,
        count: Int,
        sourceId: EntityId?,
        chosen: List<SacrificeChoice> = emptyList()
    ): EffectResult {
        val sourceName = if (sourceId != null) {
            state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        } else {
            "Unknown"
        }

        val choices = chosen.toMutableList()

        for ((index, playerId) in playerIds.withIndex()) {
            val validPermanents = findValidPermanents(state, playerId, filter, sourceId)

            if (validPermanents.isEmpty()) {
                continue
            }

            if (validPermanents.size <= count) {
                choices += SacrificeChoice(playerId, validPermanents)
                continue
            }

            // Player must choose — pause and store remaining players and earlier choices
            return presentSacrificeDecision(
                state, playerId, sourceId, sourceName,
                validPermanents, minSelections = count, maxSelections = count,
                remainingPlayers = playerIds.drop(index + 1), filter = filter, count = count,
                chosen = choices
            )
        }

        return sacrificeChoices(state, choices)
    }

    /** Rotate [playerIds] into APNAP order (CR 101.4); players outside the turn order keep their place at the end. */
    private fun inApnapOrder(state: GameState, playerIds: List<EntityId>): List<EntityId> {
        val order = state.apnapOrder
        return playerIds.sortedBy { id -> order.indexOf(id).let { if (it < 0) Int.MAX_VALUE else it } }
    }

    private fun findValidPermanents(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId?
    ): List<EntityId> {
        // Pass sourceId so source-relative filter predicates (e.g.
        // DealtCombatDamageToSourceControllerThisTurn — Witch-king of Angmar) resolve against
        // the edict's source rather than the sacrificing player.
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = playerId, sourceId = sourceId),
            predicateEvaluator = zones.predicateEvaluator
        )
    }

    private fun presentSacrificeDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String,
        validPermanents: List<EntityId>,
        minSelections: Int,
        maxSelections: Int,
        remainingPlayers: List<EntityId>,
        filter: GameObjectFilter,
        count: Int,
        chosen: List<SacrificeChoice>
    ): EffectResult {
        // Describe the actual filter ("a land", "a creature", "an artifact"...) rather than
        // hardcoding "creature" — Serendib Djinn / Goblin Firebug sacrifice lands, edicts
        // sacrifice creatures, etc.
        val noun = filter.description
        val prompt = buildString {
            append("Choose ")
            if (minSelections == 1) {
                append(if (noun.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an " else "a ")
                append(noun)
            } else {
                append("$minSelections ${noun}s")
            }
            append(" to sacrifice")
        }

        val continuation = SacrificeContinuation(
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            remainingPlayers = remainingPlayers,
            filter = filter,
            count = count,
            chosen = chosen
        )

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = minSelections,
            maxSelections = maxSelections,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true,
            answer = continuation
        )

        return EffectResult.propagatePause(decisionResult.state, decisionResult.events)
    }

    internal fun sacrificePermanents(
        state: GameState,
        playerId: EntityId,
        permanentIds: List<EntityId>
    ): EffectResult = sacrificeChoices(state, listOf(SacrificeChoice(playerId, permanentIds)))

    /** Sacrifice every player's chosen permanents as one simultaneous event. */
    internal fun sacrificeChoices(
        state: GameState,
        choices: List<SacrificeChoice>
    ): EffectResult {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val permanentIds = choices.flatMap { it.permanentIds }

        // Capture P/T/subtype/supertype/controller snapshots BEFORE the zone change so
        // a follow-up sibling effect can read the sacrificed permanent's characteristics
        // (Rise of the Witch-king's "if you sacrificed a creature this way…", Nasty End-style
        // "was legendary?" gates). Mirrors SacrificeExecutor's capture path.
        val snapshots = if (permanentIds.isNotEmpty()) {
            captureEntitySnapshots(permanentIds, newState)
        } else {
            emptyList()
        }

        for (choice in choices) {
            if (choice.permanentIds.isEmpty()) continue
            val permanentNames = choice.permanentIds.map { id ->
                newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(PermanentsSacrificedEvent(choice.playerId, choice.permanentIds, permanentNames))
            newState = ZoneTransitionService.trackPermanentSacrifice(newState, choice.permanentIds, choice.playerId)
        }

        // One simultaneous event: a batch move freezes every look-back grant before the first goes.
        val transitionResult = zones.moveToZoneBatch(newState, permanentIds, Zone.GRAVEYARD)
        newState = transitionResult.state
        events.addAll(transitionResult.events)

        return EffectResult.success(newState, events)
            .copy(updatedSacrificedPermanents = snapshots)
    }
}
