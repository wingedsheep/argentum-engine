package com.wingedsheep.mtg.sets.definitions.dtk

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Dragons of Tarkir Set (2015)
 *
 * Dragons of Tarkir is the third set in the Khans of Tarkir block, depicting
 * an alternate timeline where the dragons of Tarkir survived. It introduced
 * the megamorph, formidable, exploit, dash, and rebound mechanics.
 *
 * Set Code: DTK
 * Release Date: March 27, 2015
 * Card Count: 264
 */
object DragonsOfTarkirSet : MtgSet {

    override val code = "DTK"
    override val displayName = "Dragons of Tarkir"
    override val releaseDate = "2015-03-27"
    override val block = "Khans of Tarkir"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 53 rares and 15 mythics make the upgrade 15 in 121), plus a basic land from a
     * basic-only land slot, supplied at deck building. The foil that replaced a common in 9 packs
     * in 40 is cosmetic and not modelled. Every DTK card opens in packs. Figures from
     * mtg.wtf/pack/dtk (taw/magic-search-engine).
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 15.0 / 121)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dtk.cards"
}
