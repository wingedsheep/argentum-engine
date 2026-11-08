package com.wingedsheep.mtg.sets.definitions.v11

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * From the Vault: Legends
 *
 * A fifteen-card premium reprint product, all legendary creatures, all foil. It carries no boosters
 * and no basic lands, so it is not a sealed/draft environment — but it is the earliest real printing
 * of Mikaeus, the Lunarch, which Innistrad later reprinted.
 *
 * Set Code: V11
 * Release Date: August 26, 2011
 */
object FromTheVaultLegendsSet : MtgSet {

    override val code = "V11"
    override val displayName = "From the Vault: Legends"
    override val releaseDate = "2011-08-26"
    override val incomplete = true
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.v11.cards"
}
