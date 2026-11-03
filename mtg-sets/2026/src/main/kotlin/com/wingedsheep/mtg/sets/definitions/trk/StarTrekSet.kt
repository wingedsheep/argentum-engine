package com.wingedsheep.mtg.sets.definitions.trk

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Star Trek (2026)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real printing is
 * Star Trek, with later sets contributing reprint [Printing] rows.
 *
 * Set Code: TRK
 * Release Date: 2026-11-13
 */
object StarTrekSet : MtgSet {

    override val code = "TRK"
    override val displayName = "Star Trek"
    override val releaseDate = "2026-11-13"

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
     * Play Booster per the "Collecting Magic: The Gathering | Star Trek" article
     * (magic.wizards.com/en/news/feature/collecting-star-trek, 2026-10-20): 7 commons, 3 uncommons,
     * a non-foil wildcard, a rare or mythic (80.1% / 12.4%), a traditional-foil wildcard and a land
     * slot that opens a common dual land 50% of the time and a basic otherwise.
     *
     * The wildcard mixes are the article's main-set shares; its sub-1–2% special-treatment options
     * (Klingon, LCARS, scene, classic Enterprise, borderless lands) are art variants of main-set
     * cards, so they open as the card's ordinary printing. The 1-in-25 Stardates (SDS) common replacement is
     * not modelled: that pack opens its seventh common instead. The main set's 80 rares include the ten
     * default-frame shock lands (#297–306).
     * The land slot picks its 20 basic arts uniformly, where paper weights default frame 4:1:1 against
     * the holodeck and away-team arts — a cosmetic difference.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = PlayBooster(
                mythicChance = 12.4 / (80.1 + 12.4),
                wildcardSlots = listOf(
                    PlayBooster.wildcard(common = 0.208, uncommon = 0.541, rare = 0.167, mythic = 0.026),
                    PlayBooster.wildcard(common = 0.512, uncommon = 0.347, rare = 0.063, mythic = 0.010),
                ),
            ),
            nonbasicLandNames = COMMON_DUALS,
            basicLands = basicLands.filter { it.metadata.inBooster },
            nonbasicChance = 0.5,
        )
    }

    /** The ten common dual lands the land slot opens. */
    private val COMMON_DUALS: Set<String> = setOf(
        "Andoria", "Bajor", "Cardassia Prime", "Earth, Sector 001", "Ferenginar",
        "Orion", "Qo'noS", "Risa", "Romulus", "Vulcan",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.trk.cards"
}
