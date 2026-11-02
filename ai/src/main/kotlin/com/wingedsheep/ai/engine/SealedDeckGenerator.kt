package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.limited.SideboardDerivation
import com.wingedsheep.sdk.model.CardDefinition

/**
 * Generates a 40-card sealed deck by opening 8 boosters from a set and
 * selecting a playable build from the resulting pool.
 *
 * The build itself is [buildLimitedDeck] — the Draftsim autobuilder every AI seat builds limited
 * decks with, the same engine as the deckbuild "Auto-build" button's default.
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
     * @return A map of "Name#SetCode-CollectorNumber" printing identifiers (plain names for cards with no printing coordinates) to count
     */
    fun generate(): Map<String, Int> = generate(randomSetCode())

    /**
     * Generates a sealed deck from 8 boosters of the specified set.
     *
     * @param setCode The set to generate boosters from
     * @return A map of "Name#SetCode-CollectorNumber" printing identifiers (plain names for cards with no printing coordinates) to count
     */
    fun generate(setCode: String): Map<String, Int> = generateDeck(setCode).deckList

    /**
     * [generate], plus the sideboard the pool leaves behind: every opened card the build didn't
     * run (CR 100.4b), exactly as a human's sealed sideboard is derived. That is what makes Learn
     * and the wishes work in a random-deck game — without it they'd reach into an empty sideboard.
     */
    fun generateDeck(setCode: String): GeneratedDeck {
        requireNotNull(boosterGenerator.availableSets[setCode]) { "Unknown set code: $setCode" }

        val pool = boosterGenerator.generateSealedPool(setCode, boosterCount = 8)
        val deck = buildLimitedDeck(pool, listOf(setCode))

        return pinnedToPool(deck, pool, boosterGenerator.getBasicLands(setCode))
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
     * @return A map of "Name#SetCode-CollectorNumber" printing identifiers (plain names for cards with no printing coordinates) to count
     */
    fun generate(setCodes: List<String>): Map<String, Int> = generateDeck(setCodes).deckList

    /** [generate] across several sets, with the leftover pool as the sideboard (see [generateDeck]). */
    fun generateDeck(setCodes: List<String>): GeneratedDeck {
        require(setCodes.isNotEmpty()) { "At least one set code is required" }
        if (setCodes.size == 1) return generateDeck(setCodes.first())
        setCodes.forEach { requireNotNull(boosterGenerator.availableSets[it]) { "Unknown set code: $it" } }

        val pool = boosterGenerator.generateSealedPool(setCodes, boosterCount = 8)
        val deck = buildLimitedDeck(pool, listOf(setCodes.first()))

        return pinnedToPool(deck, pool, boosterGenerator.getBasicLands(setCodes))
    }

    /**
     * Pins every card to the printing it was opened as, exactly as a human's sealed deck is
     * (`FreeForAllHandler` / `TournamentMatchHandler`): the basics to the set's standard art, and
     * each reprint to the pool's own printing. Without the reprint pin a bare name resolves to the
     * canonical definition — an Onslaught Clone would be played with Alpha's art.
     */
    private fun pinnedToPool(
        deck: Map<String, Int>,
        pool: List<CardDefinition>,
        basics: Map<String, CardDefinition>,
    ): GeneratedDeck {
        val printings = pool + basics.values
        return GeneratedDeck(
            deckList = BoosterGenerator.withCardArt(deck, printings),
            sideboard = BoosterGenerator.withCardArt(SideboardDerivation.fromPool(pool, deck), printings),
        )
    }

    private companion object {
        /**
         * Minimum distinct-card count for a set to be opened as a standalone sealed pool. Excludes
         * small supplementary sets (e.g. "The Big Score", BIG, ~30 cards) that can't fill 8 boosters
         * on their own. A full draftable set is ~250+ cards, so 200 leaves headroom while still
         * rejecting anything too thin.
         */
        private const val MIN_STANDALONE_SET_SIZE = 200
    }
}
