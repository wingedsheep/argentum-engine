package com.wingedsheep.mtg.sets.definitions.afc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Forgotten Realms Commander (2021)
 *
 * Commander preconstructed decks released alongside Adventures in the Forgotten Realms.
 *
 * Set Code: AFC
 * Release Date: July 23, 2021
 */
object ForgottenRealmsCommanderSet : MtgSet {

    override val code = "AFC"
    override val displayName = "Forgotten Realms Commander"
    override val releaseDate = "2021-07-23"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.afc.cards"
}
