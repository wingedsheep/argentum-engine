package com.wingedsheep.mtg.sets.definitions.jud

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Judgment (2002)
 *
 * The third and final set in the Odyssey block, set on the plane of Otaria.
 *
 * Set Code: JUD
 * Release Date: May 27, 2002
 */
object JudgmentSet : MtgSet {

    override val code = "JUD"
    override val displayName = "Judgment"
    override val releaseDate = "2002-05-27"
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
     * Judgment predates token *cards* — Scryfall has no `tjud` set to sync from — so its
     * token art is self-hosted under `web-client/public/images/tokens/` and declared here, the
     * same route Invasion, Apocalypse and Odyssey take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Wurm",
            imageUri = "/images/tokens/jud-wurm.jpeg",
            power = 6,
            toughness = 6,
            colors = setOf(Color.GREEN),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.jud.cards"
}
