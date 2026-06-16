package com.wingedsheep.ai.engine

import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DraftsimDeckBuildAdvisor
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.model.CardDefinition
import org.slf4j.LoggerFactory

/**
 * Generates a 40-card sealed deck by opening 8 boosters from a set and
 * selecting a playable build from the resulting pool.
 *
 * The build itself comes from the Draftsim autobuilder ([DraftsimDeckBuildAdvisor]) — the same
 * archetype-aware, ratings-driven engine the deckbuild "Auto-build" button uses — so quick/AI games
 * get the same quality of automatic build as a human clicking Auto-build, backed by Draftsim's
 * per-set ratings/removal/archetype tables. The [buildHeuristicSealedDeck] color-and-curve heuristic
 * remains as a safety net for the rare pool Draftsim can't build from.
 *
 * Basic land names in the output are distributed across art variants
 * from the selected set.
 */
class SealedDeckGenerator(
    private val boosterGenerator: BoosterGenerator
) {
    /**
     * Picks a random set code that can actually produce a sealed deck.
     *
     * Only [BoosterGenerator.SetConfig.fullyImplemented] sets are eligible: a partial set
     * (incomplete, or not curated for sealed) can have a card pool too thin for the single-set
     * booster strategy, which then throws `No cards available for booster generation`. Random
     * quick/AI games have no host to opt into a partial set, so they must draw from a playable one.
     * Falls back to all available sets only if — unexpectedly — none are fully implemented.
     */
    fun randomSetCode(): String {
        val playable = boosterGenerator.availableSets.values.filter { it.fullyImplemented }
        val pool = playable.ifEmpty { boosterGenerator.availableSets.values.toList() }
        return pool.random().setCode
    }

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
        val deck = buildSealedDeck(pool, setCode)

        // Distribute basic lands across art variants for visual variety
        val variants = boosterGenerator.getAllBasicLandVariants(setCode)
        return BoosterGenerator.distributeBasicLandVariants(deck, variants)
    }

    /**
     * Builds a sealed deck from [pool] with the Draftsim autobuilder, scoped to [setCode] so it loads
     * that set's ratings/removal/archetype tables. Sets without a Draftsim ratings file still build
     * (the scorer falls back to a rarity ladder). Falls back to [buildHeuristicSealedDeck] only if
     * Draftsim throws or yields an empty list — the heuristic always produces a legal 40-card deck.
     */
    private fun buildSealedDeck(pool: List<CardDefinition>, setCode: String): Map<String, Int> {
        val result = runCatching {
            DraftsimDeckBuildAdvisor.buildDeck(DeckBuildRequest(pool = pool, setCodes = listOf(setCode)))
        }.getOrElse { error ->
            logger.warn("Draftsim build failed for set '{}'; falling back to heuristic", setCode, error)
            null
        }
        val build = result?.builds?.getOrNull(result.recommended)
        if (build != null && build.deckList.isNotEmpty()) return build.deckList

        if (result != null) {
            logger.warn("Draftsim produced no build for set '{}'; falling back to heuristic", setCode)
        }
        return buildHeuristicSealedDeck(pool)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SealedDeckGenerator::class.java)
    }
}
