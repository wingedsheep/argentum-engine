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
        val change: Int,
        val isYours: Boolean? = null,
        override val description: String = when {
            isYours == true && change > 0 -> "You gained $change life"
            isYours == true && change < 0 -> "You lost ${-change} life"
            isYours == false && change > 0 -> "Opponent gained $change life"
            isYours == false && change < 0 -> "Opponent lost ${-change} life"
            change > 0 -> "Player gained $change life"
            else -> "Player lost ${-change} life"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("damageDealt")
    data class DamageDealt(
        val sourceId: EntityId?,
        val sourceName: String?,
        val targetId: EntityId,
        val targetName: String,
        val amount: Int,
        val targetIsPlayer: Boolean,
        val isCombatDamage: Boolean,
        override val description: String = "${sourceName?.let { "$it dealt " } ?: "Dealt "}$amount damage to $targetName"
    ) : ClientEvent

    @Serializable
    @SerialName("statsModified")
    data class StatsModified(
        val targetId: EntityId,
        val targetName: String,
        val powerChange: Int,
        val toughnessChange: Int,
        val sourceName: String,
        override val description: String = "$sourceName gave $targetName ${if (powerChange >= 0) "+" else ""}$powerChange/${if (toughnessChange >= 0) "+" else ""}$toughnessChange"
    ) : ClientEvent

    // =========================================================================
    // Card Movement Events
    // =========================================================================

    @Serializable
    @SerialName("cardDrawn")
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String?,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> if (cardName != null) "You drew $cardName" else "You drew a card"
            false -> "Opponent drew a card"
            null -> if (cardName != null) "Drew $cardName" else "Drew a card"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("cardDiscarded")
    data class CardDiscarded(
        val playerId: EntityId,
        val cardId: EntityId,
        val cardName: String,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "You discarded $cardName"
            false -> "Opponent discarded $cardName"
            null -> "Discarded $cardName"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("permanentEntered")
    data class PermanentEntered(
        val cardId: EntityId,
        val cardName: String,
        val controllerId: EntityId,
        val enteredTapped: Boolean,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "Your $cardName entered the battlefield${if (enteredTapped) " tapped" else ""}"
            false -> "Opponent's $cardName entered the battlefield${if (enteredTapped) " tapped" else ""}"
            null -> "$cardName entered the battlefield${if (enteredTapped) " tapped" else ""}"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("permanentLeft")
    data class PermanentLeft(
        val cardId: EntityId,
        val cardName: String,
        val destination: String,
        val ownerId: EntityId? = null,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "Your $cardName went to $destination"
            false -> "Opponent's $cardName went to $destination"
            null -> "$cardName went to $destination"
        }
    ) : ClientEvent

    // =========================================================================
    // Combat Events
    // =========================================================================

    @Serializable
    @SerialName("creatureAttacked")
    data class CreatureAttacked(
        val creatureId: EntityId,
        val creatureName: String,
        val attackingPlayerId: EntityId,
        val defendingPlayerId: EntityId,
        override val description: String = "$creatureName attacked"
    ) : ClientEvent

    @Serializable
    @SerialName("creatureBlocked")
    data class CreatureBlocked(
        val blockerId: EntityId,
        val blockerName: String,
        val attackerId: EntityId,
        val attackerName: String,
        override val description: String = "$blockerName blocked $attackerName"
    ) : ClientEvent

    @Serializable
    @SerialName("creatureDied")
    data class CreatureDied(
        val creatureId: EntityId,
        val creatureName: String,
        val controllerId: EntityId? = null,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "Your $creatureName died"
            false -> "Opponent's $creatureName died"
            null -> "$creatureName died"
        }
    ) : ClientEvent

    // =========================================================================
    // Spell/Ability Events
    // =========================================================================

    @Serializable
    @SerialName("spellCast")
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "You cast $spellName"
            false -> "Opponent cast $spellName"
            null -> "Cast $spellName"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("spellResolved")
    data class SpellResolved(
        val spellId: EntityId,
        val spellName: String,
        override val description: String = "$spellName resolved"
    ) : ClientEvent

    @Serializable
    @SerialName("spellCountered")
    data class SpellCountered(
        val spellId: EntityId,
        val spellName: String,
        override val description: String = "$spellName was countered"
    ) : ClientEvent

    @Serializable
    @SerialName("abilityTriggered")
    data class AbilityTriggered(
        val sourceId: EntityId,
        val sourceName: String,
        val abilityDescription: String,
        val controllerId: EntityId? = null,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "Your $sourceName triggered: $abilityDescription"
            false -> "Opponent's $sourceName triggered: $abilityDescription"
            null -> "$sourceName triggered: $abilityDescription"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("abilityActivated")
    data class AbilityActivated(
        val sourceId: EntityId,
        val sourceName: String,
        val abilityDescription: String,
        val controllerId: EntityId? = null,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "You activated $sourceName"
            false -> "Opponent activated $sourceName"
            null -> "Activated $sourceName"
        }
    ) : ClientEvent

    // =========================================================================
    // State Change Events
    // =========================================================================

    @Serializable
    @SerialName("permanentTapped")
    data class PermanentTapped(
        val permanentId: EntityId,
        val permanentName: String,
        override val description: String = "Tapped $permanentName"
    ) : ClientEvent

    @Serializable
    @SerialName("permanentUntapped")
    data class PermanentUntapped(
        val permanentId: EntityId,
        val permanentName: String,
        override val description: String = "Untapped $permanentName"
    ) : ClientEvent

    @Serializable
    @SerialName("counterAdded")
    data class CounterAdded(
        val permanentId: EntityId,
        val permanentName: String,
        val counterType: String,
        val count: Int,
        override val description: String = "Put $count $counterType counter(s) on $permanentName"
    ) : ClientEvent

    @Serializable
    @SerialName("counterRemoved")
    data class CounterRemoved(
        val permanentId: EntityId,
        val permanentName: String,
        val counterType: String,
        val count: Int,
        override val description: String = "Removed $count $counterType counter(s) from $permanentName"
    ) : ClientEvent

    // =========================================================================
    // Mana Events
    // =========================================================================

    @Serializable
    @SerialName("manaAdded")
    data class ManaAdded(
        val playerId: EntityId,
        val manaString: String,
        override val description: String = "Added $manaString"
    ) : ClientEvent

    // =========================================================================
    // Game State Events
    // =========================================================================

    @Serializable
    @SerialName("playerLost")
    data class PlayerLost(
        val playerId: EntityId,
        val reason: String,
        override val description: String = "Player lost: $reason"
    ) : ClientEvent

    @Serializable
    @SerialName("gameEnded")
    data class GameEnded(
        val winnerId: EntityId?,
        override val description: String = if (winnerId != null) "Game ended - player won" else "Game ended in a draw"
    ) : ClientEvent

    // =========================================================================
    // Information Events
    // =========================================================================

    @Serializable
    @SerialName("handLookedAt")
    data class HandLookedAt(
        val viewingPlayerId: EntityId,
        val targetPlayerId: EntityId,
        val cardIds: List<EntityId>,
        override val description: String = "Looked at opponent's hand (${cardIds.size} cards)"
    ) : ClientEvent

    @Serializable
    @SerialName("handRevealed")
    data class HandRevealed(
        val revealingPlayerId: EntityId,
        val cardIds: List<EntityId>,
        override val description: String = "Hand revealed (${cardIds.size} cards)"
    ) : ClientEvent

    @Serializable
    @SerialName("cardsRevealed")
    data class CardsRevealed(
        val revealingPlayerId: EntityId,
        val cardIds: List<EntityId>,
        val cardNames: List<String>,
        val source: String? = null,
        override val description: String = "Revealed ${cardNames.joinToString(", ")}${source?.let { " ($it)" } ?: ""}"
    ) : ClientEvent

    // =========================================================================
    // Player Choice Events
    // =========================================================================

    @Serializable
    @SerialName("scryCompleted")
    data class ScryCompleted(
        val playerId: EntityId,
        val cardsOnTop: Int,
        val cardsOnBottom: Int,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "You scried: put $cardsOnTop on top, $cardsOnBottom on bottom"
            false -> "Opponent scried: put $cardsOnTop on top, $cardsOnBottom on bottom"
            null -> "Scried: put $cardsOnTop on top, $cardsOnBottom on bottom"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("permanentsSacrificed")
    data class PermanentsSacrificed(
        val playerId: EntityId,
        val permanentNames: List<String>,
        val isYours: Boolean? = null,
        override val description: String = when (isYours) {
            true -> "You sacrificed ${permanentNames.joinToString(", ")}"
            false -> "Opponent sacrificed ${permanentNames.joinToString(", ")}"
            null -> "Sacrificed ${permanentNames.joinToString(", ")}"
        }
    ) : ClientEvent

    @Serializable
    @SerialName("libraryReordered")
    data class LibraryReordered(
        val playerId: EntityId,
        val cardCount: Int,
        val source: String? = null,
        override val description: String = "Reordered top $cardCount cards${source?.let { " ($it)" } ?: ""}"
    ) : ClientEvent
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
                change = event.newLife - event.oldLife,
                isYours = event.playerId == viewingPlayerId
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
                val isYours = event.playerId == viewingPlayerId
                if (event.cardIds.isNotEmpty()) {
                    ClientEvent.CardDrawn(
                        playerId = event.playerId,
                        cardId = event.cardIds.first(),
                        cardName = if (isYours) "Card" else null, // Would need state lookup
                        isYours = isYours
                    )
                } else null
            }

            is CardsDiscardedEvent -> {
                if (event.cardIds.isNotEmpty()) {
                    ClientEvent.CardDiscarded(
                        playerId = event.playerId,
                        cardId = event.cardIds.first(),
                        cardName = "Card", // Would need state lookup
                        isYours = event.playerId == viewingPlayerId
                    )
                } else null
            }

            is ZoneChangeEvent -> {
                val isYours = event.ownerId == viewingPlayerId
                when (event.toZone) {
                    com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD -> ClientEvent.PermanentEntered(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        controllerId = event.ownerId,
                        enteredTapped = false, // Would need to check state
                        isYours = isYours
                    )
                    com.wingedsheep.sdk.core.ZoneType.GRAVEYARD -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "graveyard",
                        ownerId = event.ownerId,
                        isYours = isYours
                    )
                    com.wingedsheep.sdk.core.ZoneType.EXILE -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "exile",
                        ownerId = event.ownerId,
                        isYours = isYours
                    )
                    com.wingedsheep.sdk.core.ZoneType.HAND -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "hand",
                        ownerId = event.ownerId,
                        isYours = isYours
                    )
                    com.wingedsheep.sdk.core.ZoneType.LIBRARY -> ClientEvent.PermanentLeft(
                        cardId = event.entityId,
                        cardName = event.entityName,
                        destination = "library",
                        ownerId = event.ownerId,
                        isYours = isYours
                    )
                    else -> null
                }
            }

            is CreatureDestroyedEvent -> ClientEvent.CreatureDied(
                creatureId = event.entityId,
                creatureName = event.name,
                controllerId = event.controllerId,
                isYours = event.controllerId?.let { it == viewingPlayerId }
            )

            is SpellCastEvent -> ClientEvent.SpellCast(
                spellId = event.spellEntityId,
                spellName = event.cardName,
                casterId = event.casterId,
                isYours = event.casterId == viewingPlayerId
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
                abilityDescription = event.description,
                controllerId = event.controllerId,
                isYours = event.controllerId == viewingPlayerId
            )

            is AbilityActivatedEvent -> ClientEvent.AbilityActivated(
                sourceId = event.sourceId,
                sourceName = event.sourceName,
                abilityDescription = "", // Description not available
                controllerId = event.controllerId,
                isYours = event.controllerId == viewingPlayerId
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

            is StatsModifiedEvent -> ClientEvent.StatsModified(
                targetId = event.targetId,
                targetName = event.targetName,
                powerChange = event.powerChange,
                toughnessChange = event.toughnessChange,
                sourceName = event.sourceName
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

            is HandLookedAtEvent -> {
                // Only send this event to the player who looked at the hand
                if (event.viewingPlayerId == viewingPlayerId) {
                    ClientEvent.HandLookedAt(
                        viewingPlayerId = event.viewingPlayerId,
                        targetPlayerId = event.targetPlayerId,
                        cardIds = event.cardIds
                    )
                } else {
                    null  // Don't reveal to other players that their hand was looked at
                }
            }

            is HandRevealedEvent -> {
                // Public reveal - all players see this event
                ClientEvent.HandRevealed(
                    revealingPlayerId = event.revealingPlayerId,
                    cardIds = event.cardIds
                )
            }

            is CardsRevealedEvent -> {
                // Public reveal - all players see this event
                ClientEvent.CardsRevealed(
                    revealingPlayerId = event.revealingPlayerId,
                    cardIds = event.cardIds,
                    cardNames = event.cardNames,
                    source = event.source
                )
            }

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
            is DiscardRequiredEvent,
            is ScryCompletedEvent,
            is PermanentsSacrificedEvent,
            is LookedAtCardsEvent,
            is LibraryReorderedEvent,
            is KeywordGrantedEvent -> null
        }
    }
}
