package com.wingedsheep.gameserver.dto

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client-facing game events.
 *
 * These are simplified versions of internal GameEvents that:
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
 * Transforms internal GameEvents into client-friendly ClientEvents.
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
        events: List<GameEvent>,
        viewingPlayerId: EntityId
    ): List<ClientEvent> {
        return events.mapNotNull { event -> transformEvent(event, viewingPlayerId) }
    }

    private fun transformEvent(
        event: GameEvent,
        viewingPlayerId: EntityId
    ): ClientEvent? {
        return when (event) {
            is LifeChangedEvent -> ClientEvent.LifeChanged(
                playerId = event.playerId,
                oldLife = event.oldLife,
                newLife = event.newLife,
                change = event.newLife - event.oldLife
            )

            is DamageDealtEvent -> ClientEvent.DamageDealt(
                sourceId = event.sourceId,
                sourceName = null, // Would need state lookup for source name
                targetId = event.targetId,
                targetName = "Target", // Would need state lookup
                amount = event.amount,
                targetIsPlayer = false, // Would need state lookup
                isCombatDamage = event.isCombatDamage
            )

            is CardsDrawnEvent -> {
                // For now, just create one event for each card drawn
                // In future could batch them
                val showNames = event.playerId == viewingPlayerId
                if (event.cardIds.isNotEmpty()) {
                    ClientEvent.CardDrawn(
                        playerId = event.playerId,
                        cardId = event.cardIds.first(),
                        cardName = if (showNames) "Card" else null // Would need state lookup
                    )
                } else null
            }

            is CardsDiscardedEvent -> {
                if (event.cardIds.isNotEmpty()) {
                    ClientEvent.CardDiscarded(
                        playerId = event.playerId,
                        cardId = event.cardIds.first(),
                        cardName = "Card" // Would need state lookup
                    )
                } else null
            }

            is ZoneChangeEvent -> {
                when (event.toZone) {
                    com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD -> ClientEvent.PermanentEntered(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        controllerId = event.ownerId,
                        enteredTapped = false // Would need to check state
                    )
                    com.wingedsheep.sdk.core.ZoneType.GRAVEYARD -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "graveyard"
                    )
                    com.wingedsheep.sdk.core.ZoneType.EXILE -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "exile"
                    )
                    com.wingedsheep.sdk.core.ZoneType.HAND -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "hand"
                    )
                    com.wingedsheep.sdk.core.ZoneType.LIBRARY -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "library"
                    )
                    else -> null
                }
            }

            is CreatureDestroyedEvent -> ClientEvent.CreatureDied(
                creatureId = event.entityId,
                creatureName = event.name
            )

            is SpellCastEvent -> ClientEvent.SpellCast(
                spellId = event.spellEntityId,
                spellName = event.cardName,
                casterId = event.casterId
            )

            is ResolvedEvent -> ClientEvent.SpellResolved(
                spellId = event.entityId,
                spellName = event.name
            )

            is SpellCounteredEvent -> ClientEvent.SpellCountered(
                spellId = event.spellEntityId,
                spellName = event.cardName
            )

            is SpellFizzledEvent -> ClientEvent.SpellCountered(
                spellId = event.spellEntityId,
                spellName = event.cardName
            )

            is AbilityTriggeredEvent -> ClientEvent.AbilityTriggered(
                sourceId = event.sourceId,
                sourceName = event.sourceName,
                abilityDescription = event.description
            )

            is AbilityActivatedEvent -> ClientEvent.AbilityActivated(
                sourceId = event.sourceId,
                sourceName = event.sourceName,
                abilityDescription = "" // Description not available
            )

            is TappedEvent -> ClientEvent.PermanentTapped(
                permanentId = event.entityId,
                permanentName = event.entityName
            )

            is UntappedEvent -> ClientEvent.PermanentUntapped(
                permanentId = event.entityId,
                permanentName = event.entityName
            )

            is CountersAddedEvent -> ClientEvent.CounterAdded(
                permanentId = event.entityId,
                permanentName = "", // Would need state lookup
                counterType = event.counterType,
                count = event.amount
            )

            is CountersRemovedEvent -> ClientEvent.CounterRemoved(
                permanentId = event.entityId,
                permanentName = "", // Would need state lookup
                counterType = event.counterType,
                count = event.amount
            )

            is ManaAddedEvent -> {
                val parts = mutableListOf<String>()
                repeat(event.white) { parts.add("{W}") }
                repeat(event.blue) { parts.add("{U}") }
                repeat(event.black) { parts.add("{B}") }
                repeat(event.red) { parts.add("{R}") }
                repeat(event.green) { parts.add("{G}") }
                repeat(event.colorless) { parts.add("{C}") }
                ClientEvent.ManaAdded(
                    playerId = event.playerId,
                    manaString = parts.joinToString("")
                )
            }

            is PlayerLostEvent -> ClientEvent.PlayerLost(
                playerId = event.playerId,
                reason = event.reason.name
            )

            is GameEndedEvent -> ClientEvent.GameEnded(
                winnerId = event.winnerId
            )

            // Events that don't need client representation or are handled differently
            is DrawFailedEvent,
            is LibraryShuffledEvent,
            is AttackersDeclaredEvent,
            is BlockersDeclaredEvent,
            is BlockerOrderDeclaredEvent,
            is DamageAssignedEvent,
            is PhaseChangedEvent,
            is StepChangedEvent,
            is TurnChangedEvent,
            is PriorityChangedEvent,
            is ManaSpentEvent,
            is DecisionRequestedEvent,
            is DecisionSubmittedEvent,
            is AbilityResolvedEvent,
            is AbilityFizzledEvent,
            is DiscardRequiredEvent -> null
        }
    }
}
