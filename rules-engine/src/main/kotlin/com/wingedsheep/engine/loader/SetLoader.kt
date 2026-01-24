package com.wingedsheep.engine.loader

import com.wingedsheep.sdk.model.CardDefinition
import java.util.ServiceLoader

/**
 * Loads card sets from the classpath using Java ServiceLoader.
 *
 * Card sets are discovered automatically if they:
 * 1. Implement CardSetProvider
 * 2. Are registered in META-INF/services/com.wingedsheep.engine.loader.CardSetProvider
 */
class SetLoader {

    private val loadedSets: MutableMap<String, CardSet> = mutableMapOf()
    private val cardIndex: MutableMap<String, CardDefinition> = mutableMapOf()

    /**
     * Load all available card sets from the classpath.
     */
    fun loadAll() {
        val providers = ServiceLoader.load(CardSetProvider::class.java)

        for (provider in providers) {
            val set = provider.getCardSet()
            loadedSets[set.code] = set

            // Index cards by ID
            for (card in set.cards) {
                val cardId = "${set.code.lowercase()}-${card.name.lowercase().replace(' ', '-')}"
                cardIndex[cardId] = card
            }
        }
    }

    /**
     * Get a card by its ID.
     */
    fun getCard(cardId: String): CardDefinition? = cardIndex[cardId]

    /**
     * Get a card by name (searches all sets).
     */
    fun getCardByName(name: String): CardDefinition? {
        return loadedSets.values
            .flatMap { it.cards }
            .find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Get all cards from a specific set.
     */
    fun getCardsInSet(setCode: String): List<CardDefinition> {
        return loadedSets[setCode]?.cards ?: emptyList()
    }

    /**
     * Get all loaded sets.
     */
    fun getAllSets(): List<CardSet> = loadedSets.values.toList()

    /**
     * Get all loaded cards.
     */
    fun getAllCards(): List<CardDefinition> = cardIndex.values.toList()

    /**
     * Check if a set is loaded.
     */
    fun hasSet(setCode: String): Boolean = loadedSets.containsKey(setCode)

    /**
     * Get the number of loaded cards.
     */
    val cardCount: Int get() = cardIndex.size

    /**
     * Get the number of loaded sets.
     */
    val setCount: Int get() = loadedSets.size

    /**
     * Manually register a card set (for testing).
     */
    fun registerSet(set: CardSet) {
        loadedSets[set.code] = set
        for (card in set.cards) {
            val cardId = "${set.code.lowercase()}-${card.name.lowercase().replace(' ', '-')}"
            cardIndex[cardId] = card
        }
    }

    /**
     * Clear all loaded sets.
     */
    fun clear() {
        loadedSets.clear()
        cardIndex.clear()
    }
}

/**
 * Interface for card set providers.
 * Implement this and register via ServiceLoader to make sets discoverable.
 */
interface CardSetProvider {
    fun getCardSet(): CardSet
}

/**
 * Represents a collection of cards (a set or expansion).
 */
data class CardSet(
    val code: String,
    val name: String,
    val cards: List<CardDefinition>
)
