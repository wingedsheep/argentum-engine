package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.decision.PlayerDecision
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.ecs.layers.Modifier

class EffectExecutor(
    private val registry: EffectHandlerRegistry = EffectHandlerRegistry.default()
) {
    fun execute(
        state: GameState,
        effect: Effect,
        context: ExecutionContext
    ): ExecutionResult {
        return registry.execute(state, effect, context)
    }
}

data class ExecutionContext(
    val controllerId: EntityId,
    val sourceId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val targetsByIndex: Map<Int, List<ChosenTarget>> = emptyMap()
) {
    fun getTargetsForIndex(index: Int): List<ChosenTarget> {
        return targetsByIndex[index] ?: emptyList()
    }
}

data class ExecutionResult(
    val state: GameState,
    val events: List<EffectEvent> = emptyList(),
    val temporaryModifiers: List<Modifier> = emptyList(),
    val pendingDecision: PlayerDecision? = null,
    val continuation: EffectContinuation? = null
) {
    val needsPlayerInput: Boolean get() = pendingDecision != null
}

fun interface EffectContinuation {
    fun resume(selectedIds: List<EntityId>): ExecutionResult
}

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
