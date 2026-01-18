package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all game actions.
 * Actions are immutable descriptions of state changes.
 */
@Serializable
sealed interface Action {
    val description: String
}

// =============================================================================
// Life Actions
// =============================================================================

@Serializable
data class GainLife(
    val playerId: PlayerId,
    val amount: Int
) : Action {
    override val description: String = "${playerId.value} gains $amount life"
}

@Serializable
data class LoseLife(
    val playerId: PlayerId,
    val amount: Int
) : Action {
    override val description: String = "${playerId.value} loses $amount life"
}

@Serializable
data class SetLife(
    val playerId: PlayerId,
    val amount: Int
) : Action {
    override val description: String = "${playerId.value}'s life total becomes $amount"
}

@Serializable
data class DealDamage(
    val targetPlayerId: PlayerId,
    val amount: Int,
    val sourceCardId: CardId? = null
) : Action {
    override val description: String = "Deal $amount damage to ${targetPlayerId.value}"
}

// =============================================================================
// Mana Actions
// =============================================================================

@Serializable
data class AddMana(
    val playerId: PlayerId,
    val color: Color,
    val amount: Int = 1
) : Action {
    override val description: String = "${playerId.value} adds $amount ${color.displayName} mana"
}

@Serializable
data class AddColorlessMana(
    val playerId: PlayerId,
    val amount: Int = 1
) : Action {
    override val description: String = "${playerId.value} adds $amount colorless mana"
}

@Serializable
data class EmptyManaPool(
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value}'s mana pool empties"
}

// =============================================================================
// Card Drawing Actions
// =============================================================================

@Serializable
data class DrawCard(
    val playerId: PlayerId,
    val count: Int = 1
) : Action {
    override val description: String = "${playerId.value} draws $count card(s)"
}

@Serializable
data class DrawSpecificCard(
    val playerId: PlayerId,
    val cardId: CardId
) : Action {
    override val description: String = "${playerId.value} draws a specific card"
}

// =============================================================================
// Library Actions
// =============================================================================

@Serializable
data class ShuffleLibrary(
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} shuffles their library"
}

@Serializable
data class SearchLibrary(
    val playerId: PlayerId,
    val targetPlayerId: PlayerId = playerId
) : Action {
    override val description: String = "${playerId.value} searches ${targetPlayerId.value}'s library"
}

@Serializable
data class PutCardOnTopOfLibrary(
    val cardId: CardId,
    val ownerId: PlayerId
) : Action {
    override val description: String = "Put card on top of ${ownerId.value}'s library"
}

@Serializable
data class PutCardOnBottomOfLibrary(
    val cardId: CardId,
    val ownerId: PlayerId
) : Action {
    override val description: String = "Put card on bottom of ${ownerId.value}'s library"
}

// =============================================================================
// Card Movement Actions
// =============================================================================

@Serializable
data class MoveCard(
    val cardId: CardId,
    val fromZone: ZoneType,
    val toZone: ZoneType,
    val fromOwnerId: PlayerId? = null,
    val toOwnerId: PlayerId? = null,
    val toTop: Boolean = true
) : Action {
    override val description: String = "Move card from $fromZone to $toZone"
}

@Serializable
data class PutCardOntoBattlefield(
    val cardId: CardId,
    val controllerId: PlayerId,
    val tapped: Boolean = false
) : Action {
    override val description: String = "Put card onto the battlefield${if (tapped) " tapped" else ""}"
}

@Serializable
data class DestroyCard(
    val cardId: CardId
) : Action {
    override val description: String = "Destroy permanent"
}

@Serializable
data class SacrificeCard(
    val cardId: CardId,
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} sacrifices a permanent"
}

@Serializable
data class ExileCard(
    val cardId: CardId
) : Action {
    override val description: String = "Exile card"
}

@Serializable
data class ReturnToHand(
    val cardId: CardId,
    val ownerId: PlayerId
) : Action {
    override val description: String = "Return card to ${ownerId.value}'s hand"
}

@Serializable
data class DiscardCard(
    val cardId: CardId,
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} discards a card"
}

