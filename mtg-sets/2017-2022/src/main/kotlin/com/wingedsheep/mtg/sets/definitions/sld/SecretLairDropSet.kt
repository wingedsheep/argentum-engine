package com.wingedsheep.mtg.sets.definitions.sld

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Secret Lair Drop (2019–)
 *
 * Wizards' direct-to-consumer drop series. Scaffolded here as the canonical home for
 * cards whose only printings are Secret Lair drops (Scryfall set type `box`), and so
 * every already-implemented card it reprints carries its Secret Lair printing.
 * Intentionally incomplete relative to the official set.
 *
 * Set Code: SLD
 * Release Date: 2019-12-02
 */
object SecretLairDropSet : MtgSet {

    override val code = "SLD"
    override val displayName = "Secret Lair Drop"
    override val releaseDate = "2019-12-02"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.sld.cards"
}
