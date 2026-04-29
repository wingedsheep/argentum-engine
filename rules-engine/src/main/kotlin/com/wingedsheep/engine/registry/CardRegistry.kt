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
    // Reverse DFC index: back face name -> front face name
    private val backFaceToFrontFace = mutableMapOf<String, String>()

    /**
     * Register a single card definition.
     *
     * Double-faced cards (where `card.isDoubleFaced`) are also registered under the back face's
     * name so the back face is resolvable by name (e.g., after a transform). The back face entry
     * carries no further `backFace` pointer — it stands alone as the back-face identity.
     */
    fun register(card: CardDefinition) {
        cardsByName[card.name] = card
        // Also register by name#collectorNumber for variants.
        // When setCode is present, use "Name#SetCode-CollectorNumber" to avoid collisions
        // between sets that share collector numbers (e.g., Khans and Dominaria both use 250-269).
        val collectorNumber = card.metadata.collectorNumber
        if (collectorNumber != null) {
            val key = if (card.setCode != null) {
                "${card.name}#${card.setCode}-$collectorNumber"
            } else {
                "${card.name}#$collectorNumber"
            }
            cardsByNameAndNumber[key] = card
        }

        // Auto-register the back face as a standalone entry so lookups by its name succeed.
        // The TransformEffectExecutor resolves face definitions via this registry.
        card.backFace?.let { backFace ->
            // Don't clobber an existing registration (e.g., if someone already registered the back face).
            if (!cardsByName.containsKey(backFace.name)) {
                cardsByName[backFace.name] = backFace
            }
            // Track the reverse mapping so scenario builders can find the front face.
            backFaceToFrontFace[backFace.name] = card.name
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
     * Look up the front face of a DFC given its back face name.
     * Returns null if the name is not a registered back face.
     */
    fun getFrontFace(backFaceName: String): CardDefinition? {
        return backFaceToFrontFace[backFaceName]?.let { cardsByName[it] }
    }

    /**
     * Clear all registered cards.
     */
    fun clear() {
        cardsByName.clear()
        cardsByNameAndNumber.clear()
        backFaceToFrontFace.clear()
    }
}
