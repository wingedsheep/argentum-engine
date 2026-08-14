package com.wingedsheep.mtg.sets.definitions.mom

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * March of the Machine (2023)
 *
 * Set Code: MOM
 * Release Date: April 21, 2023
 */
object MarchOfTheMachineSet : MtgSet {

    override val code = "MOM"
    override val displayName = "March of the Machine"
    override val releaseDate = "2023-04-21"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Scryfall's `tmom` sync has no Incubator row — the transforming double-faced token from
     * CR 701.51 — so it is self-hosted under `web-client/public/images/tokens/`. Its back face
     * (Phyrexian) is not set-scoped art: a token keeps the image its *face* declares when it
     * transforms, so that side lives on `PredefinedTokens.Phyrexian`.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(name = "Incubator", imageUri = "/images/tokens/mom-incubator.jpeg"),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mom.cards"
}
