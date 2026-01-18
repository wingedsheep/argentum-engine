package com.wingedsheep.rulesengine.player

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.zone.Zone
import kotlinx.serialization.Serializable

/**
 * Legacy player representation.
 *
 * @deprecated Use ECS components instead:
 * - PlayerComponent for identity
 * - LifeComponent for life total
 * - ManaPoolComponent for mana
 * - PoisonComponent for poison counters
 * - LandsPlayedComponent for land tracking
 * - LostGameComponent/WonGameComponent for game end state
 *
 * Player zones are stored in EcsGameState:
 * - state.getLibrary(playerId)
 * - state.getHand(playerId)
 * - state.getGraveyard(playerId)
 *
 * @see com.wingedsheep.rulesengine.ecs.EcsGameState
 * @see com.wingedsheep.rulesengine.ecs.components.PlayerComponent
 */
@Deprecated(
    message = "Use ECS components (PlayerComponent, LifeComponent, etc.) and EcsGameState instead",
    replaceWith = ReplaceWith("EcsGameState.newGame(listOf(playerId to name))")
)
@Serializable
data class Player(
    val id: PlayerId,
    val name: String,
    val life: Int = STARTING_LIFE,
    val manaPool: ManaPool = ManaPool.EMPTY,
    val poisonCounters: Int = 0,
    val library: Zone,
    val hand: Zone,
    val graveyard: Zone,
    val hasLost: Boolean = false,
    val hasWon: Boolean = false,
    val landsPlayedThisTurn: Int = 0,
    val maxLandsPerTurn: Int = 1
) {
    val isAlive: Boolean
        get() = !hasLost && !hasWon

    val canPlayLand: Boolean
        get() = landsPlayedThisTurn < maxLandsPerTurn

    val librarySize: Int
        get() = library.size

    val handSize: Int
        get() = hand.size

    val graveyardSize: Int
        get() = graveyard.size

    fun gainLife(amount: Int): Player {
        require(amount >= 0) { "Cannot gain negative life" }
        return copy(life = life + amount)
    }

    fun loseLife(amount: Int): Player {
        require(amount >= 0) { "Cannot lose negative life" }
        return copy(life = life - amount)
    }

    fun setLife(amount: Int): Player = copy(life = amount)

    fun dealDamage(amount: Int): Player {
        require(amount >= 0) { "Cannot deal negative damage" }
        return copy(life = life - amount)
    }

    fun addMana(color: Color, amount: Int = 1): Player =
        copy(manaPool = manaPool.add(color, amount))

    fun addColorlessMana(amount: Int = 1): Player =
        copy(manaPool = manaPool.addColorless(amount))

    fun spendMana(color: Color, amount: Int = 1): Player =
        copy(manaPool = manaPool.spend(color, amount))

    fun emptyManaPool(): Player =
        copy(manaPool = ManaPool.EMPTY)

    fun addPoisonCounters(amount: Int): Player =
        copy(poisonCounters = poisonCounters + amount)

    fun recordLandPlayed(): Player =
        copy(landsPlayedThisTurn = landsPlayedThisTurn + 1)

    fun resetLandsPlayed(): Player =
        copy(landsPlayedThisTurn = 0)

    fun markAsLost(): Player =
        copy(hasLost = true)

    fun markAsWon(): Player =
        copy(hasWon = true)

    fun updateLibrary(transform: (Zone) -> Zone): Player =
        copy(library = transform(library))

    fun updateHand(transform: (Zone) -> Zone): Player =
        copy(hand = transform(hand))

    fun updateGraveyard(transform: (Zone) -> Zone): Player =
        copy(graveyard = transform(graveyard))

    companion object {
        const val STARTING_LIFE = 20
        const val LETHAL_POISON = 10

        fun create(id: PlayerId, name: String): Player = Player(
            id = id,
            name = name,
            library = Zone.library(id.value),
            hand = Zone.hand(id.value),
            graveyard = Zone.graveyard(id.value)
        )

        fun create(name: String): Player = create(PlayerId.generate(), name)
    }
}
