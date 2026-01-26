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
    // Secondary index: "CardName#CollectorNumber" -> CardDefinition (for variants like basic lands)
    private val cardsByNameAndNumber = mutableMapOf<String, CardDefinition>()

    /**
     * Register a single card definition.
     */
    fun register(card: CardDefinition) {
        cardsByName[card.name] = card
        // Also register by name#collectorNumber for variants
        val collectorNumber = card.metadata.collectorNumber
        if (collectorNumber != null) {
            cardsByNameAndNumber["${card.name}#$collectorNumber"] = card
        }
    }

    /**
     * Register multiple card definitions.
     */
    fun register(cards: Iterable<CardDefinition>) {
        cards.forEach { register(it) }
    }

    /**
     * Look up a card by name or by name#collectorNumber.
     *
     * Supports two formats:
     * - "Lightning Bolt" - returns the card by name
     * - "Plains#196" - returns the specific variant by collector number
     *
     * @param name The card name or name#collectorNumber (case-sensitive)
     * @return The card definition, or null if not found
     */
    fun getCard(name: String): CardDefinition? {
        // First try exact match with collector number format
        cardsByNameAndNumber[name]?.let { return it }
        // Fall back to name-only lookup
        return cardsByName[name]
    }

    /**
     * Look up a card by name, throwing if not found.
     *
     * @param name The card name or name#collectorNumber (case-sensitive)
     * @return The card definition
     * @throws IllegalArgumentException if the card is not found
     */
    fun requireCard(name: String): CardDefinition {
        return getCard(name)
            ?: throw IllegalArgumentException("Card not found in registry: $name")
    }

    /**
     * Check if a card is registered.
     */
    fun hasCard(name: String): Boolean = getCard(name) != null

    /**
     * Get all registered card names (unique names only, not variants).
     */
    fun allCardNames(): Set<String> = cardsByName.keys.toSet()

    /**
     * Get all cards with a given name (useful for basic lands with multiple variants).
     *
     * @param name The card name (e.g., "Plains")
     * @return All cards with that name, including variants
     */
    fun getCardsByName(name: String): List<CardDefinition> {
        return cardsByNameAndNumber.entries
            .filter { (key, _) -> key.startsWith("$name#") }
            .map { it.value }
            .ifEmpty { listOfNotNull(cardsByName[name]) }
    }

    /**
     * Get the total number of registered unique card names.
     */
    val size: Int get() = cardsByName.size

    /**
     * Clear all registered cards.
     */
    fun clear() {
        cardsByName.clear()
        cardsByNameAndNumber.clear()
    }
}
