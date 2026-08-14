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
     * Eligible sets must be [BoosterGenerator.SetConfig.fullyImplemented], not an
     * [BoosterGenerator.SetConfig.extensionSet] (bonus sheets like "The Big Score" are only playable
     * alongside a regular set), *and* large enough to open 8 boosters on their own — a pool that is
     * too thin for the single-set booster strategy throws `No cards available for booster
     * generation`. Random quick/AI games have no host to opt into a partial set, so they must draw
     * from a full standalone set. The fallbacks widen the pool only if — unexpectedly — nothing
     * qualifies, so this never throws on an empty pool.
     */
    fun randomSetCode(): String {
        val sets = boosterGenerator.availableSets.values.filter { !it.extensionSet }.ifEmpty {
            boosterGenerator.availableSets.values.toList()
        }
        val standalone = sets.filter { it.fullyImplemented && it.distinctCardCount >= MIN_STANDALONE_SET_SIZE }
        val pool = standalone
            .ifEmpty { sets.filter { it.fullyImplemented } }
            .ifEmpty { sets }
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

        // Pin the basics to the set's standard art, exactly as a human's submitted deck is.
        return BoosterGenerator.withBasicLandArt(deck, boosterGenerator.getBasicLands(setCode))
    }

    /**
     * Generates a sealed deck from 8 boosters spread evenly across [setCodes].
     *
     * The multi-set flavour of [generate]: [BoosterGenerator.generateSealedPool] already knows how
     * to split a booster count across sets, so this only has to pick which set's ratings tables the
     * autobuilder loads — the first one, since Draftsim's tables are per-set and a mixed pool has no
     * single home set. Sets without a ratings file fall back to the scorer's rarity ladder anyway.
     *
     * @param setCodes the sets to open boosters from; a single-element list behaves exactly like
     *        [generate].
     * @return A map of card name (or "Name#SetCode-CollectorNumber" for lands) to count
     */
    fun generate(setCodes: List<String>): Map<String, Int> {
        require(setCodes.isNotEmpty()) { "At least one set code is required" }
        if (setCodes.size == 1) return generate(setCodes.first())
        setCodes.forEach { requireNotNull(boosterGenerator.availableSets[it]) { "Unknown set code: $it" } }

        val pool = boosterGenerator.generateSealedPool(setCodes, boosterCount = 8)
        val deck = buildSealedDeck(pool, setCodes.first())

        return BoosterGenerator.withBasicLandArt(deck, boosterGenerator.getBasicLands(setCodes))
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

        /**
         * Minimum distinct-card count for a set to be opened as a standalone sealed pool. Excludes
         * small supplementary sets (e.g. "The Big Score", BIG, ~30 cards) that can't fill 8 boosters
         * on their own. A full draftable set is ~250+ cards, so 200 leaves headroom while still
         * rejecting anything too thin.
         */
        private const val MIN_STANDALONE_SET_SIZE = 200
    }
}
