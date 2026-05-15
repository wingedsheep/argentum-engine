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
 * Companion product to Marvel's Spider-Man (SPM) — released three days earlier and
 * marked as SPM's parent set on Scryfall. Hosts the canonical [CardDefinition]s for
 * cards whose earliest real-expansion printing is OM1 (e.g., Kraven the Hunter);
 * later SPM printings contribute only `Printing` rows.
 */
object OmenpathsSet : MtgSet {

    override val code = "OM1"
    override val displayName = "Through the Omenpaths"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.om1.cards"
}
