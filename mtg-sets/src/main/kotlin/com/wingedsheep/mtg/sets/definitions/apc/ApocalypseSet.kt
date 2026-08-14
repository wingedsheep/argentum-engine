package com.wingedsheep.mtg.sets.definitions.apc

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Apocalypse (2001)
 *
 * Set Code: APC
 * Release Date: 2001-06-04
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object ApocalypseSet : MtgSet {

    override val code = "APC"
    override val displayName = "Apocalypse"
    override val releaseDate = "2001-06-04"
    override val block = "Invasion"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Apocalypse predates token *cards* — Scryfall has no `tapc` set to sync from — so its token art
     * is self-hosted under `web-client/public/images/tokens/` and declared here, the same route
     * Invasion takes.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Cat",
            imageUri = "/images/tokens/apc-cat.jpeg",
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
        ),
        TokenPrinting(
            name = "Goblin Soldier",
            imageUri = "/images/tokens/apc-goblin-soldier.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.RED),
        ),
        TokenPrinting(
            name = "Kavu",
            imageUri = "/images/tokens/apc-kavu.jpeg",
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLACK),
        ),
        TokenPrinting(
            name = "Wurm",
            imageUri = "/images/tokens/apc-wurm.jpeg",
            power = 6,
            toughness = 6,
            colors = setOf(Color.BLACK),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.apc.cards"
}
