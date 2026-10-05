package com.wingedsheep.mtg.sets.definitions.om1

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Through the Omenpaths (2025)
 *
 * Set Code: OM1
 * Release Date: September 23, 2025
 *
 * The digital in-universe companion to Marvel's Spider-Man: the same 188 cards with new names
 * and art set on Magic's own planes. Each card shares its oracle identity with the SPM printing,
 * so this set is all [Printing] rows — the new name rides on [Printing.printedName] and the rules
 * text stays on the canonical SPM `card(...)`.
 */
object ThroughTheOmenpathsSet : MtgSet {

    override val code = "OM1"
    override val displayName = "Through the Omenpaths"
    override val releaseDate = "2025-09-23"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /** OM1 prints no basic lands; Limited pools use Spider-Man's. */
    override val basicLandsFallbackCode = "SPM"

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.om1.cards"
}
