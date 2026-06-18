package com.wingedsheep.mtg.sets.definitions.big

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Big Score (2024) — bonus sheet shipped alongside Outlaws of Thunder Junction.
 *
 * Set Code: BIG
 * Release Date: April 19, 2024
 */
object TheBigScoreSet : MtgSet {

    override val code = "BIG"
    override val displayName = "The Big Score"
    override val releaseDate = "2024-04-19"

    // All 30 cards of the bonus sheet are implemented — surface it as complete, not "partial".
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.big.cards"
}
