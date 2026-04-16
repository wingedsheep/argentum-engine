package com.wingedsheep.ai.engine

import com.wingedsheep.engine.limited.BoosterGenerator

/**
 * Generates a 40-card sealed deck by opening 8 boosters from a set and
 * selecting a playable build from the resulting pool.
 *
 * Basic land names in the output are distributed across art variants
 * from the selected set.
 */
class SealedDeckGenerator(
    private val boosterGenerator: BoosterGenerator
) {
    /**
     * Picks a random available set code.
     */
    fun randomSetCode(): String = boosterGenerator.availableSets.keys.random()

    /**
     * Generates a sealed deck from 8 boosters of a random available set.
     *
     * @return A map of card name (or "Name#SetCode-CollectorNumber" for lands) to count
     */
    fun generate(): Map<String, Int> = generate(randomSetCode())

    /**
     * Generates a sealed deck from 8 boosters of the specified set.
     *
     * @param setCode The set to generate boosters from
     * @return A map of card name (or "Name#SetCode-CollectorNumber" for lands) to count
     */
    fun generate(setCode: String): Map<String, Int> {
        requireNotNull(boosterGenerator.availableSets[setCode]) { "Unknown set code: $setCode" }

        val pool = boosterGenerator.generateSealedPool(setCode, boosterCount = 8)
        val deck = buildHeuristicSealedDeck(pool)

        // Distribute basic lands across art variants for visual variety
        val variants = boosterGenerator.getAllBasicLandVariants(setCode)
        return BoosterGenerator.distributeBasicLandVariants(deck, variants)
    }
}
