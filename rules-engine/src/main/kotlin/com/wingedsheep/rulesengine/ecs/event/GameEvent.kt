package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import kotlinx.serialization.Serializable

/**
 * Unified game events for the ECS system.
 *
 * These events represent all observable game occurrences that can trigger abilities.
 * They can be created from GameActionEvent (action execution) or EffectEvent (effect execution).
 *
 * Events are used for:
 * - Trigger detection (which triggered abilities should fire)
 * - Game logging and replay
 * - UI notifications
 */
@Serializable
sealed interface GameEvent {

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
    ) : GameEvent

    /**
     * A permanent left the battlefield.
     */
    @Serializable
    data class LeftBattlefield(
        val entityId: EntityId,
        val cardName: String,
        val toZone: ZoneId
    ) : GameEvent

    /**
     * A creature died (went to graveyard from battlefield).
     */
    @Serializable
    data class CreatureDied(
        val entityId: EntityId,
        val cardName: String,
        val ownerId: EntityId
    ) : GameEvent

    /**
     * A card was exiled.
     */
    @Serializable
    data class CardExiled(
        val entityId: EntityId,
        val cardName: String,
        val fromZone: ZoneId?
    ) : GameEvent

    /**
     * A card was returned to its owner's hand.
     */
    @Serializable
    data class ReturnedToHand(
        val entityId: EntityId,
        val cardName: String
    ) : GameEvent

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
    ) : GameEvent

    /**
     * A player discarded a card.
     */
    @Serializable
    data class CardDiscarded(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String
    ) : GameEvent

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
    ) : GameEvent

    /**
     * A creature was declared as an attacker.
     */
    @Serializable
    data class AttackerDeclared(
        val creatureId: EntityId,
        val cardName: String,
        val attackingPlayerId: EntityId,
        val defendingPlayerId: EntityId
    ) : GameEvent

    /**
     * A creature was declared as a blocker.
     */
    @Serializable
    data class BlockerDeclared(
        val blockerId: EntityId,
        val blockerName: String,
        val attackerId: EntityId
    ) : GameEvent

    /**
     * Combat ended.
     */
    @Serializable
    data class CombatEnded(
        val activePlayerId: EntityId
    ) : GameEvent

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
    ) : GameEvent

    /**
     * Damage was dealt to a creature.
     */
    @Serializable
    data class DamageDealtToCreature(
        val sourceId: EntityId?,
        val targetCreatureId: EntityId,
        val amount: Int,
        val isCombatDamage: Boolean = false
    ) : GameEvent

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
    ) : GameEvent

    /**
     * A player lost life.
     */
    @Serializable
    data class LifeLost(
        val playerId: EntityId,
        val amount: Int,
        val newTotal: Int
    ) : GameEvent

    // =========================================================================
    // Phase/Step Events
    // =========================================================================

    /**
     * Upkeep began.
     */
    @Serializable
    data class UpkeepBegan(
        val activePlayerId: EntityId
    ) : GameEvent

    /**
     * End step began.
     */
    @Serializable
    data class EndStepBegan(
        val activePlayerId: EntityId
    ) : GameEvent

    /**
     * First main phase began (pre-combat main phase).
     */
    @Serializable
    data class FirstMainPhaseBegan(
        val activePlayerId: EntityId
    ) : GameEvent

    // =========================================================================
    // Transform Events
    // =========================================================================

    /**
     * A double-faced permanent transformed.
     */
    @Serializable
    data class Transformed(
        val entityId: EntityId,
        val cardName: String,
        val toBackFace: Boolean  // true if transformed to back face, false if to front
    ) : GameEvent

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
    ) : GameEvent

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
    ) : GameEvent

    /**
     * A permanent was untapped.
     */
    @Serializable
    data class PermanentUntapped(
        val entityId: EntityId,
        val cardName: String
    ) : GameEvent

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
    ) : GameEvent

    /**
     * Counters were removed from a permanent.
     */
    @Serializable
    data class CountersRemoved(
        val entityId: EntityId,
        val counterType: String,
        val count: Int
    ) : GameEvent

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
    ) : GameEvent

    /**
     * The game ended.
     */
    @Serializable
    data class GameEnded(
        val winnerId: EntityId?
    ) : GameEvent
}

