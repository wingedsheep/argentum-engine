package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * Stateful, deterministic dealer for a cube.
 *
 * The complete cube is shuffled once and then consumed in order. This is deliberately separate
 * from [com.wingedsheep.sdk.limited.BoosterStrategy], whose per-pack API cannot preserve
 * no-replacement state across a draft.
 */
class CubeDealer(
    cube: List<CardDefinition>,
    private val packSize: Int,
    seed: Long,
) {
    private constructor(
        remainingCards: List<CardDefinition>,
        packSize: Int,
    ) : this(emptyList(), packSize, 0L) {
        shuffledCube = remainingCards
    }

    init {
        require(packSize > 0) { "Cube pack size must be positive, got $packSize" }
    }

    private var shuffledCube = cube.shuffled(Random(seed))
    private var dealt = 0

    val remaining: Int
        get() = shuffledCube.size - dealt

    /** Ordered undealt tail, used only to persist and resume an in-flight cube draft safely. */
    fun remainingCards(): List<CardDefinition> = shuffledCube.subList(dealt, shuffledCube.size).toList()

    /**
     * Deal [packs] complete packs and consume them from this dealer.
     *
     * @throws IllegalArgumentException when [packs] is negative or the cube lacks enough cards.
     */
    fun deal(packs: Int): List<List<CardDefinition>> {
        require(packs >= 0) { "Number of cube packs must not be negative, got $packs" }
        val requestedCards = Math.multiplyExact(packs, packSize)
        require(requestedCards <= remaining) {
            val shortfall = requestedCards - remaining
            "Cannot deal $packs cube packs of $packSize cards: " +
                "$remaining cards remain, short by $shortfall"
        }

        val result = shuffledCube
            .subList(dealt, dealt + requestedCards)
            .chunked(packSize)
        dealt += requestedCards
        return result
    }

    companion object {
        fun resume(remainingCards: List<CardDefinition>, packSize: Int): CubeDealer =
            CubeDealer(remainingCards, packSize)
    }
}
