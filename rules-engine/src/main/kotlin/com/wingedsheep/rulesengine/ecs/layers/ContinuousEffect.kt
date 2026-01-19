package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Represents an active continuous effect in the game.
 *
 * Continuous effects are created by spells and abilities that modify game objects
 * for a duration (e.g., "Target creature gets +3/+3 until end of turn").
 *
 * Unlike static abilities (which are inherent to cards on the battlefield),
 * continuous effects are "floating" - they exist independently of their source
 * and persist based on their duration.
 *
 * Key principles:
 * - The effect persists even if the source leaves the battlefield
 * - Effects are applied in layer order, then by timestamp
 * - Effects expire based on their duration
 *
 * Example: Giant Growth creates a continuous effect with:
 * - modification: ModifyPT(+3, +3)
 * - filter: Specific(targetCreatureId)
 * - duration: UntilEndOfTurn
 * - layer: PT_MODIFY
 */
@Serializable
data class ActiveContinuousEffect(
    /**
     * Unique identifier for this effect instance.
     */
    val id: EntityId = EntityId.generate(),

    /**
     * The entity that created this effect (the spell/ability source).
     * Used for tracking and debugging, not for effect application.
     */
    val sourceId: EntityId,

    /**
     * A human-readable description of this effect (e.g., "Giant Growth").
     */
    val description: String,

    /**
     * The layer in which this effect applies (per MTG Rule 613).
     */
    val layer: Layer,

    /**
     * The actual modification to apply.
     */
    val modification: Modification,

    /**
     * Filter determining which entities this effect affects.
     */
    val filter: ModifierFilter,

    /**
     * How long this effect lasts.
     */
    val duration: EffectDuration,

    /**
     * Timestamp for ordering effects within the same layer.
     * Earlier timestamps apply first.
     */
    val timestamp: Long = Modifier.nextTimestamp(),

    /**
     * The turn number when this effect was created.
     * Used for "until end of turn" and "until your next turn" effects.
     */
    val createdOnTurn: Int = 0,

    /**
     * The controller who created this effect.
     * Used for "until your next turn" effects.
     */
    val controllerId: EntityId? = null
) {
    /**
     * Convert this continuous effect to a Modifier for the StateProjector.
     */
    fun toModifier(): Modifier = Modifier(
        layer = layer,
        sourceId = sourceId,
        timestamp = timestamp,
        modification = modification,
        filter = filter
    )

    companion object {
        /**
         * Create a continuous effect from a Modifier with a duration.
         */
        fun fromModifier(
            modifier: Modifier,
            duration: EffectDuration,
            description: String,
            createdOnTurn: Int = 0,
            controllerId: EntityId? = null
        ): ActiveContinuousEffect = ActiveContinuousEffect(
            sourceId = modifier.sourceId,
            description = description,
            layer = modifier.layer,
            modification = modifier.modification,
            filter = modifier.filter,
            duration = duration,
            timestamp = modifier.timestamp,
            createdOnTurn = createdOnTurn,
            controllerId = controllerId
        )
    }
}

/**
 * Defines how long a continuous effect lasts.
 *
 * Per MTG rules, effects can have various durations:
 * - Until end of turn (most common for combat tricks)
 * - Until your next turn (some defensive effects)
 * - Until end of combat
 * - While source is on battlefield (handled differently - via static abilities)
 * - Indefinitely (rare, usually from one-shot effects)
 */
@Serializable
sealed interface EffectDuration {
    /**
     * Effect lasts until the cleanup step of the current turn.
     * Most common duration for spells like Giant Growth.
     */
    @Serializable
    data object UntilEndOfTurn : EffectDuration

    /**
     * Effect lasts until the end of the current combat phase.
     * Used by some combat-related effects.
     */
    @Serializable
    data object UntilEndOfCombat : EffectDuration

    /**
     * Effect lasts until the beginning of a specific player's next turn.
     * The playerId is who's "next turn" we're waiting for.
     */
    @Serializable
    data class UntilNextTurn(val playerId: EntityId) : EffectDuration

