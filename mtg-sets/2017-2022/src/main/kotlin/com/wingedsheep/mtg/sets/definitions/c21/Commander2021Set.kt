package com.wingedsheep.mtg.sets.definitions.c21

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

object Commander2021Set : MtgSet {
    override val code = "C21"
    override val displayName = "Commander 2021"
    override val releaseDate = "2021-04-23"
    override val sealedSupported = false
    override val incomplete = true
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.c21.cards"
}
