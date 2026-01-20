package com.wingedsheep.gameserver.dto

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client-facing game events.
 *
 * These are simplified versions of internal GameActionEvents that:
 * - Only include information relevant to the client
 * - Use consistent naming conventions
 * - Are stable for client consumption
 * - Don't leak internal implementation details
 */
@Serializable
sealed interface ClientEvent {
    /** A description of what happened for display */
    val description: String

    // =========================================================================
    // Life/Damage Events
    // =========================================================================

    @Serializable
    @SerialName("lifeChanged")
    data class LifeChanged(
        val playerId: EntityId,
        val oldLife: Int,
        val newLife: Int,
        val change: Int
    ) : ClientEvent {
        override val description: String = if (change > 0) {
            "Player gained $change life"
        } else {
            "Player lost ${-change} life"
        }
    }

    @Serializable
    @SerialName("damageDealt")
    data class DamageDealt(
        val sourceId: EntityId?,
        val sourceName: String?,
        val targetId: EntityId,
        val targetName: String,
        val amount: Int,
        val targetIsPlayer: Boolean,
        val isCombatDamage: Boolean
    ) : ClientEvent {
        override val description: String = buildString {
            if (sourceName != null) {
                append("$sourceName dealt ")
            } else {
                append("Dealt ")
            }
            append("$amount damage to $targetName")
        }
    }

    // =========================================================================
    // Card Movement Events
    // =========================================================================

