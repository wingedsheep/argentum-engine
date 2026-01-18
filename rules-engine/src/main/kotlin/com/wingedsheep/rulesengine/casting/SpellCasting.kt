package com.wingedsheep.rulesengine.casting

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId

/**
 * Determines whether a spell can be cast at the current time.
 */
object SpellTimingValidator {

    /**
     * Result of timing validation.
     */
    sealed interface TimingResult {
        data object Valid : TimingResult
        data class Invalid(val reason: String) : TimingResult
    }

    /**
     * Checks if a spell can be cast given the current game state.
     */
    fun canCast(state: GameState, card: CardInstance, playerId: PlayerId): TimingResult {
        // Player must have priority
        @Suppress("DEPRECATION")
        if (!state.turnState.isPriorityPlayer(playerId)) {
            return TimingResult.Invalid("You don't have priority")
        }

        // Game must not be over
        if (state.isGameOver) {
            return TimingResult.Invalid("Game is over")
        }

        return when {
            card.definition.isInstant -> canCastInstantSpeed(state)
            card.hasKeyword(Keyword.FLASH) -> canCastInstantSpeed(state)
            else -> canCastSorcerySpeed(state, playerId)
        }
    }

    /**
     * Checks if instant-speed spells can be cast.
     * Instants can be cast any time the player has priority.
     */
    private fun canCastInstantSpeed(state: GameState): TimingResult {
        // Current step must allow priority (not untap or cleanup)
        if (!state.currentStep.hasPriority) {
            return TimingResult.Invalid("Cannot cast spells during ${state.currentStep.displayName}")
        }
        return TimingResult.Valid
    }

    /**
     * Checks if sorcery-speed spells can be cast.
     * Sorceries can only be cast during main phase, with empty stack, by active player.
     */
    private fun canCastSorcerySpeed(state: GameState, playerId: PlayerId): TimingResult {
        // Must be active player
        @Suppress("DEPRECATION")
        if (!state.turnState.isActivePlayer(playerId)) {
            return TimingResult.Invalid("Only the active player can cast sorcery-speed spells")
        }

        // Must be main phase
        if (!state.turnState.isMainPhase) {
            return TimingResult.Invalid("Sorcery-speed spells can only be cast during main phase")
        }

        // Stack must be empty
        if (!state.stackIsEmpty) {
            return TimingResult.Invalid("Sorcery-speed spells can only be cast when the stack is empty")
        }

        return TimingResult.Valid
    }

    /**
     * Convenience method to check if a spell can be cast.
     */
    fun isValidTiming(state: GameState, card: CardInstance, playerId: PlayerId): Boolean =
        canCast(state, card, playerId) is TimingResult.Valid
}

/**
 * Validates mana payment for spells.
 */
object ManaPaymentValidator {

    /**
     * Result of mana payment validation.
     */
    sealed interface PaymentResult {
        data object Valid : PaymentResult
        data class Invalid(val reason: String) : PaymentResult
    }

    /**
     * Checks if a player can pay for a spell.
     */
    fun canPay(state: GameState, card: CardInstance, playerId: PlayerId): PaymentResult {
        val player = state.getPlayer(playerId)
        val cost = card.definition.manaCost

        if (!player.manaPool.canPay(cost)) {
            return PaymentResult.Invalid("Not enough mana to pay ${cost}")
        }

        return PaymentResult.Valid
    }

    /**
     * Convenience method to check if payment is valid.
     */
    fun isValidPayment(state: GameState, card: CardInstance, playerId: PlayerId): Boolean =
        canPay(state, card, playerId) is PaymentResult.Valid
}
