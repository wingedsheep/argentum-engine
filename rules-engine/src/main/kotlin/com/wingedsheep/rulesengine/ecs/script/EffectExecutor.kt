package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.decision.PlayerDecision
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.ecs.layers.Modifier

/**
 * Executes effects against an GameState.
 *
 * This is the ECS counterpart to EffectExecutor, working with the new
 * entity-component architecture instead of the legacy GameState.
 *
 * Each effect is executed as a pure transformation:
 * (GameState, Effect, Context) -> GameState
 *
 * Uses a registry-based handler pattern for extensibility - new effects
 * can be added by creating handlers without modifying this class.
 *
 * Example usage:
 * ```kotlin
 * val executor = EffectExecutor()
 * val newState = executor.execute(
 * state = gameState,
 * effect = DealDamageEffect(3, EffectTarget.TargetCreature),
 * context = ExecutionContext(controllerId, sourceId, targets)
 * )
 * ```
 */
class EffectExecutor(
    private val registry: EffectHandlerRegistry = EffectHandlerRegistry.default()
) {

    /**
     * Execute an effect and return the new game state.
     *
     * Delegates to the appropriate handler from the registry.
     */
    fun execute(
        state: GameState,
        effect: Effect,
        context: ExecutionContext
    ): ExecutionResult {
        return registry.execute(state, effect, context)
    }
}

/**
 * Context for effect execution.
 *
 * Targets are represented as ChosenTarget, which is the unified target type
 * used throughout the ECS architecture.
 *
 * @property targetsByIndex Map of Requirement Index -> List of Chosen Targets.
 * This is the robust way to handle multi-target spells.
 */
data class ExecutionContext(
    val controllerId: EntityId,
    val sourceId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val targetsByIndex: Map<Int, List<ChosenTarget>> = emptyMap()
) {
    /**
     * Helper to get targets for a specific requirement index.
     */
    fun getTargetsForIndex(index: Int): List<ChosenTarget> {
        return targetsByIndex[index] ?: emptyList()
    }
}

/**
 * Result of effect execution.
 *
 * When an effect needs player input, the decision and context are stored in the
 * [state]'s `pendingDecision` and `decisionContext` fields. The game loop should:
 * 1. Check if [state.isPausedForDecision]
 * 2. If so, present the decision to the player
 * 3. Collect the response
 * 4. Submit it via [SubmitDecision] action to complete the effect
 *
 * ## Migration from Lambda-Based Continuations
 *
 * The old pattern used [pendingDecision] and [continuation] fields on this class.
 * The new stateless pattern stores decisions in [GameState] instead:
 *
 * **Old (deprecated):**
 * ```kotlin
 * val result = executor.execute(state, effect, context)
 * if (result.pendingDecision != null) {
 *     val response = getPlayerResponse(result.pendingDecision)
 *     val finalResult = result.continuation!!.resume(response)
 * }
 * ```
 *
 * **New (preferred):**
 * ```kotlin
 * val result = executor.execute(state, effect, context)
 * if (result.state.isPausedForDecision) {
 *     val response = getPlayerResponse(result.state.pendingDecision!!)
 *     val finalResult = actionHandler.execute(result.state, SubmitDecision(response))
 * }
 * ```
 */
data class ExecutionResult(
    val state: GameState,
    val events: List<EffectEvent> = emptyList(),
    val temporaryModifiers: List<Modifier> = emptyList(),
    /**
     * A decision that needs player input before the effect can complete.
     *
     * @deprecated Use [state.pendingDecision] instead. Decisions are now stored in
     * [GameState] for stateless resumption. This field is retained for backward
     * compatibility during migration.
     */
    @Deprecated(
        message = "Use state.pendingDecision instead for stateless decision handling",
        replaceWith = ReplaceWith("state.pendingDecision")
    )
    val pendingDecision: PlayerDecision? = null,
    /**
     * Continuation to call when the pending decision is resolved.
     *
     * @deprecated Lambda-based continuations are not serializable and prevent
     * stateless game resumption. Use [SubmitDecision] action with [DecisionResumer]
     * instead. This field is retained for backward compatibility during migration.
     *
     * @see com.wingedsheep.rulesengine.decision.DecisionResumer
     */
    @Deprecated(
        message = "Use SubmitDecision action with DecisionResumer instead for stateless decision handling",
        replaceWith = ReplaceWith("actionHandler.execute(state, SubmitDecision(response))")
    )
    val continuation: EffectContinuation? = null
) {
    /**
     * Whether this result requires player input before completing.
     *
     * Checks both the legacy [pendingDecision] field and the new stateless
     * [GameState.isPausedForDecision] for backward compatibility.
     */
    val needsPlayerInput: Boolean get() = state.isPausedForDecision || pendingDecision != null
}

/**
 * A continuation for resuming effect execution after player input.
 */
fun interface EffectContinuation {
    /**
     * Resume effect execution with the selected entity IDs.
     * @param selectedIds The entities selected by the player
     * @return The final result after completing the effect
     */
    fun resume(selectedIds: List<EntityId>): ExecutionResult
}

/**
 * Events generated during effect execution.
 */
sealed interface EffectEvent {
    data class LifeGained(val playerId: EntityId, val amount: Int) : EffectEvent
    data class LifeLost(val playerId: EntityId, val amount: Int) : EffectEvent
    data class DamageDealtToPlayer(val sourceId: EntityId, val targetId: EntityId, val amount: Int) : EffectEvent
    data class DamageDealtToCreature(val sourceId: EntityId, val targetId: EntityId, val amount: Int) : EffectEvent
    data class CardDrawn(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EffectEvent
    data class DrawFailed(val playerId: EntityId) : EffectEvent
    data class CardDiscarded(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EffectEvent
    data class PermanentDestroyed(val entityId: EntityId, val name: String) : EffectEvent
    data class CreatureDied(val entityId: EntityId, val name: String, val ownerId: EntityId) : EffectEvent
    data class PermanentExiled(val entityId: EntityId, val name: String) : EffectEvent
    data class CardExiled(val cardId: EntityId, val cardName: String) : EffectEvent
    data class PermanentReturnedToHand(val entityId: EntityId, val name: String) : EffectEvent
    data class PermanentTapped(val entityId: EntityId, val name: String) : EffectEvent
    data class PermanentUntapped(val entityId: EntityId, val name: String) : EffectEvent
    data class StatsModified(val entityId: EntityId, val powerDelta: Int, val toughnessDelta: Int) : EffectEvent
    data class CountersAdded(val entityId: EntityId, val counterType: String, val count: Int) : EffectEvent
    data class ManaAdded(val playerId: EntityId, val color: String, val amount: Int) : EffectEvent
    data class TokenCreated(val controllerId: EntityId, val count: Int, val description: String) : EffectEvent
    data class KeywordGranted(val entityId: EntityId, val keyword: com.wingedsheep.rulesengine.core.Keyword) : EffectEvent
    data class LibraryShuffled(val playerId: EntityId) : EffectEvent
    data class LibrarySearched(val playerId: EntityId, val foundCount: Int, val filterDescription: String) : EffectEvent
    data class CardMovedToZone(val cardId: EntityId, val cardName: String, val toZone: String) : EffectEvent
    data class PermanentSacrificed(val entityId: EntityId, val name: String, val controllerId: EntityId) : EffectEvent
    data class SpellCountered(val spellEntityId: EntityId, val spellName: String, val ownerId: EntityId) : EffectEvent
}
