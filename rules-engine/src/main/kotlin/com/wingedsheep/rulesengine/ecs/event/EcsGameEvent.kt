package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.EcsActionEvent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import kotlinx.serialization.Serializable

/**
 * Unified game events for the ECS system.
 *
 * These events represent all observable game occurrences that can trigger abilities.
 * They can be created from EcsActionEvent (action execution) or EcsEvent (effect execution).
 *
 * Events are used for:
 * - Trigger detection (which triggered abilities should fire)
 * - Game logging and replay
 * - UI notifications
 */
@Serializable
sealed interface EcsGameEvent {

    // =========================================================================
    // Zone Change Events
    // =========================================================================

    /**
     * A permanent entered the battlefield.
     */
    @Serializable
    data class EnteredBattlefield(
        val entityId: EntityId,
        val cardName: String,
        val controllerId: EntityId,
        val fromZone: ZoneId?
    ) : EcsGameEvent

    /**
     * A permanent left the battlefield.
     */
    @Serializable
    data class LeftBattlefield(
        val entityId: EntityId,
        val cardName: String,
        val toZone: ZoneId
    ) : EcsGameEvent

    /**
     * A creature died (went to graveyard from battlefield).
     */
    @Serializable
    data class CreatureDied(
        val entityId: EntityId,
        val cardName: String,
        val ownerId: EntityId
    ) : EcsGameEvent

    /**
     * A card was exiled.
     */
    @Serializable
    data class CardExiled(
        val entityId: EntityId,
        val cardName: String,
        val fromZone: ZoneId?
    ) : EcsGameEvent

    /**
     * A card was returned to its owner's hand.
     */
    @Serializable
    data class ReturnedToHand(
        val entityId: EntityId,
        val cardName: String
    ) : EcsGameEvent

    // =========================================================================
    // Card Drawing Events
    // =========================================================================

    /**
     * A player drew a card.
     */
    @Serializable
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String
    ) : EcsGameEvent

    /**
     * A player discarded a card.
     */
    @Serializable
    data class CardDiscarded(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String
    ) : EcsGameEvent

    // =========================================================================
    // Combat Events
    // =========================================================================

    /**
     * Combat began.
     */
    @Serializable
    data class CombatBegan(
        val attackingPlayerId: EntityId,
        val defendingPlayerId: EntityId
    ) : EcsGameEvent

    /**
     * A creature was declared as an attacker.
     */
    @Serializable
    data class AttackerDeclared(
        val creatureId: EntityId,
        val cardName: String,
        val attackingPlayerId: EntityId,
        val defendingPlayerId: EntityId
    ) : EcsGameEvent

    /**
     * A creature was declared as a blocker.
     */
    @Serializable
    data class BlockerDeclared(
        val blockerId: EntityId,
        val blockerName: String,
        val attackerId: EntityId
    ) : EcsGameEvent

    /**
     * Combat ended.
     */
    @Serializable
    data class CombatEnded(
        val activePlayerId: EntityId
    ) : EcsGameEvent

    // =========================================================================
    // Damage Events
    // =========================================================================

    /**
     * Damage was dealt to a player.
     */
    @Serializable
    data class DamageDealtToPlayer(
        val sourceId: EntityId?,
        val targetPlayerId: EntityId,
        val amount: Int,
        val isCombatDamage: Boolean = false
    ) : EcsGameEvent

    /**
     * Damage was dealt to a creature.
     */
    @Serializable
    data class DamageDealtToCreature(
        val sourceId: EntityId?,
        val targetCreatureId: EntityId,
        val amount: Int,
        val isCombatDamage: Boolean = false
    ) : EcsGameEvent

    // =========================================================================
    // Life Events
    // =========================================================================

    /**
     * A player gained life.
     */
    @Serializable
    data class LifeGained(
        val playerId: EntityId,
        val amount: Int,
        val newTotal: Int
    ) : EcsGameEvent

    /**
     * A player lost life.
     */
    @Serializable
    data class LifeLost(
        val playerId: EntityId,
        val amount: Int,
        val newTotal: Int
    ) : EcsGameEvent

    // =========================================================================
    // Phase/Step Events
    // =========================================================================

    /**
     * Upkeep began.
     */
    @Serializable
    data class UpkeepBegan(
        val activePlayerId: EntityId
    ) : EcsGameEvent

    /**
     * End step began.
     */
    @Serializable
    data class EndStepBegan(
        val activePlayerId: EntityId
    ) : EcsGameEvent

    // =========================================================================
    // Spell Events
    // =========================================================================

    /**
     * A spell was cast.
     */
    @Serializable
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId,
        val isCreatureSpell: Boolean,
        val isInstantOrSorcery: Boolean
    ) : EcsGameEvent

    // =========================================================================
    // Tap/Untap Events
    // =========================================================================

    /**
     * A permanent was tapped.
     */
    @Serializable
    data class PermanentTapped(
        val entityId: EntityId,
        val cardName: String
    ) : EcsGameEvent

    /**
     * A permanent was untapped.
     */
    @Serializable
    data class PermanentUntapped(
        val entityId: EntityId,
        val cardName: String
    ) : EcsGameEvent

    // =========================================================================
    // Counter Events
    // =========================================================================

    /**
     * Counters were added to a permanent.
     */
    @Serializable
    data class CountersAdded(
        val entityId: EntityId,
        val counterType: String,
        val count: Int
    ) : EcsGameEvent

    /**
     * Counters were removed from a permanent.
     */
    @Serializable
    data class CountersRemoved(
        val entityId: EntityId,
        val counterType: String,
        val count: Int
    ) : EcsGameEvent

    // =========================================================================
    // Game End Events
    // =========================================================================

    /**
     * A player lost the game.
     */
    @Serializable
    data class PlayerLost(
        val playerId: EntityId,
        val reason: String
    ) : EcsGameEvent

    /**
     * The game ended.
     */
    @Serializable
    data class GameEnded(
        val winnerId: EntityId?
    ) : EcsGameEvent
}

