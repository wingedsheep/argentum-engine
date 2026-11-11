package com.wingedsheep.mtg.sets.definitions.ogw

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Oath of the Gatewatch (2016)
 *
 * Every card of the set is implemented: canonicals for the cards first printed here, and
 * `Printing` rows for the reprints.
 *
 * Set Code: OGW
 * Release Date: 2016-01-22
 */
object OathOfTheGatewatchSet : MtgSet {

    override val code = "OGW"
    override val displayName = "Oath of the Gatewatch"
    override val releaseDate = "2016-01-22"
    override val block = "Battle for Zendikar"

    /**
     * Oath of the Gatewatch's only basics are its two Wastes, which are pack cards (a Limited
     * player plays only the Wastes they open), so deck building hands out Battle for Zendikar's
     * basics — the ones its boosters' land slot carried.
     */
    override val basicLandsFallbackCode = "BFZ"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 42 rares and 12 mythics make the upgrade exactly 1 in 8), plus a full-art Battle
     * for Zendikar basic, supplied at deck building (see [basicLandsFallbackCode]). The two full-art
     * Wastes sit on the common sheet, so Wastes opens about twice as often as any other common. The
     * foil (or Zendikar Expedition, 1 in 144 packs) that replaced a common in 9 packs in 40 is
     * cosmetic and not modelled. Figures from taw/magic-search-engine `data/boosters/ogw/draft.yaml`
     * and MTGGoldfish's OGW Limited expected numbers.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ogw.cards"
}
