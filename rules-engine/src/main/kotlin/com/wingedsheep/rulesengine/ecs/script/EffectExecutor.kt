package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.decision.EffectDecision
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
 *     state = gameState,
 *     effect = DealDamageEffect(3, EffectTarget.TargetCreature),
 *     context = ExecutionContext(controllerId, sourceId, targets)
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
 */
data class ExecutionContext(
    val controllerId: EntityId,
    val sourceId: EntityId,
    val targets: List<ChosenTarget> = emptyList()
)

/**
 * Result of effect execution.
 *
 * When an effect needs player input, it sets [pendingDecision] and [continuation].
 * The game loop should:
 * 1. Present the decision to the player
 * 2. Collect the response
 * 3. Call the continuation with the response to complete the effect
 *
 * If [pendingDecision] is null, the effect completed synchronously.
 */
data class ExecutionResult(
    val state: GameState,
    val events: List<EffectEvent> = emptyList(),
    val temporaryModifiers: List<Modifier> = emptyList(),
    /**
     * A decision that needs player input before the effect can complete.
     * Null if the effect completed without needing input.
     */
    val pendingDecision: EffectDecision? = null,
    /**
     * Continuation to call when the pending decision is resolved.
     * The game loop should call this with the player's response.
     * Null if no pending decision.
     */
    val continuation: EffectContinuation? = null
) {
    /** Whether this result requires player input before completing */
    val needsPlayerInput: Boolean get() = pendingDecision != null
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
}
