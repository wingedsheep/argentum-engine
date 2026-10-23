package com.wingedsheep.mtg.sets.definitions.mid

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.TwoDoubleFacedSlotBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Innistrad: Midnight Hunt (2021)
 *
 * Set Code: MID
 * Release Date: September 24, 2021
 */
object InnistradMidnightHuntSet : MtgSet {

    override val code = "MID"
    override val displayName = "Innistrad: Midnight Hunt"
    override val releaseDate = "2021-09-24"

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
     * Draft Booster: 1 double-faced common, 9 commons, 2 uncommons and a double-faced/single-faced pair
     * that is one uncommon and one rare or mythic — the double-faced card is the rare in 1 pack in 6 —
     * plus a basic land (supplied at deck building). The traditional foil replacing a common in a third
     * of packs is cosmetic and not modelled.
     */
    override val boosterStrategy: BoosterStrategy = TwoDoubleFacedSlotBooster(commons = 9, uncommons = 3)

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mid.cards"
}