// =============================================================================
// Tap/Untap Actions
// =============================================================================

@Serializable
data class TapCard(
    val cardId: CardId
) : Action {
    override val description: String = "Tap permanent"
}

@Serializable
data class UntapCard(
    val cardId: CardId
) : Action {
    override val description: String = "Untap permanent"
}

@Serializable
data class UntapAllPermanents(
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} untaps all permanents they control"
}

// =============================================================================
// Combat Actions
// =============================================================================

@Serializable
data class DealCombatDamageToPlayer(
    val attackerId: CardId,
    val defendingPlayerId: PlayerId,
    val amount: Int
) : Action {
    override val description: String = "Deal $amount combat damage to ${defendingPlayerId.value}"
}

@Serializable
data class DealCombatDamageToCreature(
    val sourceId: CardId,
    val targetId: CardId,
    val amount: Int
) : Action {
    override val description: String = "Deal $amount combat damage to creature"
}

@Serializable
data class MarkDamageOnCreature(
    val cardId: CardId,
    val amount: Int
) : Action {
    override val description: String = "Mark $amount damage on creature"
}

@Serializable
data class ClearDamageFromCreature(
    val cardId: CardId
) : Action {
    override val description: String = "Clear damage from creature"
}

@Serializable
data class ClearAllDamage(
    val playerId: PlayerId? = null
) : Action {
    override val description: String = "Clear damage from all creatures"
}

// =============================================================================
// Counter Actions
// =============================================================================

@Serializable
data class AddCounters(
    val cardId: CardId,
    val counterType: String,
    val amount: Int = 1
) : Action {
    override val description: String = "Add $amount $counterType counter(s)"
}

@Serializable
data class RemoveCounters(
    val cardId: CardId,
    val counterType: String,
    val amount: Int = 1
) : Action {
    override val description: String = "Remove $amount $counterType counter(s)"
}

@Serializable
data class AddPoisonCounters(
    val playerId: PlayerId,
    val amount: Int
) : Action {
    override val description: String = "${playerId.value} gets $amount poison counter(s)"
}

// =============================================================================
// Summoning Sickness
// =============================================================================

@Serializable
data class RemoveSummoningSickness(
    val cardId: CardId
) : Action {
    override val description: String = "Remove summoning sickness"
}

@Serializable
data class RemoveSummoningSicknessFromAll(
    val playerId: PlayerId
) : Action {
    override val description: String = "Remove summoning sickness from all creatures ${playerId.value} controls"
}

// =============================================================================
// Land Actions
// =============================================================================

@Serializable
data class PlayLand(
    val cardId: CardId,
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} plays a land"
}

@Serializable
data class ResetLandsPlayed(
    val playerId: PlayerId
) : Action {
    override val description: String = "Reset lands played this turn"
}

// =============================================================================
// Game Flow Actions
// =============================================================================

@Serializable
data class EndGame(
    val winnerId: PlayerId?
) : Action {
    override val description: String = winnerId?.let { "Game ends. ${it.value} wins!" } ?: "Game ends in a draw"
}

@Serializable
data class PlayerLoses(
    val playerId: PlayerId,
    val reason: String
) : Action {
    override val description: String = "${playerId.value} loses the game: $reason"
}

// =============================================================================
// Casting and Stack Actions
// =============================================================================

@Serializable
data class CastSpell(
    val cardId: CardId,
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} casts a spell"
}

@Serializable
data class PayManaCost(
    val playerId: PlayerId,
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
    val generic: Int = 0
) : Action {
    override val description: String = "${playerId.value} pays mana"
}

@Serializable
data class ResolveTopOfStack(
    val playerId: PlayerId? = null
) : Action {
    override val description: String = "Resolve top of stack"
}

@Serializable
data class PassPriority(
    val playerId: PlayerId
) : Action {
    override val description: String = "${playerId.value} passes priority"
}

@Serializable
data class CounterSpell(
    val cardId: CardId
) : Action {
    override val description: String = "Counter spell"
}