    /**
     * Effect lasts until the beginning of the controller's next upkeep.
     */
    @Serializable
    data class UntilYourNextUpkeep(val playerId: EntityId) : EffectDuration

    /**
     * Effect lasts while a specific permanent remains on the battlefield.
     * When the permanent leaves, the effect ends.
     */
    @Serializable
    data class WhileOnBattlefield(val permanentId: EntityId) : EffectDuration

    /**
     * Effect lasts while a specific permanent remains attached.
     * When the permanent becomes unattached, the effect ends.
     */
    @Serializable
    data class WhileAttached(val attachmentId: EntityId) : EffectDuration

    /**
     * Effect lasts indefinitely (permanent one-shot modification).
     * Rare - used for effects like "becomes a 0/1 Frog" with no duration.
     */
    @Serializable
    data object Indefinite : EffectDuration

    /**
     * Effect lasts for a specific number of turns.
     * The counter tracks remaining turns.
     */
    @Serializable
    data class ForTurns(val remainingTurns: Int, val countingPlayerId: EntityId) : EffectDuration
}

/**
 * Helper functions for creating common continuous effects.
 */
object ContinuousEffectFactory {

    /**
     * Create a "target creature gets +X/+Y until end of turn" effect.
     */
    fun pumpCreature(
        sourceId: EntityId,
        targetId: EntityId,
        powerBonus: Int,
        toughnessBonus: Int,
        description: String = "+$powerBonus/+$toughnessBonus until end of turn",
        createdOnTurn: Int = 0
    ): ActiveContinuousEffect = ActiveContinuousEffect(
        sourceId = sourceId,
        description = description,
        layer = Layer.PT_MODIFY,
        modification = Modification.ModifyPT(powerBonus, toughnessBonus),
        filter = ModifierFilter.Specific(targetId),
        duration = EffectDuration.UntilEndOfTurn,
        createdOnTurn = createdOnTurn
    )

    /**
     * Create a "target creature gains [keyword] until end of turn" effect.
     */
    fun grantKeyword(
        sourceId: EntityId,
        targetId: EntityId,
        keyword: com.wingedsheep.rulesengine.core.Keyword,
        description: String = "Gains ${keyword.name.lowercase()} until end of turn",
        createdOnTurn: Int = 0
    ): ActiveContinuousEffect = ActiveContinuousEffect(
        sourceId = sourceId,
        description = description,
        layer = Layer.ABILITY,
        modification = Modification.AddKeyword(keyword),
        filter = ModifierFilter.Specific(targetId),
        duration = EffectDuration.UntilEndOfTurn,
        createdOnTurn = createdOnTurn
    )

    /**
     * Create a "target creature becomes [P]/[T] until end of turn" effect.
     */
    fun setStats(
        sourceId: EntityId,
        targetId: EntityId,
        power: Int,
        toughness: Int,
        description: String = "Becomes $power/$toughness until end of turn",
        createdOnTurn: Int = 0
    ): ActiveContinuousEffect = ActiveContinuousEffect(
        sourceId = sourceId,
        description = description,
        layer = Layer.PT_SET,
        modification = Modification.SetPT(power, toughness),
        filter = ModifierFilter.Specific(targetId),
        duration = EffectDuration.UntilEndOfTurn,
        createdOnTurn = createdOnTurn
    )

    /**
     * Create a "creatures you control get +X/+Y until end of turn" effect.
     */
    fun pumpAllCreatures(
        sourceId: EntityId,
        controllerId: EntityId,
        powerBonus: Int,
        toughnessBonus: Int,
        description: String = "Creatures you control get +$powerBonus/+$toughnessBonus until end of turn",
        createdOnTurn: Int = 0
    ): ActiveContinuousEffect = ActiveContinuousEffect(
        sourceId = sourceId,
        description = description,
        layer = Layer.PT_MODIFY,
        modification = Modification.ModifyPT(powerBonus, toughnessBonus),
        filter = ModifierFilter.ControlledBy(controllerId),
        duration = EffectDuration.UntilEndOfTurn,
        createdOnTurn = createdOnTurn,
        controllerId = controllerId
    )
}
