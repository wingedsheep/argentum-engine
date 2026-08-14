package com.wingedsheep.mtg.sets.definitions.big

import com.wingedsheep.mtg.sets.definitions.otj.OutlawsOfThunderJunctionSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

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

    // A 30-card bonus sheet can't sustain a sealed/draft pool by itself — it is only playable
    // together with at least one regular set.
    override val extensionSet = true

    // The bonus sheet was opened inside OTJ boosters and shares its token sheet, so `tbig` lists
    // only the tokens unique to BIG — the Clue and Treasure its cards mint are OTJ tokens.
    override val tokenArt: List<TokenPrinting> by lazy {
        TokenArtData.borrowedFrom(OutlawsOfThunderJunctionSet.code, code)
    }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.big.cards"
}
