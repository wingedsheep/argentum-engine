package com.wingedsheep.mtg.sets.definitions.arn

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Arabian Nights (1993)
 *
 * The first Magic: The Gathering expansion. 78 cards, no block. Many of its
 * mechanics (banding, landwalk variants, coin flips, control-changing effects,
 * subgames, ante) predate or stress the modern rules.
 *
 * Complete: every card we intend to build is built. The two that remain are policy
 * exclusions listed in `coverage/card-exclusions.json` — Jeweled Bird needs the ante
 * zone the engine doesn't have, and Shahrazad needs a full Magic subgame — so the set
 * is draftable and no longer carries `incomplete`.
 *
 * Set Code: ARN
 * Release Date: December 17, 1993
 * Card Count: 78
 */
object ArabianNightsSet : MtgSet {

    override val code = "ARN"
    override val displayName = "Arabian Nights"
    override val releaseDate = "1993-12-17"
    override val basicLandsFallbackCode = "POR"
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        // Self-stamp [code] onto every card, honouring the MtgSet contract that `cards` is already
        // set-stamped. This makes `CardDefinition.setCode` a reliable "originally printed in ARN"
        // signal for every consumer (engine tests included), not only the game-server load path —
        // which is what City in a Bottle's OriginallyPrintedInSet("ARN") predicate reads.
        CardDiscovery.findIn(CARDS_PACKAGE).map { if (it.setCode == null) it.copy(setCode = code) else it }
    }

    /**
     * ARN printed only Mountain. Keep [basicLandsFallbackCode] for a complete Limited land supply while
     * exposing the set's own printing here so discovery and set coverage include it.
     */
    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    /**
     * Arabian Nights predates token *cards* — Scryfall has no `tarn` set to sync from — so its token
     * art is self-hosted under `web-client/public/images/tokens/` and declared here, the same route
     * Invasion, Apocalypse and Odyssey take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Bird",
            imageUri = "/images/tokens/arn-bird.jpeg",
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
        ),
        TokenPrinting(
            name = "Djinn",
            imageUri = "/images/tokens/arn-djinn.jpeg",
            power = 5,
            toughness = 5,
            colors = emptySet(),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.arn.cards"
}
