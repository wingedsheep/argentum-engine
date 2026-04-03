package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.ai.buildHeuristicSealedDeck
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import org.slf4j.LoggerFactory

/**
 * Generates a 40-card sealed deck by opening 8 boosters from a random set
 * and using the heuristic deck builder to pick the best cards.
 *
 * Basic land names in the output are distributed across art variants
 * from the selected set.
 */
class SealedDeckGenerator(
    private val boosterGenerator: BoosterGenerator
) {
    private val logger = LoggerFactory.getLogger(SealedDeckGenerator::class.java)

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
        val setName = boosterGenerator.availableSets[setCode]!!.setName

        val pool = boosterGenerator.generateSealedPool(setCode, boosterCount = 8)
        val deck = buildHeuristicSealedDeck(pool)

        logger.info("Built sealed deck from {} ({}) — pool: {} cards, deck: {} cards ({})",
            setName, setCode, pool.size, deck.values.sum(),
            deck.entries.filter { !BASIC_LAND_NAMES.contains(it.key) }.take(5))

        // Distribute basic lands across art variants for visual variety
        val variants = boosterGenerator.getAllBasicLandVariants(setCode)
        return BoosterGenerator.distributeBasicLandVariants(deck, variants)
    }

    companion object {
        private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    }
}
