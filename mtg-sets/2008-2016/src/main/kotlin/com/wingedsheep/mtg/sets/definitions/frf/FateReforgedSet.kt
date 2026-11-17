package com.wingedsheep.mtg.sets.definitions.frf

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DedicatedSlot
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.SheetSelector
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fate Reforged Set (2015)
 *
 * The second set in the Khans of Tarkir block, featuring the Manifest mechanic,
 * the Dragons cycle, and the five Sieges (Outpost Siege, etc.) with a Khans /
 * Dragons mode choice at entry.
 *
 * Set Code: FRF
 * Release Date: January 23, 2015
 */
object FateReforgedSet : MtgSet {

    override val code = "FRF"
    override val displayName = "Fate Reforged"
    override val releaseDate = "2015-01-23"
    override val block = "Khans of Tarkir"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (35 rares print twice on the sheet,
     * 10 mythics once: the upgrade is 10 in 80), plus a land slot that never holds a basic: one of
     * the ten common gain lands (23 in 240 each) or, in one pack in 24, one of Khans of Tarkir's five
     * fetchlands (1 in 120 each). The gain lands open only in that slot, never on the common sheet.
     * The foil that replaced a common in 9 packs in 40 is cosmetic and not modelled, nor is the
     * common sheet's colour balancing. Figures from mtg.wtf/pack/frf (taw/magic-search-engine).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        DedicatedSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 10.0 / 80),
            slots = listOf(
                DedicatedSlot(
                    sheet = SheetSelector.Named(GAIN_LANDS + KTK_FETCHLANDS),
                    rarityWeights = mapOf(Rarity.COMMON to 230.0, Rarity.RARE to 10.0),
                ),
            ),
        )
    }

    override val boosterGuestCards: Map<String, String> by lazy { KTK_FETCHLANDS.associateWith { "KTK" } }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private val GAIN_LANDS = setOf(
        "Bloodfell Caves", "Blossoming Sands", "Dismal Backwater", "Jungle Hollow", "Rugged Highlands",
        "Scoured Barrens", "Swiftwater Cliffs", "Thornwood Falls", "Tranquil Cove", "Wind-Scarred Crag",
    )

    private val KTK_FETCHLANDS = setOf(
        "Bloodstained Mire", "Flooded Strand", "Polluted Delta", "Windswept Heath", "Wooded Foothills",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.frf.cards"
}
