package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.player.ManaPool
import kotlinx.serialization.Serializable

/**
 * Components for player-specific state.
 */

/**
 * Tracks a player's life total.
 *
 * Standard starting life is 20. Some formats use different values
 * (Commander uses 40, Two-Headed Giant uses 30, etc.)
 *
 * @property life Current life total (can be negative)
 */
@Serializable
data class LifeComponent(
    val life: Int = 20
) : Component {
    /**
     * Gain life.
     */
    fun gainLife(amount: Int): LifeComponent =
        copy(life = life + amount)

    /**
     * Lose life.
     */
    fun loseLife(amount: Int): LifeComponent =
        copy(life = life - amount)

    /**
     * Set life to a specific value.
     */
    fun setLife(newLife: Int): LifeComponent =
        copy(life = newLife)

    /**
     * Check if player would lose due to life total.
     */
    val isAtZeroOrLess: Boolean get() = life <= 0

    companion object {
        const val STARTING_LIFE = 20
        const val COMMANDER_STARTING_LIFE = 40

        fun starting(): LifeComponent = LifeComponent(STARTING_LIFE)
        fun commander(): LifeComponent = LifeComponent(COMMANDER_STARTING_LIFE)
    }
}

/**
 * Tracks a player's mana pool.
 *
 * Mana is added from lands and other sources, and spent to cast spells
 * and activate abilities. Mana empties at the end of each step/phase.
 *
 * @property pool The current mana pool
 */
@Serializable
data class ManaPoolComponent(
    val pool: ManaPool = ManaPool.EMPTY
) : Component {
    /**
     * Add colored mana to the pool.
     */
    fun add(color: Color, amount: Int = 1): ManaPoolComponent =
        copy(pool = pool.add(color, amount))

    /**
     * Add colorless mana to the pool.
     */
    fun addColorless(amount: Int = 1): ManaPoolComponent =
        copy(pool = pool.addColorless(amount))

    /**
     * Empty the mana pool.
     */
    fun empty(): ManaPoolComponent =
        copy(pool = ManaPool.EMPTY)

    /**
     * Check if pool is empty.
     */
    val isEmpty: Boolean get() = pool.isEmpty

    /**
     * Update the pool directly.
     */
    fun withPool(newPool: ManaPool): ManaPoolComponent =
        copy(pool = newPool)

    companion object {
        val EMPTY = ManaPoolComponent()
    }
}

/**
 * Tracks poison counters on a player.
 *
 * A player with 10 or more poison counters loses the game.
 *
 * @property counters Number of poison counters
 */
@Serializable
data class PoisonComponent(
    val counters: Int = 0
) : Component {
    init {
        require(counters >= 0) { "Poison counters cannot be negative: $counters" }
    }

    /**
     * Add poison counters.
     */
    fun add(amount: Int): PoisonComponent =
        copy(counters = counters + amount)

    /**
     * Remove poison counters (rarely happens, but some cards do this).
     */
    fun remove(amount: Int): PoisonComponent =
        copy(counters = (counters - amount).coerceAtLeast(0))

    /**
     * Check if player would lose due to poison.
     */
    val isLethal: Boolean get() = counters >= LETHAL_POISON

    companion object {
        const val LETHAL_POISON = 10
        val NONE = PoisonComponent(0)
    }
}

/**
 * Tracks lands played this turn.
 *
 * Players normally can play one land per turn, but effects can
 * modify this limit.
 *
 * @property count Lands played this turn
 * @property maximum Maximum lands allowed this turn (default 1)
 */
@Serializable
data class LandsPlayedComponent(
    val count: Int = 0,
    val maximum: Int = 1
) : Component {
    /**
     * Whether the player can play another land this turn.
     */
    val canPlayLand: Boolean get() = count < maximum

    /**
     * Record a land being played.
     */
    fun playLand(): LandsPlayedComponent =
        copy(count = count + 1)

    /**
     * Reset at start of turn.
     */
    fun reset(): LandsPlayedComponent =
        copy(count = 0)

    /**
     * Modify the maximum (e.g., Exploration adds +1).
     */
    fun modifyMaximum(delta: Int): LandsPlayedComponent =
        copy(maximum = (maximum + delta).coerceAtLeast(0))

    companion object {
        fun newTurn(): LandsPlayedComponent = LandsPlayedComponent()
    }
}

/**
 * Marks a player as having lost the game.
 *
 * @property reason The reason for the loss (for game log/display)
 */
@Serializable
data class LostGameComponent(
    val reason: String
) : Component {
    companion object {
        fun zeroLife(): LostGameComponent = LostGameComponent("Life total reached 0 or less")
        fun poison(): LostGameComponent = LostGameComponent("Received 10 or more poison counters")
        fun decked(): LostGameComponent = LostGameComponent("Attempted to draw from empty library")
        fun conceded(): LostGameComponent = LostGameComponent("Conceded")
    }
}

/**
 * Marks a player as having won the game.
 *
 * Presence of this component means the player has won.
 */
@Serializable
data object WonGameComponent : Component