    @Serializable
    @SerialName("cardDrawn")
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String?  // Null if hidden from viewing player
    ) : ClientEvent {
        override val description: String = if (cardName != null) {
            "Drew $cardName"
        } else {
            "Drew a card"
        }
    }

    @Serializable
    @SerialName("cardDiscarded")
    data class CardDiscarded(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String
    ) : ClientEvent {
        override val description: String = "Discarded $cardName"
    }

    @Serializable
    @SerialName("permanentEntered")
    data class PermanentEntered(
        val cardId: EntityId,
        val cardName: String,
        val controllerId: EntityId,
        val enteredTapped: Boolean
    ) : ClientEvent {
        override val description: String = "$cardName entered the battlefield" +
            if (enteredTapped) " tapped" else ""
    }

    @Serializable
    @SerialName("permanentLeft")
    data class PermanentLeft(
        val cardId: EntityId,
        val cardName: String,
        val destination: String  // "graveyard", "exile", "hand", "library"
    ) : ClientEvent {
        override val description: String = "$cardName went to $destination"
    }

    // =========================================================================
    // Combat Events
    // =========================================================================

    @Serializable
    @SerialName("creatureAttacked")
    data class CreatureAttacked(
        val creatureId: EntityId,
        val creatureName: String,
        val attackingPlayerId: EntityId,
        val defendingPlayerId: EntityId
    ) : ClientEvent {
        override val description: String = "$creatureName attacked"
    }

    @Serializable
    @SerialName("creatureBlocked")
    data class CreatureBlocked(
        val blockerId: EntityId,
        val blockerName: String,
        val attackerId: EntityId,
        val attackerName: String
    ) : ClientEvent {
        override val description: String = "$blockerName blocked $attackerName"
    }

    @Serializable
    @SerialName("creatureDied")
    data class CreatureDied(
        val creatureId: EntityId,
        val creatureName: String
    ) : ClientEvent {
        override val description: String = "$creatureName died"
    }

    // =========================================================================
    // Spell/Ability Events
    // =========================================================================

    @Serializable
    @SerialName("spellCast")
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId
    ) : ClientEvent {
        override val description: String = "Cast $spellName"
    }

    @Serializable
    @SerialName("spellResolved")
    data class SpellResolved(
        val spellId: EntityId,
        val spellName: String
    ) : ClientEvent {
        override val description: String = "$spellName resolved"
    }

    @Serializable
    @SerialName("spellCountered")
    data class SpellCountered(
        val spellId: EntityId,
        val spellName: String
    ) : ClientEvent {
        override val description: String = "$spellName was countered"
    }

    @Serializable
    @SerialName("abilityTriggered")
    data class AbilityTriggered(
        val sourceId: EntityId,
        val sourceName: String,
        val abilityDescription: String
    ) : ClientEvent {
        override val description: String = "$sourceName: $abilityDescription"
    }

    @Serializable
    @SerialName("abilityActivated")
    data class AbilityActivated(
        val sourceId: EntityId,
        val sourceName: String,
        val abilityDescription: String
    ) : ClientEvent {
        override val description: String = "Activated $sourceName: $abilityDescription"
    }

    // =========================================================================
    // State Change Events
    // =========================================================================

    @Serializable
    @SerialName("permanentTapped")
    data class PermanentTapped(
        val permanentId: EntityId,
        val permanentName: String
    ) : ClientEvent {
        override val description: String = "Tapped $permanentName"
    }

    @Serializable
    @SerialName("permanentUntapped")
    data class PermanentUntapped(
        val permanentId: EntityId,
        val permanentName: String
    ) : ClientEvent {
        override val description: String = "Untapped $permanentName"
    }

    @Serializable
    @SerialName("counterAdded")
    data class CounterAdded(
        val permanentId: EntityId,
        val permanentName: String,
        val counterType: String,
        val count: Int
    ) : ClientEvent {
        override val description: String = "Put $count $counterType counter(s) on $permanentName"
    }

    @Serializable
    @SerialName("counterRemoved")
    data class CounterRemoved(
        val permanentId: EntityId,
        val permanentName: String,
        val counterType: String,
        val count: Int
    ) : ClientEvent {
        override val description: String = "Removed $count $counterType counter(s) from $permanentName"
    }

    // =========================================================================
    // Mana Events
    // =========================================================================

    @Serializable
    @SerialName("manaAdded")
    data class ManaAdded(
        val playerId: EntityId,
        val manaString: String  // e.g., "{G}{G}" or "{R}"
    ) : ClientEvent {
        override val description: String = "Added $manaString"
    }

    // =========================================================================
    // Game State Events
    // =========================================================================

    @Serializable
    @SerialName("playerLost")
    data class PlayerLost(
        val playerId: EntityId,
        val reason: String
    ) : ClientEvent {
        override val description: String = "Player lost: $reason"
    }

    @Serializable
    @SerialName("gameEnded")
    data class GameEnded(
        val winnerId: EntityId?
    ) : ClientEvent {
        override val description: String = if (winnerId != null) {
            "Game ended - player won"
        } else {
            "Game ended in a draw"
        }
    }
}

/**
 * Transforms internal GameActionEvents into client-friendly ClientEvents.
 */
object ClientEventTransformer {

    /**
     * Transform internal events to client events.
     *
     * @param events The internal events
     * @param viewingPlayerId The player viewing these events (for masking)
     * @return Client-friendly events
     */
    fun transform(
        events: List<GameActionEvent>,
        viewingPlayerId: EntityId
    ): List<ClientEvent> {
        return events.mapNotNull { event -> transformEvent(event, viewingPlayerId) }
    }

