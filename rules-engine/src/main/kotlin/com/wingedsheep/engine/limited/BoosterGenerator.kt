package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * Generates booster packs and sealed pools from card sets.
 *
 * Standard booster composition:
 * - 11 Commons
 * - 3 Uncommons
 * - 1 Rare (with ~12.5% chance of Mythic if the set has mythics)
 *
 * ## Scope
 *
 * A pure function of (set configs) → card pool. No Spring, I/O, or WebSocket
 * dependencies. Callers supply their own [SetConfig] map — the generator
 * itself does not import any specific card set, so it stays compatible with
 * rules-engine's "no card-specific dependencies" rule.
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
        val boosterStrategy: BoosterStrategy = StandardBooster(),
    )

    companion object {

        private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        /**
         * Distribute basic lands in a deck list across art variants for a nice mix.
         *
         * Replaces plain land names (e.g., "Plains" → 8) with variant identifiers
         * (e.g., "Plains#331" → 2, "Plains#332" → 2, "Plains#333" → 2, "Plains#334" → 2).
         *
         * The variant identifiers are `Name#SetCode-CollectorNumber` strings that resolve
         * via [com.wingedsheep.engine.registry.CardRegistry]'s secondary index — predating
         * the multi-printing system. This works correctly under multi-printing because the
         * resolved [CardDefinition.metadata.imageUri] is stamped onto each entity's
         * [com.wingedsheep.engine.state.components.identity.CardComponent.imageUri] at
         * game-init, which then beats the canonical metadata via the precedence flip in
         * `ClientStateTransformer`. The rich `cardEntries` channel (Phase 4 of the
         * multi-printing plan) is not used here — switching is part of the Phase 6.5
         * cleanup that retires the `Name#SET-CN` secondary index. See
         * `backlog/multi-printing-system.md`.
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
        return setConfig.boosterStrategy.generate(boosterPool(setConfig.cards), Random.Default)
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
            return StandardBooster().generate(boosterPool(combinedCards), Random.Default)
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
            val combinedStrategy = StandardBooster()
            val combinedPool = boosterPool(combinedCards)
            return (1..boosterCount).flatMap { combinedStrategy.generate(combinedPool, Random.Default) }
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
            val combinedStrategy = StandardBooster()
            val combinedPool = boosterPool(combinedCards)
            return (1..totalBoosters).flatMap { combinedStrategy.generate(combinedPool, Random.Default) }
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
     * Strip basic lands and non-booster cards (Special Guests / The List / promos);
     * strategies operate on the booster pool only.
     */
    private fun boosterPool(allCards: List<CardDefinition>): List<CardDefinition> =
        allCards.filter { !it.typeLine.isBasicLand && it.metadata.inBooster }
}
