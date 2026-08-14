package com.wingedsheep.mtg.sets.definitions.j22

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Jumpstart 2022
 *
 * Set Code: J22
 */
object Jumpstart2022Set : MtgSet {

    override val code = "J22"
    override val displayName = "Jumpstart 2022"
    override val releaseDate = "2022-12-02"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Our Scryfall sync returns no `tj22` rows, so Jumpstart 2022's token art is self-hosted
     * under `web-client/public/images/tokens/` and declared here.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Zombie",
            imageUri = "/images/tokens/j22-zombie.jpeg",
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.j22.cards"
}
