package com.wingedsheep.mtg.sets.definitions.dsc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duskmourn: House of Horror Commander (2024)
 *
 * Commander preconstructed decks. Scaffolded to hold the canonical [CardDefinition] for cards
 * first printed here (e.g. Gleeful Arsonist) and the [Printing] rows of the already-implemented
 * cards it reprints.
 *
 * [sealedSupported] stays false: Commander precons are fixed 100-card decks, not a draftable product.
 *
 * Set Code: DSC
 * Release Date: 2024-09-27
 */
object DuskmournHouseOfHorrorCommanderSet : MtgSet {

    override val code = "DSC"
    override val displayName = "Duskmourn: House of Horror Commander"
    override val releaseDate = "2024-09-27"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dsc.cards"
}
