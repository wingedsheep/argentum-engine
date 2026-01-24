package com.wingedsheep.sdk.model

import kotlinx.serialization.Serializable

/**
 * Represents a deck of Magic cards for game initialization.
 *
 * A deck is a list of card definition IDs (typically card names) that will be
 * instantiated into entity IDs when the game starts.
 *
 * ## Usage
 * ```kotlin
 * val deck = Deck(
 *     cards = listOf("Mountain", "Mountain", "Lightning Bolt", "Goblin Guide", ...)
 * )
 * ```
 *
 * The engine uses the [CardDefinition.name] to look up card definitions from
 * registered card sets.
 */
@Serializable
data class Deck(
    /**
     * Card names in the deck.
     * Duplicates are allowed (e.g., 4x Lightning Bolt).
     */
    val cards: List<String>
) {
    /**
     * Number of cards in the deck.
     */
    val size: Int get() = cards.size

    /**
     * Check if the deck is empty.
     */
    val isEmpty: Boolean get() = cards.isEmpty()

    /**
     * Count occurrences of a specific card.
     */
    fun countOf(cardName: String): Int = cards.count { it == cardName }

    /**
     * Get unique card names in the deck.
     */
    fun uniqueCards(): Set<String> = cards.toSet()

    companion object {
        /**
         * Create an empty deck.
         */
        val EMPTY = Deck(emptyList())

        /**
         * Create a deck from card name/count pairs.
         */
        fun of(vararg entries: Pair<String, Int>): Deck {
            val cards = entries.flatMap { (name, count) ->
                List(count) { name }
            }
            return Deck(cards)
        }

        /**
         * Create a simple test deck with basic lands and vanilla creatures.
         */
        fun testDeck(landName: String, landCount: Int, creatures: List<Pair<String, Int>>): Deck {
            val cards = mutableListOf<String>()
            repeat(landCount) { cards.add(landName) }
            creatures.forEach { (name, count) ->
                repeat(count) { cards.add(name) }
            }
            return Deck(cards)
        }
    }
}
