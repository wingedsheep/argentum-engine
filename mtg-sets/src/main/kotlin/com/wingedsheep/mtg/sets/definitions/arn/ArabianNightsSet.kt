package com.wingedsheep.mtg.sets.definitions.arn

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Arabian Nights (1993)
 *
 * The first Magic: The Gathering expansion. 78 cards, no block. Many of its
 * mechanics (banding, landwalk variants, coin flips, control-changing effects,
 * subgames, ante) predate or stress the modern rules, so a handful of cards
 * require engine work and are tracked separately in
 * `backlog/sets/arabian-nights/cards.md`.
 *
 * Set Code: ARN
 * Release Date: December 17, 1993
 * Card Count: 78
 */
object ArabianNightsSet : MtgSet {

    override val code = "ARN"
    override val displayName = "Arabian Nights"
    override val releaseDate = "1993-12-17"
    override val basicLandsFallback = PortalSet
    override val incomplete = true
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.arn.cards"
}
