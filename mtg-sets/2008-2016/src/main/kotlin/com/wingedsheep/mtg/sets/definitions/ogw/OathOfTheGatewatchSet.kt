package com.wingedsheep.mtg.sets.definitions.ogw

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
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
    override val sealedSupported = false

    /**
     * Oath of the Gatewatch's only basics are its two Wastes, which are pack cards (a Limited
     * player plays only the Wastes they open), so deck building hands out Battle for Zendikar's
     * basics — the ones its boosters' land slot carried.
     */
    override val basicLandsFallbackCode = "BFZ"

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
