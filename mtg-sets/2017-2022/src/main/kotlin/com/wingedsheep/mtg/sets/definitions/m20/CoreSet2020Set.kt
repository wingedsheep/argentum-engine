package com.wingedsheep.mtg.sets.definitions.m20

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Core Set 2020 (2019)
 *
 * Complete: every card is implemented, canonicals first printed here plus `Printing` rows for reprints.
 *
 * Set Code: M20
 * Release Date: 2019-07-12
 */
object CoreSet2020Set : MtgSet {

    override val code = "M20"
    override val displayName = "Core Set 2020"
    override val releaseDate = "2019-07-12"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Draft Booster: 10 commons, 3 uncommons, 1 rare or mythic, and a land slot holding one of 31 cards
     * at 1/31 each: the ten common gain lands, Evolving Wilds, or one of the 20 basic arts (so a
     * nonbasic 11/31 of the time). The rare slot weights rares twice to mythics' once
     * (53 rares, 15 mythics -> 15/121). The foil replacing a common in a third of packs is cosmetic,
     * not modelled. Source: the M20 draft booster sheets as kept in taw/magic-search-engine
     * (mtg.wtf/pack/m20).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 15.0 / 121),
            nonbasicLandNames = LAND_SLOT_NONBASICS,
            basicLands = basicLands.filter { it.metadata.inBooster },
            nonbasicChance = 11.0 / 31,
        )
    }

    private val LAND_SLOT_NONBASICS = setOf(
        "Bloodfell Caves", "Blossoming Sands", "Dismal Backwater", "Evolving Wilds", "Jungle Hollow",
        "Rugged Highlands", "Scoured Barrens", "Swiftwater Cliffs", "Thornwood Falls", "Tranquil Cove",
        "Wind-Scarred Crag",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m20.cards"
}
