package com.wingedsheep.mtg.sets.definitions.neo

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DoubleFacedSlotBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Kamigawa: Neon Dynasty
 *
 * Set Code: NEO
 */
object NeonDynastySet : MtgSet {

    override val code = "NEO"
    override val displayName = "Kamigawa: Neon Dynasty"
    override val releaseDate = "2022-02-18"

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
     * Draft Booster: 1 rare or mythic, 1 double-faced common or uncommon, 3 uncommons, 9 commons and a
     * basic land (supplied at deck building), per the set's product overview.
     */
    override val boosterStrategy: BoosterStrategy = DoubleFacedSlotBooster(commons = 9, uncommons = 3)

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.neo.cards"
}
