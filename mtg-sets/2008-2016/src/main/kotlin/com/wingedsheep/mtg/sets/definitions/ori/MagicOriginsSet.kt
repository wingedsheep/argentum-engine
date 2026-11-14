package com.wingedsheep.mtg.sets.definitions.ori

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic Origins (2015)
 *
 * Every card of the set is implemented: canonicals for the cards first printed here, and
 * `Printing` rows for the reprints.
 *
 * Set Code: ORI
 * Release Date: 2015-07-17
 */
object MagicOriginsSet : MtgSet {

    override val code = "ORI"
    override val displayName = "Magic Origins"
    override val releaseDate = "2015-07-17"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ori.cards"
}