/**
 * Extension functions to convert from other event types to EcsGameEvent.
 */
object EcsGameEventConverter {

    /**
     * Convert an EcsActionEvent to EcsGameEvent(s).
     * Some action events may produce multiple game events.
     */
    fun fromActionEvent(event: EcsActionEvent): List<EcsGameEvent> {
        return when (event) {
            is EcsActionEvent.LifeChanged -> {
                val amount = event.newLife - event.oldLife
                if (amount > 0) {
                    listOf(EcsGameEvent.LifeGained(event.playerId, amount, event.newLife))
                } else if (amount < 0) {
                    listOf(EcsGameEvent.LifeLost(event.playerId, -amount, event.newLife))
                } else {
                    emptyList()
                }
            }

            is EcsActionEvent.DamageDealtToPlayer -> listOf(
                EcsGameEvent.DamageDealtToPlayer(event.sourceId, event.targetId, event.amount)
            )

            is EcsActionEvent.DamageDealtToCreature -> listOf(
                EcsGameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount)
            )

            is EcsActionEvent.CardDrawn -> listOf(
                EcsGameEvent.CardDrawn(event.playerId, event.cardId, event.cardName)
            )

            is EcsActionEvent.CardDiscarded -> listOf(
                EcsGameEvent.CardDiscarded(event.playerId, event.cardId, event.cardName)
            )

            is EcsActionEvent.CreatureDied -> listOf(
                EcsGameEvent.CreatureDied(event.entityId, event.name, event.ownerId)
            )

            is EcsActionEvent.CardExiled -> listOf(
                EcsGameEvent.CardExiled(event.entityId, event.name, null)
            )

            is EcsActionEvent.CardReturnedToHand -> listOf(
                EcsGameEvent.ReturnedToHand(event.entityId, event.name)
            )

            is EcsActionEvent.PermanentTapped -> listOf(
                EcsGameEvent.PermanentTapped(event.entityId, event.name)
            )

            is EcsActionEvent.PermanentUntapped -> listOf(
                EcsGameEvent.PermanentUntapped(event.entityId, event.name)
            )

            is EcsActionEvent.CombatStarted -> listOf(
                EcsGameEvent.CombatBegan(event.attackingPlayerId, event.defendingPlayerId)
            )

            is EcsActionEvent.AttackerDeclared -> listOf(
                // Note: We don't have full context here, would need state to populate defendingPlayerId
                EcsGameEvent.AttackerDeclared(
                    event.creatureId,
                    event.name,
                    EntityId.of("unknown"), // Would be filled in by trigger detector with state
                    EntityId.of("unknown")
                )
            )

            is EcsActionEvent.BlockerDeclared -> listOf(
                EcsGameEvent.BlockerDeclared(event.blockerId, event.name, event.attackerId)
            )

            is EcsActionEvent.CombatEnded -> listOf(
                EcsGameEvent.CombatEnded(event.playerId)
            )

            is EcsActionEvent.PlayerLost -> listOf(
                EcsGameEvent.PlayerLost(event.playerId, event.reason)
            )

            is EcsActionEvent.GameEnded -> listOf(
                EcsGameEvent.GameEnded(event.winnerId)
            )

            // DamageDealt (unified) maps to the appropriate damage event
            is EcsActionEvent.DamageDealt -> {
                if (event.isCombatDamage) {
                    // Combat damage - could expand to track both player and creature damage
                    listOf(EcsGameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount))
                } else {
                    listOf(EcsGameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount))
                }
            }

            // Stack resolution events
            is EcsActionEvent.SpellCast -> listOf(
                EcsGameEvent.SpellCast(
                    spellId = event.entityId,
                    spellName = event.name,
                    casterId = event.casterId,
                    isCreatureSpell = false,  // Would need CardComponent to determine
                    isInstantOrSorcery = false // Would need CardComponent to determine
                )
            )

            is EcsActionEvent.PermanentEnteredBattlefield -> listOf(
                EcsGameEvent.EnteredBattlefield(
                    entityId = event.entityId,
                    cardName = event.name,
                    controllerId = event.controllerId,
                    fromZone = ZoneId.STACK
                )
            )

            // Events that don't directly map to trigger-relevant game events
            is EcsActionEvent.ManaAdded,
            is EcsActionEvent.DrawFailed,
            is EcsActionEvent.CardMoved,
            is EcsActionEvent.PermanentDestroyed,
            is EcsActionEvent.LandPlayed,
            is EcsActionEvent.Attached,
            is EcsActionEvent.Detached,
            is EcsActionEvent.BlockersOrdered,
            is EcsActionEvent.SpellResolved,
            is EcsActionEvent.SpellFizzled,
            is EcsActionEvent.AbilityResolved,
            is EcsActionEvent.AbilityFizzled -> emptyList()
        }
    }

    /**
     * Convert an EcsEvent (from effect execution) to EcsGameEvent(s).
     */
    fun fromEffectEvent(event: EcsEvent): List<EcsGameEvent> {
        return when (event) {
            is EcsEvent.LifeGained -> listOf(
                EcsGameEvent.LifeGained(event.playerId, event.amount, 0) // Total not available
            )

            is EcsEvent.LifeLost -> listOf(
                EcsGameEvent.LifeLost(event.playerId, event.amount, 0) // Total not available
            )

            is EcsEvent.DamageDealtToPlayer -> listOf(
                EcsGameEvent.DamageDealtToPlayer(event.sourceId, event.targetId, event.amount)
            )

            is EcsEvent.DamageDealtToCreature -> listOf(
                EcsGameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount)
            )

            is EcsEvent.CardDrawn -> listOf(
                EcsGameEvent.CardDrawn(event.playerId, event.cardId, event.cardName)
            )

            is EcsEvent.CardDiscarded -> listOf(
                EcsGameEvent.CardDiscarded(event.playerId, event.cardId, event.cardName)
            )

            is EcsEvent.CreatureDied -> listOf(
                EcsGameEvent.CreatureDied(event.entityId, event.name, event.ownerId)
            )

            is EcsEvent.PermanentExiled -> listOf(
                EcsGameEvent.CardExiled(event.entityId, event.name, null)
            )

            is EcsEvent.PermanentReturnedToHand -> listOf(
                EcsGameEvent.ReturnedToHand(event.entityId, event.name)
            )

            is EcsEvent.PermanentTapped -> listOf(
                EcsGameEvent.PermanentTapped(event.entityId, event.name)
            )

            is EcsEvent.PermanentUntapped -> listOf(
                EcsGameEvent.PermanentUntapped(event.entityId, event.name)
            )

            is EcsEvent.CountersAdded -> listOf(
                EcsGameEvent.CountersAdded(event.entityId, event.counterType, event.count)
            )

            // Events that don't map to trigger-relevant game events
            is EcsEvent.DrawFailed,
            is EcsEvent.PermanentDestroyed,
            is EcsEvent.StatsModified,
            is EcsEvent.ManaAdded,
            is EcsEvent.TokenCreated,
            is EcsEvent.KeywordGranted -> emptyList()
        }
    }
}
