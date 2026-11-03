package com.wingedsheep.mtg.sets.definitions.m19

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Core Set 2019 (2018)
 *
 * Complete: every booster card is implemented, plus the Planeswalker Deck exclusives and Nexus of Fate
 * (Buy-a-Box promo), which never open in packs.
 *
 * Set Code: M19
 * Release Date: 2018-07-13
 */
object CoreSet2019Set : MtgSet {

    override val code = "M19"
    override val displayName = "Core Set 2019"
    override val releaseDate = "2018-07-13"

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
     * Draft Booster: 10 commons, 3 uncommons, 1 rare or mythic, and a land slot holding one of 30 cards
     * at 1/30 each: the ten common dual taplands or one of the 20 basic arts (so a nonbasic 10/30 of the
     * time). The rare slot weights rares twice to mythics' once (53 rares, 16 mythics -> 16/122). The
     * foil replacing a common in 9 of 40 packs is cosmetic, not modelled. The Planeswalker Deck cards
     * (#281-314) never open: the pool is limited to the set's booster cards. Source: the M19 draft
     * booster sheets as kept in taw/magic-search-engine (mtg.wtf/pack/m19).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 16.0 / 122),
            nonbasicLandNames = LAND_SLOT_NONBASICS,
            basicLands = basicLands.filter { it.metadata.inBooster },
            nonbasicChance = 10.0 / 30,
        )
    }

    private val LAND_SLOT_NONBASICS = setOf(
        "Cinder Barrens", "Forsaken Sanctuary", "Foul Orchard", "Highland Lake", "Meandering River",
        "Stone Quarry", "Submerged Boneyard", "Timber Gorge", "Tranquil Expanse", "Woodland Stream",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m19.cards"
}
