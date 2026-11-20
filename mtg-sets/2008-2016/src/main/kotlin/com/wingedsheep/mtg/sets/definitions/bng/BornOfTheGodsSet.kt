package com.wingedsheep.mtg.sets.definitions.bng

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Born of the Gods
 *
 * Set Code: BNG
 */
object BornOfTheGodsSet : MtgSet {

    override val code = "BNG"
    override val displayName = "Born of the Gods"
    override val releaseDate = "2014-02-07"
    override val block = "Theros"

    /**
     * Draft Booster (mtg.wtf/pack/bng): 10 commons, 3 uncommons, a rare or mythic (35 rares 2/80,
     * 10 mythics 1/80 — the era default's 1-in-8) and a basic land. Born of the Gods printed no
     * basics; its land slot carried Theros's, so those are the set's basics at deck building. The
     * foil replacing a common in 9 packs of 40 is cosmetic and not modelled.
     */
    override val basicLandsFallbackCode = "THS"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.bng.cards"
}
