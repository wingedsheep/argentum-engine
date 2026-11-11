package com.wingedsheep.mtg.sets.definitions.fin

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Final Fantasy (2025)
 *
 * Set Code: FIN
 * Release Date: June 13, 2025
 */
object FinalFantasySet : MtgSet {

    override val code = "FIN"
    override val displayName = "Final Fantasy"
    override val releaseDate = "2025-06-13"

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
     * Play Booster: the non-foil wildcard is 19.3% C / 64% U / 16.7% R-M and the traditional-foil wildcard 55.9% C /
     * 36.7% U / 6.5% R / 1% M. The Through the Ages card (FCA, not in the engine) in 1 pack in 3 stays a common. The
     * land slot is one of the ten common dual Towns 55% of the time and a basic otherwise. Figures from WotC's
     * "Collecting Final Fantasy" article, as kept in taw/magic-search-engine `data/boosters`.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = PlayBooster(
                wildcardSlots = listOf(
                    PlayBooster.wildcard(common = 0.193, uncommon = 0.64, rareOrMythic = 0.167),
                    PlayBooster.wildcard(common = 0.5585, uncommon = 0.3665, rare = 0.065, mythic = 0.01),
                ),
            ),
            nonbasicLandNames = setOf(
                "Baron, Airship Kingdom", "Gohn, Town of Ruin", "Gongaga, Reactor Town",
                "Guadosalam, Farplane Gateway", "Insomnia, Crown City", "Rabanastre, Royal City",
                "Sharlayan, Nation of Scholars", "Treno, Dark City", "Vector, Imperial Capital",
                "Windurst, Federation Center",
            ),
            // Wastes reaches the land slot through the pool, as a pack-only basic.
            basicLands = basicLands.filter { it.metadata.inBooster && it.typeLine.isFreelyAddedBasicLand },
            nonbasicChance = 0.55,
        )
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fin.cards"
}