    private fun transformEvent(
        event: GameActionEvent,
        viewingPlayerId: EntityId
    ): ClientEvent? {
        return when (event) {
            is GameActionEvent.LifeChanged -> ClientEvent.LifeChanged(
                playerId = event.playerId,
                oldLife = event.oldLife,
                newLife = event.newLife,
                change = event.newLife - event.oldLife
            )

            is GameActionEvent.DamageDealtToPlayer -> ClientEvent.DamageDealt(
                sourceId = event.sourceId,
                sourceName = null, // Would need state lookup for source name
                targetId = event.targetId,
                targetName = "Player",
                amount = event.amount,
                targetIsPlayer = true,
                isCombatDamage = false
            )

            is GameActionEvent.DamageDealtToCreature -> ClientEvent.DamageDealt(
                sourceId = event.sourceId,
                sourceName = null,
                targetId = event.targetId,
                targetName = "Creature", // Would need state lookup
                amount = event.amount,
                targetIsPlayer = false,
                isCombatDamage = false
            )

            is GameActionEvent.CardDrawn -> {
                // Mask card name if it's opponent's draw
                val showName = event.playerId == viewingPlayerId
                ClientEvent.CardDrawn(
                    playerId = event.playerId,
                    cardId = event.cardId,
                    cardName = if (showName) event.cardName else null
                )
            }

            is GameActionEvent.CardDiscarded -> ClientEvent.CardDiscarded(
                playerId = event.playerId,
                cardId = event.cardId,
                cardName = event.cardName
            )

            is GameActionEvent.PermanentEnteredBattlefield -> ClientEvent.PermanentEntered(
                cardId = event.entityId,
                cardName = event.name,
                controllerId = event.controllerId,
                enteredTapped = false // Would need to check state
            )

            is GameActionEvent.CreatureDied -> ClientEvent.CreatureDied(
                creatureId = event.entityId,
                creatureName = event.name
            )

            is GameActionEvent.AttackerDeclared -> ClientEvent.CreatureAttacked(
                creatureId = event.creatureId,
                creatureName = event.name,
                attackingPlayerId = EntityId.of("unknown"), // Would need state
                defendingPlayerId = EntityId.of("unknown")
            )

            is GameActionEvent.BlockerDeclared -> ClientEvent.CreatureBlocked(
                blockerId = event.blockerId,
                blockerName = event.name,
                attackerId = event.attackerId,
                attackerName = "Attacker" // Would need state lookup
            )

            is GameActionEvent.SpellCast -> ClientEvent.SpellCast(
                spellId = event.entityId,
                spellName = event.name,
                casterId = event.casterId
            )

            is GameActionEvent.SpellResolved -> ClientEvent.SpellResolved(
                spellId = event.entityId,
                spellName = event.name
            )

            is GameActionEvent.SpellFizzled -> ClientEvent.SpellCountered(
                spellId = event.entityId,
                spellName = event.name
            )

            is GameActionEvent.PermanentTapped -> ClientEvent.PermanentTapped(
                permanentId = event.entityId,
                permanentName = event.name
            )

            is GameActionEvent.PermanentUntapped -> ClientEvent.PermanentUntapped(
                permanentId = event.entityId,
                permanentName = event.name
            )

            is GameActionEvent.CounterAdded -> ClientEvent.CounterAdded(
                permanentId = event.entityId,
                permanentName = "", // Would need state lookup
                counterType = event.counterType,
                count = event.count
            )

            is GameActionEvent.CounterRemoved -> ClientEvent.CounterRemoved(
                permanentId = event.entityId,
                permanentName = "", // Would need state lookup
                counterType = event.counterType,
                count = event.count
            )

            is GameActionEvent.ManaAdded -> ClientEvent.ManaAdded(
                playerId = event.playerId,
                manaString = "{${event.color}}"
            )

            is GameActionEvent.PlayerLost -> ClientEvent.PlayerLost(
                playerId = event.playerId,
                reason = event.reason
            )

            is GameActionEvent.GameEnded -> ClientEvent.GameEnded(
                winnerId = event.winnerId
            )

            // Events that don't need client representation
            is GameActionEvent.DrawFailed,
            is GameActionEvent.CardMoved,
            is GameActionEvent.PermanentDestroyed,
            is GameActionEvent.LandPlayed,
            is GameActionEvent.Attached,
            is GameActionEvent.Detached,
            is GameActionEvent.CombatStarted,
            is GameActionEvent.CombatEnded,
            is GameActionEvent.BlockersOrdered,
            is GameActionEvent.AbilityResolved,
            is GameActionEvent.AbilityFizzled,
            is GameActionEvent.DecisionSubmitted,
            is GameActionEvent.DamageDealt,
            is GameActionEvent.CardExiled,
            is GameActionEvent.CardReturnedToHand -> null
        }
    }
}
