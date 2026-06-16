package com.wingedsheep.mtg.sets.definitions.sos

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Secrets of Strixhaven (2026)
 *
 * A return to Strixhaven University, revisiting the five mage colleges and their
 * instant/sorcery "spells matter" identity.
 *
 * Set Code: SOS
 * Release Date: April 24, 2026
 */
object SecretsOfStrixhavenSet : MtgSet {

    override val code = "SOS"
    override val displayName = "Secrets of Strixhaven"
    override val releaseDate = "2026-04-24"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.sos.cards"
}
