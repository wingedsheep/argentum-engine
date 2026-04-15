package com.wingedsheep.engine.ai

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Generates booster packs and sealed pools from card sets.
 *
 * Standard booster composition:
 * - 11 Commons
 * - 3 Uncommons
 * - 1 Rare (with ~12.5% chance of Mythic if the set has mythics)
 *
 * ## Why this lives in rules-engine
 *
 * The generator is a pure function of (set configs) → card pool. It has no
 * Spring, I/O, or WebSocket dependencies, which is exactly what the gym
 * training loop needs. Callers supply their own [SetConfig] map — the
 * generator itself does not import any specific card set, so it stays
 * compatible with rules-engine's "no card-specific dependencies" rule.
 *
 * Application-level set catalogues (Portal, Bloomburrow, …) live in their
 * respective consumer modules (e.g. `game-server`'s `sealed/SetConfigs.kt`).
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
        val totalSetSize: Int? = null,
        val guaranteedLegendary: Boolean = false
    )

    companion object {

        private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        /**
         * Distribute basic lands in a deck list across art variants for a nice mix.
         *
         * Replaces plain land names (e.g., "Plains" → 8) with variant identifiers
         * (e.g., "Plains#331" → 2, "Plains#332" → 2, "Plains#333" → 2, "Plains#334" → 2).
         *
         * @param deckList The original deck list with basic land names
         * @param variants Map of land name to all art variants from the set
         * @return Modified deck list with basic lands distributed across variants
         */
        fun distributeBasicLandVariants(
            deckList: Map<String, Int>,
            variants: Map<String, List<CardDefinition>>
        ): Map<String, Int> {
            val result = mutableMapOf<String, Int>()

            for ((cardName, count) in deckList) {
                if (cardName !in BASIC_LAND_NAMES || count <= 0) {
                    result[cardName] = count
                    continue
                }

                val landVariants = variants[cardName]
                if (landVariants.isNullOrEmpty()) {
                    result[cardName] = count
                    continue
                }

                // Distribute evenly across variants with round-robin assignment
                val variantCount = landVariants.size
                val basePerVariant = count / variantCount
                val remainder = count % variantCount

                for ((i, variant) in landVariants.withIndex()) {
                    val variantCopies = basePerVariant + if (i < remainder) 1 else 0
                    if (variantCopies > 0) {
                        val collectorNumber = variant.metadata.collectorNumber
                        val identifier = if (collectorNumber != null && variant.setCode != null) {
                            "$cardName#${variant.setCode}-$collectorNumber"
                        } else if (collectorNumber != null) {
                            "$cardName#$collectorNumber"
                        } else {
                            cardName
                        }
                        result[identifier] = (result[identifier] ?: 0) + variantCopies
                    }
                }
            }

            return result
        }
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

        return if (setConfig.guaranteedLegendary) {
            generateDominariaBooster(setConfig.cards)
        } else {
            generateBoosterFromCards(setConfig.cards)
        }
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
     * Generate a sealed pool using an explicit per-set booster distribution.
     *
     * @param boosterDistribution Map of set code to number of boosters from that set
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateSealedPool(
        boosterDistribution: Map<String, Int>
    ): List<CardDefinition> {
        if (boosterDistribution.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }

        // Validate all set codes exist
        val setConfigs = boosterDistribution.keys.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // If any set is incomplete, merge all cards and generate total boosters
        val hasIncomplete = setConfigs.any { it.incomplete }
        if (hasIncomplete) {
            val combinedCards = setConfigs.flatMap { it.cards }
            val totalBoosters = boosterDistribution.values.sum()
            return (1..totalBoosters).flatMap { generateBoosterFromCards(combinedCards) }
        }

        // Generate boosters per set according to distribution
        return boosterDistribution.flatMap { (setCode, count) ->
            (1..count).flatMap { generateBooster(setCode) }
        }
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

    /**
     * Get all basic land variants for deck building from a set.
     *
     * @param setCode The set code
     * @return Map of land name to list of all art variants
     */
    fun getAllBasicLandVariants(setCode: String): Map<String, List<CardDefinition>> {
        val setConfig = availableSets[setCode]
            ?: throw IllegalArgumentException("Unknown set code: $setCode")

        return setConfig.basicLands.groupBy { it.name }
    }

    /**
     * Get all basic land variants for deck building from multiple sets.
     * Uses the basic lands from the first set that has them.
     *
     * @param setCodes The set codes
     * @return Map of land name to list of all art variants
     */
    fun getAllBasicLandVariants(setCodes: List<String>): Map<String, List<CardDefinition>> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }
        return getAllBasicLandVariants(setCodes.first())
    }

    /**
     * Dominaria-style booster: guaranteed legendary creature in every pack.
     * The legendary replaces a card of its rarity, so the pack is still 15 cards:
     * - If legendary is uncommon: 11C + 2U + 1 legendary U + 1R = 15
     * - If legendary is rare/mythic: 11C + 3U + 1 legendary R/M = 15
     * Falls back to standard generation if no legendary creatures are available.
     */
    private fun generateDominariaBooster(allCards: List<CardDefinition>): List<CardDefinition> {
        val boosterPool = allCards.filter { !it.typeLine.isBasicLand }

        val legendaries = boosterPool.filter { it.typeLine.isLegendary && it.typeLine.isCreature }
        if (legendaries.isEmpty()) return generateBoosterFromCards(allCards)

        val legendary = legendaries.random()

        val commons = boosterPool.filter { it.metadata.rarity == Rarity.COMMON }.toMutableList()
        val uncommons = boosterPool.filter {
            it.metadata.rarity == Rarity.UNCOMMON && it.name != legendary.name
        }.toMutableList()
        val rares = boosterPool.filter {
            it.metadata.rarity == Rarity.RARE && it.name != legendary.name
        }.toMutableList()
        val mythics = boosterPool.filter {
            it.metadata.rarity == Rarity.MYTHIC && it.name != legendary.name
        }.toMutableList()

        val booster = mutableListOf<CardDefinition>()
        val usedCardNames = mutableSetOf(legendary.name)

        fun pickWithoutDuplicates(pool: MutableList<CardDefinition>): CardDefinition? {
            val available = pool.filter { it.name !in usedCardNames }
            if (available.isEmpty()) return null
            val picked = available.random()
            usedCardNames.add(picked.name)
            return picked
        }

        // 11 Commons
        repeat(11) {
            pickWithoutDuplicates(commons)?.let { booster.add(it) }
        }

        // Uncommons: 3 if legendary is rare/mythic, 2 if legendary is uncommon
        val uncommonCount = if (legendary.metadata.rarity == Rarity.UNCOMMON) 2 else 3
        repeat(uncommonCount) {
            pickWithoutDuplicates(uncommons)?.let { booster.add(it) }
        }

        // Rare/mythic slot: skip if the legendary already fills it
        val legendaryIsRareOrMythic = legendary.metadata.rarity == Rarity.RARE ||
            legendary.metadata.rarity == Rarity.MYTHIC
        if (!legendaryIsRareOrMythic) {
            val rareSlot = if (mythics.isNotEmpty() && Math.random() < 0.125) {
                pickWithoutDuplicates(mythics)
            } else {
                null
            } ?: pickWithoutDuplicates(rares)
              ?: pickWithoutDuplicates(uncommons)
              ?: pickWithoutDuplicates(commons)

            rareSlot?.let { booster.add(it) }
        }

        // Guaranteed legendary slot (last, matching physical pack order)
        booster.add(legendary)

        return booster
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
