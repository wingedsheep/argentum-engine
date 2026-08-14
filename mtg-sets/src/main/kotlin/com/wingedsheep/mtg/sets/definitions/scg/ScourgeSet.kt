package com.wingedsheep.mtg.sets.definitions.scg

import com.wingedsheep.mtg.sets.definitions.ons.OnslaughtSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Scourge Set (2003)
 *
 * Scourge was the third and final set in the Onslaught block, featuring
 * the Storm mechanic and heavy tribal themes including Dragons.
 *
 * Set Code: SCG
 * Release Date: May 26, 2003
 * Card Count: 143
 */
object ScourgeSet : MtgSet {

    override val code = "SCG"
    override val displayName = "Scourge"
    override val releaseDate = "2003-05-26"
    override val block = "Onslaught"
    override val basicLandsFallback = OnslaughtSet
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    /**
     * Scourge predates token *cards* — Scryfall has no `tscg` set to sync from — so its
     * token art is self-hosted under `web-client/public/images/tokens/` and declared here, the
     * same route Invasion, Apocalypse and Odyssey take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Beast",
            imageUri = "/images/tokens/scg-beast.jpeg",
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.scg.cards"
}
