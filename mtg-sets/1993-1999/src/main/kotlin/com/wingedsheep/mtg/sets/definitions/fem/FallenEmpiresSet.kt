package com.wingedsheep.mtg.sets.definitions.fem

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Fallen Empires (1994)
 *
 * Complete: all 102 cards. Holds the canonical [CardDefinition]s of cards whose earliest
 * real-expansion printing is Fallen Empires, with later sets contributing reprint [Printing] rows.
 *
 * Set Code: FEM
 * Release Date: November 1, 1994
 */
object FallenEmpiresSet : MtgSet {

    override val code = "FEM"
    override val displayName = "Fallen Empires"
    override val releaseDate = "1994-11-01"

    /**
     * Fallen Empires printed no basic lands of its own and belongs to no block, so its limited
     * environment borrows Portal's — the same fallback The Dark takes.
     */
    override val basicLandsFallback = PortalSet

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Fallen Empires predates token *cards* — there is no `tfem` set on Scryfall to sync from — so
     * the four tokens the set mints borrow the closest matching art from sets that did print one.
     *
     * The fifth, the 1/1 blue Camarid from Homarid Spawning Bed, has never been printed as a token
     * card at all and has no art anywhere on Scryfall, so it has no row here: it resolves through
     * the engine-wide `TokenArt` table instead, which points it at this set's own Homarid.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Citizen",
            imageUri = "https://cards.scryfall.io/normal/front/1/6/165164e7-5693-4d65-b789-8ed8a222365b.jpg?1783933883",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Thrull",
            imageUri = "https://cards.scryfall.io/normal/front/2/7/2795320c-57fc-4140-b306-a92237390353.jpg?1783942443",
            power = 0,
            toughness = 1,
            colors = setOf(Color.BLACK),
        ),
        TokenPrinting(
            name = "Saproling",
            imageUri = "https://cards.scryfall.io/normal/front/6/9/698d48fa-d6c1-44b9-8aa2-bb03e0e53788.jpg?1783943085",
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
        ),
        TokenPrinting(
            name = "Goblin",
            imageUri = "https://cards.scryfall.io/normal/front/e/9/e9577d3c-ee19-4b53-adac-b304287a066f.jpg?1783943086",
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fem.cards"
}
