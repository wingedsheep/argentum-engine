package com.wingedsheep.gameserver.sealed

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.legions.LegionsSet
import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
import com.wingedsheep.mtg.sets.definitions.khans.KhansOfTarkirSet
import kotlin.random.Random

/**
 * Generates booster packs and sealed pools from card sets.
 *
 * Standard booster composition:
 * - 11 Commons
 * - 3 Uncommons
 * - 1 Rare (with ~12.5% chance of Mythic if the set has mythics)
 */
class BoosterGenerator(
    val availableSets: Map<String, SetConfig>
) {

    /**
     * Configuration for a card set that can be used for sealed.
     */
    data class SetConfig(
        val setCode: String,
        val setName: String,
        val cards: List<CardDefinition>,
        val basicLands: List<CardDefinition>,
        val incomplete: Boolean = false,
        val block: String? = null,
        val totalSetSize: Int? = null
    )

    companion object {
        /**
         * Portal set configuration (always available).
         */
        val portalSetConfig = SetConfig(
            setCode = PortalSet.SET_CODE,
            setName = PortalSet.SET_NAME,
            cards = PortalSet.allCards,
            basicLands = PortalSet.basicLands
        )

        /**
         * Onslaught set configuration.
         */
        val onslaughtSetConfig = SetConfig(
            setCode = OnslaughtSet.SET_CODE,
            setName = OnslaughtSet.SET_NAME,
            cards = OnslaughtSet.allCards,
            basicLands = PortalSet.basicLands,  // Use Portal lands for now
            block = "Onslaught"
        )

        /**
         * Scourge set configuration.
         */
        val scourgeSetConfig = SetConfig(
            setCode = ScourgeSet.SET_CODE,
            setName = ScourgeSet.SET_NAME,
            cards = ScourgeSet.allCards,
            basicLands = PortalSet.basicLands,  // Use Portal lands for now
            incomplete = true,
            block = "Onslaught",
            totalSetSize = 143
        )

        /**
         * Legions set configuration.
         */
        val legionsSetConfig = SetConfig(
            setCode = LegionsSet.SET_CODE,
            setName = LegionsSet.SET_NAME,
            cards = LegionsSet.allCards,
            basicLands = PortalSet.basicLands,
            incomplete = true,
            block = "Onslaught",
            totalSetSize = 145
        )

        /**
         * Khans of Tarkir set configuration.
         */
        val khansSetConfig = SetConfig(
            setCode = KhansOfTarkirSet.SET_CODE,
            setName = KhansOfTarkirSet.SET_NAME,
            cards = KhansOfTarkirSet.allCards,
            basicLands = PortalSet.basicLands,  // Use Portal lands for now
            incomplete = true,
            totalSetSize = 249
        )
    }

    /**
     * Get set configuration by set code.
     */
    fun getSetConfig(setCode: String): SetConfig? = availableSets[setCode]

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
     * Generate a single 15-card booster pack from one of the specified sets.
     * Randomly selects one set and generates a booster from that set only.
     * Each booster contains cards from a single set, never mixed.
     *
     * @param setCodes The set codes to choose from
     * @return List of 15 card definitions from a single randomly-selected set
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateBooster(setCodes: List<String>): List<CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }

        // Validate all set codes exist
        val setConfigs = setCodes.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // If any set is incomplete, merge all cards into a combined pool
        val hasIncomplete = setConfigs.any { it.incomplete }
        if (hasIncomplete) {
            val combinedCards = setConfigs.flatMap { it.cards }
            return generateBoosterFromCards(combinedCards)
        }

        // Pick a random set and generate a booster from it
        val selectedSet = setCodes.random()
        return generateBooster(selectedSet)
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
     * Generate a sealed pool from multiple sets with equal distribution.
     * Boosters are distributed evenly across all sets. Any remainder is
     * distributed deterministically based on the distributionSeed.
     *
     * Example: 2 sets with 6 boosters → 3 boosters from each set
     * Example: 3 sets with 6 boosters → 2 boosters from each set
     * Example: 2 sets with 5 boosters → 3 from one set, 2 from the other
     *
     * @param setCodes The set codes to generate from
     * @param boosterCount Number of boosters to open (default 6)
     * @param distributionSeed Seed for remainder distribution. Use the same seed
     *                         for all players in a tournament to ensure they get
     *                         the same set distribution (e.g., both get 3 Portal + 2 Onslaught).
     *                         If null, uses random distribution.
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateSealedPool(
        setCodes: List<String>,
        boosterCount: Int = 6,
        distributionSeed: Long? = null
    ): List<CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }
        if (setCodes.size == 1) {
            return generateSealedPool(setCodes.first(), boosterCount)
        }

        // Validate all set codes exist
        val setConfigs = setCodes.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // If any set is incomplete, merge all cards into a combined pool
        val hasIncomplete = setConfigs.any { it.incomplete }
        if (hasIncomplete) {
            val combinedCards = setConfigs.flatMap { it.cards }
            return (1..boosterCount).flatMap { generateBoosterFromCards(combinedCards) }
        }

        // Use seeded random for deterministic distribution, or default random
        val distributionRandom = distributionSeed?.let { Random(it) } ?: Random

        // Calculate even distribution
        val boostersPerSet = boosterCount / setCodes.size
        val remainder = boosterCount % setCodes.size

        // Build list of set codes for each booster
        val boosterAssignments = mutableListOf<String>()

        // Add base allocation for each set
        setCodes.forEach { setCode ->
            repeat(boostersPerSet) {
                boosterAssignments.add(setCode)
            }
        }

        // Distribute remainder using seeded random (deterministic for same seed)
        val shuffledSets = setCodes.shuffled(distributionRandom)
        repeat(remainder) { i ->
            boosterAssignments.add(shuffledSets[i])
        }

        // Note: We don't shuffle the final assignments - each player gets their
        // own random card contents anyway, only the set distribution needs to match
        return boosterAssignments.flatMap { generateBooster(it) }
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

    /**
     * Get basic lands available for deck building from multiple sets.
     * Uses the basic lands from the first set that has them.
     *
     * @param setCodes The set codes
     * @return Map of land name to CardDefinition (one variant per type)
     */
    fun getBasicLands(setCodes: List<String>): Map<String, CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }
        // Use basic lands from the first set
        return getBasicLands(setCodes.first())
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
