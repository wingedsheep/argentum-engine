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

/**
 * Component tracking additional combat phases to be inserted into the current turn.
 * When the postcombat main phase would advance to the end step, if this component
 * exists, the game instead transitions to an additional combat phase followed by
 * a main phase. The count is decremented each time an additional combat phase begins.
 *
 * Applied by effects like Aggravated Assault.
 *
 * @param count Number of additional combat+main phase cycles remaining
 */
@Serializable
data class AdditionalCombatPhasesComponent(
    val count: Int = 1
) : Component

/**
 * Marker component indicating that a player should skip all combat phases
 * during their next turn. Applied by effects like False Peace.
 *
 * This component is consumed (removed) at the start of the combat phase
 * when the turn is skipped.
 */
@Serializable
data object SkipCombatPhasesComponent : Component

/**
 * Component indicating that a player's creatures and/or lands don't untap
 * during their next untap step. Applied by effects like Exhaustion.
 *
 * This component is consumed (removed) after the untap step is processed.
 *
 * @param affectsCreatures If true, creatures controlled by this player don't untap
 * @param affectsLands If true, lands controlled by this player don't untap
 */
@Serializable
data class SkipUntapComponent(
    val affectsCreatures: Boolean = true,
    val affectsLands: Boolean = true
) : Component

/**
 * Component marking that a player has lost the game.
 *
 * This is added when a player loses due to various game rules:
 * - 704.5a: Life total 0 or less
 * - 704.5b: 10 or more poison counters
 * - 704.5c: Attempted to draw from empty library
 * - Concession
 *
 * The checkGameEnd SBA uses this to determine when the game ends.
 */
@Serializable
data class PlayerLostComponent(
    val reason: LossReason
) : Component

/**
 * Reason why a player lost the game.
 */
@Serializable
enum class LossReason {
    LIFE_ZERO,
    POISON_COUNTERS,
    EMPTY_LIBRARY,
    CONCESSION,
    CARD_EFFECT
}

/**
 * Component indicating that a player has shroud.
 * Applied by spells like Gilded Light ("You gain shroud until end of turn").
 *
 * When present on a player entity, that player cannot be the target of
 * spells or abilities.
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup (e.g., Gilded Light)
 *   - [PlayerEffectRemoval.Permanent] — stays until explicitly removed
 */
@Serializable
data class PlayerShroudComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Describes when a player-level effect component should be removed.
 */
@Serializable
enum class PlayerEffectRemoval {
    /** Removed at end of turn during cleanup (e.g., Gilded Light) */
    EndOfTurn,
    /** Never removed automatically — must be explicitly cleared */
    Permanent
}

/**
 * Tracks the number of cards a player has drawn during the current turn.
 * Reset to 0 at the start of each turn (for ALL players, since "each turn"
 * means every turn, not just your own).
 *
 * Used by RevealFirstDrawEachTurn to determine when to emit reveal events.
 */
@Serializable
data class CardsDrawnThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Marker component indicating that a player should skip their entire next turn.
 * Applied by effects like Last Chance (which gives the opponent an "extra turn"
 * by skipping the other player's turn in a 2-player game).
 *
 * This component is consumed (removed) when the turn would start, and the turn
 * is skipped instead.
 */
@Serializable
data object SkipNextTurnComponent : Component

/**
 * Component indicating that a player will lose the game at the beginning of
 * a future end step. Applied by effects like Last Chance.
 *
 * @param turnsUntilLoss Number of end steps to skip before triggering.
 *   - 1 means lose at the end of the NEXT turn (not the current one)
 *   - 0 means lose at the end of the CURRENT turn
 * @param message Optional custom message to display when the player loses
 *
 * This component is consumed (removed) when it triggers and the player loses.
 */
@Serializable
data class LoseAtEndStepComponent(
    val turnsUntilLoss: Int = 1,
    val message: String? = null
) : Component