/**
 * Extension functions to convert from other event types to GameEvent.
 */
object GameEventConverter {

    /**
     * Convert an GameActionEvent to GameEvent(s).
     * Some action events may produce multiple game events.
     */
    fun fromActionEvent(event: GameActionEvent): List<GameEvent> {
        return when (event) {
            is GameActionEvent.LifeChanged -> {
                val amount = event.newLife - event.oldLife
                if (amount > 0) {
                    listOf(GameEvent.LifeGained(event.playerId, amount, event.newLife))
                } else if (amount < 0) {
                    listOf(GameEvent.LifeLost(event.playerId, -amount, event.newLife))
                } else {
                    emptyList()
                }
            }

            is GameActionEvent.DamageDealtToPlayer -> listOf(
                GameEvent.DamageDealtToPlayer(event.sourceId, event.targetId, event.amount)
            )

            is GameActionEvent.DamageDealtToCreature -> listOf(
                GameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount)
            )

            is GameActionEvent.CardDrawn -> listOf(
                GameEvent.CardDrawn(event.playerId, event.cardId, event.cardName)
            )

            is GameActionEvent.CardDiscarded -> listOf(
                GameEvent.CardDiscarded(event.playerId, event.cardId, event.cardName)
            )

            is GameActionEvent.CreatureDied -> listOf(
                GameEvent.CreatureDied(event.entityId, event.name, event.ownerId)
            )

            is GameActionEvent.CardExiled -> listOf(
                GameEvent.CardExiled(event.entityId, event.name, null)
            )

            is GameActionEvent.CardReturnedToHand -> listOf(
                GameEvent.ReturnedToHand(event.entityId, event.name)
            )

            is GameActionEvent.PermanentTapped -> listOf(
                GameEvent.PermanentTapped(event.entityId, event.name)
            )

            is GameActionEvent.PermanentUntapped -> listOf(
                GameEvent.PermanentUntapped(event.entityId, event.name)
            )

            is GameActionEvent.CombatStarted -> listOf(
                GameEvent.CombatBegan(event.attackingPlayerId, event.defendingPlayerId)
            )

            is GameActionEvent.AttackerDeclared -> listOf(
                // Note: We don't have full context here, would need state to populate defendingPlayerId
                GameEvent.AttackerDeclared(
                    event.creatureId,
                    event.name,
                    EntityId.of("unknown"), // Would be filled in by trigger detector with state
                    EntityId.of("unknown")
                )
            )

            is GameActionEvent.BlockerDeclared -> listOf(
                GameEvent.BlockerDeclared(event.blockerId, event.name, event.attackerId)
            )

            is GameActionEvent.CombatEnded -> listOf(
                GameEvent.CombatEnded(event.playerId)
            )

            is GameActionEvent.PlayerLost -> listOf(
                GameEvent.PlayerLost(event.playerId, event.reason)
            )

            is GameActionEvent.GameEnded -> listOf(
                GameEvent.GameEnded(event.winnerId)
            )

            // DamageDealt (unified) maps to the appropriate damage event
            is GameActionEvent.DamageDealt -> {
                if (event.isCombatDamage) {
                    // Combat damage - could expand to track both player and creature damage
                    // Assuming for now it's mostly creatures in this context, or differentiating via target
                    listOf(GameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount))
                } else {
                    listOf(GameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount))
                }
            }

            // Stack resolution events
            is GameActionEvent.SpellCast -> listOf(
                GameEvent.SpellCast(
                    spellId = event.entityId,
                    spellName = event.name,
                    casterId = event.casterId,
                    isCreatureSpell = false,  // Would need CardComponent to determine
                    isInstantOrSorcery = false // Would need CardComponent to determine
                )
            )

            is GameActionEvent.PermanentEnteredBattlefield -> listOf(
                GameEvent.EnteredBattlefield(
                    entityId = event.entityId,
                    cardName = event.name,
                    controllerId = event.controllerId,
                    fromZone = ZoneId.STACK
                )
            )

            // Events that don't directly map to trigger-relevant game events
            is GameActionEvent.ManaAdded,
            is GameActionEvent.DrawFailed,
            is GameActionEvent.CardMoved,
            is GameActionEvent.PermanentDestroyed,
            is GameActionEvent.LandPlayed,
            is GameActionEvent.Attached,
            is GameActionEvent.Detached,
            is GameActionEvent.BlockersOrdered,
            is GameActionEvent.SpellResolved,
            is GameActionEvent.SpellFizzled,
            is GameActionEvent.AbilityResolved,
            is GameActionEvent.AbilityFizzled,
            is GameActionEvent.DecisionSubmitted -> emptyList()

            is GameActionEvent.CounterAdded -> listOf(
                GameEvent.CountersAdded(event.entityId, event.counterType, event.count)
            )

            is GameActionEvent.CounterRemoved -> listOf(
                GameEvent.CountersRemoved(event.entityId, event.counterType, event.count)
            )
        }
    }

    /**
     * Convert an EffectEvent (from effect execution) to GameEvent(s).
     */
    fun fromEffectEvent(event: EffectEvent): List<GameEvent> {
        return when (event) {
            is EffectEvent.LifeGained -> listOf(
                GameEvent.LifeGained(event.playerId, event.amount, 0) // Total not available
            )

            is EffectEvent.LifeLost -> listOf(
                GameEvent.LifeLost(event.playerId, event.amount, 0) // Total not available
            )

            is EffectEvent.DamageDealtToPlayer -> listOf(
                GameEvent.DamageDealtToPlayer(event.sourceId, event.targetId, event.amount)
            )

            is EffectEvent.DamageDealtToCreature -> listOf(
                GameEvent.DamageDealtToCreature(event.sourceId, event.targetId, event.amount)
            )

            is EffectEvent.CardDrawn -> listOf(
                GameEvent.CardDrawn(event.playerId, event.cardId, event.cardName)
            )

            is EffectEvent.CardDiscarded -> listOf(
                GameEvent.CardDiscarded(event.playerId, event.cardId, event.cardName)
            )

            is EffectEvent.CreatureDied -> listOf(
                GameEvent.CreatureDied(event.entityId, event.name, event.ownerId)
            )

            is EffectEvent.PermanentExiled -> listOf(
                GameEvent.CardExiled(event.entityId, event.name, ZoneId.BATTLEFIELD)
            )

            is EffectEvent.CardExiled -> listOf(
                GameEvent.CardExiled(event.cardId, event.cardName, null)
            )

            is EffectEvent.PermanentReturnedToHand -> listOf(
                GameEvent.ReturnedToHand(event.entityId, event.name)
            )

            is EffectEvent.PermanentTapped -> listOf(
                GameEvent.PermanentTapped(event.entityId, event.name)
            )

            is EffectEvent.PermanentUntapped -> listOf(
                GameEvent.PermanentUntapped(event.entityId, event.name)
            )

            is EffectEvent.CountersAdded -> listOf(
                GameEvent.CountersAdded(event.entityId, event.counterType, event.count)
            )

            // Events that don't map to trigger-relevant game events
            is EffectEvent.DrawFailed,
            is EffectEvent.PermanentDestroyed,
            is EffectEvent.PermanentSacrificed,
            is EffectEvent.StatsModified,
            is EffectEvent.ManaAdded,
            is EffectEvent.TokenCreated,
            is EffectEvent.KeywordGranted,
            is EffectEvent.LibraryShuffled,
            is EffectEvent.LibrarySearched,
            is EffectEvent.CardMovedToZone,
            is EffectEvent.SpellCountered,
            is EffectEvent.SpellCast,
            is EffectEvent.SpellCastCancelled -> emptyList()
        }
    }
}
