package com.wingedsheep.engine.state.components.player

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Mana pool for a player.
 */
@Serializable
data class ManaPoolComponent(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) : Component {
    /**
     * Add mana of a specific color.
     */
    fun add(color: Color, amount: Int = 1): ManaPoolComponent = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    /**
     * Add colorless mana.
     */
    fun addColorless(amount: Int): ManaPoolComponent =
        copy(colorless = colorless + amount)

    /**
     * Get mana of a specific color.
     */
    fun getAmount(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    /**
     * Remove mana of a specific color.
     */
    fun spend(color: Color, amount: Int = 1): ManaPoolComponent? {
        val current = getAmount(color)
        return if (current >= amount) {
            when (color) {
                Color.WHITE -> copy(white = white - amount)
                Color.BLUE -> copy(blue = blue - amount)
                Color.BLACK -> copy(black = black - amount)
                Color.RED -> copy(red = red - amount)
                Color.GREEN -> copy(green = green - amount)
            }
        } else null
    }

    /**
     * Spend colorless mana.
     */
    fun spendColorless(amount: Int): ManaPoolComponent? =
        if (colorless >= amount) copy(colorless = colorless - amount) else null

    /**
     * Total mana available.
     */
    val total: Int get() = white + blue + black + red + green + colorless

    /**
     * Check if pool is empty.
     */
    val isEmpty: Boolean get() = total == 0

    /**
     * Empty the mana pool.
     */
    fun empty(): ManaPoolComponent = ManaPoolComponent()
}

/**
 * Tracks land drops for the turn.
 */
@Serializable
data class LandDropsComponent(
    val remaining: Int = 1,
    val maxPerTurn: Int = 1
) : Component {
    /**
     * Use a land drop.
     */
    fun use(): LandDropsComponent = copy(remaining = remaining - 1)

    /**
     * Reset for a new turn.
     */
    fun reset(): LandDropsComponent = copy(remaining = maxPerTurn)

    /**
     * Check if a land can be played.
     */
    val canPlayLand: Boolean get() = remaining > 0
}

/**
 * Tracks mulligan state for a player during game setup.
 *
 * Per CR 103.4 (London Mulligan): A player may mulligan any number of times.
 * After taking a mulligan, the player shuffles their hand into their library
 * and draws a new hand of 7 cards. After all players have kept, each player
 * who took mulligans puts that many cards from their hand on the bottom of
 * their library in any order.
 */
@Serializable
data class MulliganStateComponent(
    /** Number of mulligans taken so far */
    val mulligansTaken: Int = 0,
    /** Whether the player has decided to keep their current hand */
    val hasKept: Boolean = false
) : Component {
    companion object {
        const val STARTING_HAND_SIZE = 7
    }

    /**
     * The number of cards this player must put on the bottom after keeping.
     */
    val cardsToBottom: Int get() = mulligansTaken

    /**
     * Check if the player can still mulligan (they can always mulligan until
     * they would have to bottom more cards than they drew).
     */
    val canMulligan: Boolean get() = mulligansTaken < STARTING_HAND_SIZE && !hasKept

    /**
     * Record a mulligan taken.
     */
    fun takeMulligan(): MulliganStateComponent = copy(mulligansTaken = mulligansTaken + 1)

    /**
     * Record that the player keeps their hand.
     */
    fun keep(): MulliganStateComponent = copy(hasKept = true)
}
