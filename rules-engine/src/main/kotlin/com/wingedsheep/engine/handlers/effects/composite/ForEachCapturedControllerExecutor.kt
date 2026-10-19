package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ForEachContinuation
import com.wingedsheep.engine.core.ForEachItem
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Executor for [ForEachCapturedControllerEffect].
 *
 * Cross-references three pipeline-stored lists, builds a per-controller tally, and
 * runs the sub-effects pipeline once per tallied controller (in turn order, starting
 * from the active player) with `context.controllerId` set to that player and
 * `storedNumbers[countVariable]` set to that player's count.
 *
 * Unlike [ForEachExecutor]'s player space, the outer pipeline's `storedCollections`
 * are preserved per iteration so sub-effects can reference the same collections that
 * drove the tally.
 *
 * Sub-effect pauses re-enter via a player-space [ForEachContinuation] over the
 * remaining controllers, resumed by [ForEachExecutor.processItems]; each remaining item
 * carries its controller's `countVariable` tally and keeps the outer collections, so a
 * resumed pass binds exactly what an unpaused one does.
 */
class ForEachCapturedControllerExecutor(
    /** The registry's plain entry point, for the effect program each controller's sub-effects run as. */
    plainExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ForEachCapturedControllerEffect> {

    private val interpreter = EffectProgramInterpreter(plainExecutor)

    override val effectType: KClass<ForEachCapturedControllerEffect> =
        ForEachCapturedControllerEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachCapturedControllerEffect,
        context: EffectContext
    ): EffectResult {
        val deadCards = context.pipeline.storedCollections[effect.collection]
            ?: return EffectResult.error(state, "No collection named '${effect.collection}' in storedCollections")
        if (deadCards.isEmpty()) return EffectResult.success(state)

        val original = context.pipeline.storedCollections[effect.originalCollection]
            ?: return EffectResult.error(state, "No collection named '${effect.originalCollection}' in storedCollections")
        val controllers = context.pipeline.storedCollections[effect.controllerSnapshot]
            ?: return EffectResult.error(state, "No collection named '${effect.controllerSnapshot}' in storedCollections")
        if (controllers.size != original.size) {
            return EffectResult.error(
                state,
                "Controller snapshot '${effect.controllerSnapshot}' (${controllers.size}) is not " +
                    "parallel to originalCollection '${effect.originalCollection}' (${original.size})"
            )
        }

        val originalIndex: Map<EntityId, Int> = original.withIndex().associate { (i, id) -> id to i }
        val tallies = linkedMapOf<EntityId, Int>()
        for (deadId in deadCards) {
            val idx = originalIndex[deadId] ?: continue
            tallies.merge(controllers[idx], 1, Int::plus)
        }
        if (tallies.isEmpty()) return EffectResult.success(state)

        // Sort controllers by turn order, starting from the active player, for deterministic
        // ordering of any state-mutating sub-effects (damage, life loss, etc.).
        val turnOrder = state.turnOrder
        val activeIdx = state.activePlayerId?.let { turnOrder.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        val ordered = (turnOrder.drop(activeIdx) + turnOrder.take(activeIdx))
            .filter { tallies.containsKey(it) }
            .map { it to tallies.getValue(it) }

        return processControllers(state, effect, ordered, context)
    }

    private fun processControllers(
        state: GameState,
        effect: ForEachCapturedControllerEffect,
        controllerTallies: List<Pair<EntityId, Int>>,
        outerContext: EffectContext
    ): EffectResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        // What every controller's sub-effects stored, later ones replacing earlier names.
        var stored = EffectResult(state)

        for ((index, entry) in controllerTallies.withIndex()) {
            val (playerId, count) = entry
            val remaining = controllerTallies.drop(index + 1)

            val perIterationContext = outerContext.copy(
                controllerId = playerId,
                // Keep the spell's controller reachable (Chooser.SourceController), as the player
                // iteration space does: the caster still makes the choices inside each pass.
                effectControllerId = outerContext.effectControllerId ?: outerContext.controllerId,
                pipeline = outerContext.pipeline.copy(
                    // Preserve outer storedCollections (unlike the player iteration space) so the
                    // sub-effects can still see the dead pile, original list, and snapshot.
                    storedNumbers = outerContext.pipeline.storedNumbers + (effect.countVariable to count)
                )
            )

            val stateForExecution = if (remaining.isNotEmpty()) {
                val continuation = ForEachContinuation(
                    remainingItems = remaining.map { (id, n) ->
                        ForEachItem.OfPlayer(id, boundNumbers = mapOf(effect.countVariable to n), keepCollections = true)
                    },
                    effect = ForEachEffect(
                        // Resume-only carrier: the items are already enumerated, so the
                        // Players selector is never consulted — only the per-player binding.
                        space = IterationSpace.Players(Player.Each),
                        body = effect.effects.singleOrNull() ?: CompositeEffect(effect.effects)
                    ),
                    effectContext = outerContext
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = executeSubEffects(stateForExecution, effect.effects, perIterationContext)

            if (result.outcome is Outcome.Paused) {
                return EffectResult.propagatePause(
                    result.state,
                    allEvents + result.events
                )
            }

            currentState = if (remaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
            stored = stored.copy(
                updatedCollections = stored.updatedCollections + result.updatedCollections,
                updatedSubtypeGroups = stored.updatedSubtypeGroups + result.updatedSubtypeGroups,
                updatedStoredNumbers = stored.updatedStoredNumbers + result.updatedStoredNumbers,
                updatedChosenValues = stored.updatedChosenValues + result.updatedChosenValues,
                updatedStringLists = stored.updatedStringLists + result.updatedStringLists,
                updatedSacrificedPermanents = stored.updatedSacrificedPermanents + result.updatedSacrificedPermanents,
                updatedDepartedPermanents = stored.updatedDepartedPermanents + result.updatedDepartedPermanents,
                updatedPermittedMoves = stored.updatedPermittedMoves + result.updatedPermittedMoves,
            )
        }

        // Published like a composite's outputs: a paused controller's program hands the frame
        // beneath the same outputs when it finishes.
        return stored.copy(state = currentState, events = allEvents)
    }

    /**
     * One controller's sub-effects, as an effect program over them: the same run, pause and finish
     * as any composite, so a paused iteration publishes exactly what an unpaused one returns.
     */
    private fun executeSubEffects(
        state: GameState,
        effects: List<Effect>,
        context: EffectContext
    ): EffectResult = interpreter.run(state, CompositeEffect(effects), context.copy(resolutionDepth = context.resolutionDepth + 1))
}
