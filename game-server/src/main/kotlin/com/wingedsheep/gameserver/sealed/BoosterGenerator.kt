package com.wingedsheep.gameserver.sealed

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.mtg.sets.definitions.lorwyn.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet

/**
 * Generates booster packs and sealed pools from card sets.
 *
 * Standard booster composition:
 * - 11 Commons
 * - 3 Uncommons
 * - 1 Rare (with ~12.5% chance of Mythic if the set has mythics)
 */
class BoosterGenerator {

    /**
     * Configuration for a card set that can be used for sealed.
     */
    data class SetConfig(
        val setCode: String,
        val setName: String,
        val cards: List<CardDefinition>,
        val basicLands: List<CardDefinition>
    )

    companion object {
        /**
         * Available sets for sealed play.
         */
        val availableSets: Map<String, SetConfig> = mapOf(
            PortalSet.SET_CODE to SetConfig(
                setCode = PortalSet.SET_CODE,
                setName = PortalSet.SET_NAME,
                cards = PortalSet.allCards,
                basicLands = PortalSet.basicLands
            ),
            LorwynEclipsedSet.SET_CODE to SetConfig(
                setCode = LorwynEclipsedSet.SET_CODE,
                setName = LorwynEclipsedSet.SET_NAME,
                cards = LorwynEclipsedSet.allCards,
                basicLands = LorwynEclipsedSet.basicLands
            )
        )

        /**
         * Get set configuration by set code.
         */
        fun getSetConfig(setCode: String): SetConfig? = availableSets[setCode]
    }

    /**
     * Generate a single 15-card booster pack from the specified set.
     *
     * @param setCode The set code to generate from (e.g., "POR" for Portal)
     * @return List of 15 card definitions
     * @throws IllegalArgumentException if set code is not found
     */
    fun generateBooster(setCode: String): List<CardDefinition> {
        val setConfig = availableSets[setCode]
            ?: throw IllegalArgumentException("Unknown set code: $setCode")

        return generateBoosterFromCards(setConfig.cards)
    }

    /**
     * Generate a sealed pool of 90 cards (6 boosters) from the specified set.
     *
     * @param setCode The set code to generate from
     * @param boosterCount Number of boosters to open (default 6)
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if set code is not found
     */
    fun generateSealedPool(setCode: String, boosterCount: Int = 6): List<CardDefinition> {
        return (1..boosterCount).flatMap { generateBooster(setCode) }
    }

    /**
     * Get basic lands available for deck building from a set.
     *
     * @param setCode The set code
     * @return Map of land name to CardDefinition (one variant per type)
     */
    fun getBasicLands(setCode: String): Map<String, CardDefinition> {
        val setConfig = availableSets[setCode]
            ?: throw IllegalArgumentException("Unknown set code: $setCode")

        // Return one variant of each basic land type
        return setConfig.basicLands
            .groupBy { it.name }
            .mapValues { (_, variants) -> variants.first() }
    }

    private fun generateBoosterFromCards(allCards: List<CardDefinition>): List<CardDefinition> {
        // Filter out basic lands from the booster pool
        val boosterPool = allCards.filter { card ->
            !card.typeLine.isBasicLand
        }

        // Group cards by rarity
        val commons = boosterPool.filter { it.metadata.rarity == Rarity.COMMON }.toMutableList()
        val uncommons = boosterPool.filter { it.metadata.rarity == Rarity.UNCOMMON }.toMutableList()
        val rares = boosterPool.filter { it.metadata.rarity == Rarity.RARE }.toMutableList()
        val mythics = boosterPool.filter { it.metadata.rarity == Rarity.MYTHIC }.toMutableList()

        val booster = mutableListOf<CardDefinition>()
        val usedCardNames = mutableSetOf<String>()

        // Helper to pick a random card without duplicates
        fun pickWithoutDuplicates(pool: MutableList<CardDefinition>): CardDefinition? {
            val available = pool.filter { it.name !in usedCardNames }
            if (available.isEmpty()) return null
            val picked = available.random()
            usedCardNames.add(picked.name)
            return picked
        }

        // 11 Commons (without duplicates within the same booster)
        repeat(11) {
            pickWithoutDuplicates(commons)?.let { booster.add(it) }
        }

        // 3 Uncommons (without duplicates within the same booster)
        repeat(3) {
            pickWithoutDuplicates(uncommons)?.let { booster.add(it) }
        }

        // 1 Rare (or Mythic with ~12.5% chance if mythics exist)
        val rareSlot = if (mythics.isNotEmpty() && Math.random() < 0.125) {
            pickWithoutDuplicates(mythics)
        } else {
            null
        } ?: pickWithoutDuplicates(rares)
          ?: pickWithoutDuplicates(uncommons)
          ?: pickWithoutDuplicates(commons)
          ?: throw IllegalStateException("No cards available for booster generation")

        booster.add(rareSlot)

        return booster
    }
}
