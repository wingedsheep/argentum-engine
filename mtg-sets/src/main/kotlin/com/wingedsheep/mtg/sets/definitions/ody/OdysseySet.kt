package com.wingedsheep.mtg.sets.definitions.ody

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Odyssey (2001)
 *
 * Set Code: ODY
 * Release Date: October 1, 2001
 */
object OdysseySet : MtgSet {

    override val code = "ODY"
    override val displayName = "Odyssey"
    override val releaseDate = "2001-10-01"
    override val block = "Odyssey"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Odyssey predates token *cards* — Scryfall has no `tody` set to sync from — so its token art
     * is self-hosted under `web-client/public/images/tokens/` and declared here, the same route
     * Invasion and Apocalypse take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Beast",
            imageUri = "/images/tokens/ody-beast.jpeg",
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
        ),
        TokenPrinting(
            name = "Elephant",
            imageUri = "/images/tokens/ody-elephant.jpeg",
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
        ),
        TokenPrinting(
            name = "Squirrel",
            imageUri = "/images/tokens/ody-squirrel.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
        ),
        TokenPrinting(
            name = "Wurm",
            imageUri = "/images/tokens/ody-wurm.jpeg",
            power = 6,
            toughness = 6,
            colors = setOf(Color.GREEN),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ody.cards"
}
