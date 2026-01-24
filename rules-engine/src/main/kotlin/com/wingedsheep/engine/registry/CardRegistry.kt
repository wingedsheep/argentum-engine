package com.wingedsheep.engine.registry

import com.wingedsheep.sdk.model.CardDefinition

/**
 * Registry for looking up card definitions by name.
 *
 * The CardRegistry is injected into the GameInitializer to resolve card names
 * from decks into CardDefinition objects.
 *
 * ## Usage
 * ```kotlin
 * val registry = CardRegistry()
 * registry.register(PortalSet.allCards)
 * val lightningBolt = registry.getCard("Lightning Bolt")
 * ```
 */
class CardRegistry {
    private val cardsByName = mutableMapOf<String, CardDefinition>()

    /**
     * Register a single card definition.
     */
    fun register(card: CardDefinition) {
        cardsByName[card.name] = card
    }

    /**
     * Register multiple card definitions.
     */
    fun register(cards: Iterable<CardDefinition>) {
        cards.forEach { register(it) }
    }

    /**
     * Look up a card by name.
     *
     * @param name The card name (case-sensitive)
     * @return The card definition, or null if not found
     */
    fun getCard(name: String): CardDefinition? = cardsByName[name]

    /**
     * Look up a card by name, throwing if not found.
     *
     * @param name The card name (case-sensitive)
     * @return The card definition
     * @throws IllegalArgumentException if the card is not found
     */
    fun requireCard(name: String): CardDefinition {
        return cardsByName[name]
            ?: throw IllegalArgumentException("Card not found in registry: $name")
    }

    /**
     * Check if a card is registered.
     */
    fun hasCard(name: String): Boolean = name in cardsByName

    /**
     * Get all registered card names.
     */
    fun allCardNames(): Set<String> = cardsByName.keys.toSet()

    /**
     * Get the total number of registered cards.
     */
    val size: Int get() = cardsByName.size

    /**
     * Clear all registered cards.
     */
    fun clear() {
        cardsByName.clear()
    }
}
