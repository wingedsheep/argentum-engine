package com.wingedsheep.mtg.sets.definitions.emn

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Eldritch Moon (2016)
 *
 * mtgish-tooling auto-generated seed: only the cards relocated here as their canonical
 * earliest printing. Intentionally incomplete relative to the official set.
 *
 * Set Code: EMN
 * Release Date: 2016-07-22
 */
object EldritchMoonSet : MtgSet {

    override val code = "EMN"
    override val displayName = "Eldritch Moon"
    override val releaseDate = "2016-07-22"
    override val block = "Shadows over Innistrad"
    override val sealedSupported = false
    override val incomplete = true

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
     * Scryfall's `temn` sync covers Eldritch Moon's Eldrazi Horror, Human, Zombie and Spider but
     * not Haunted Dead's Spirit, so that one is self-hosted under
     * `web-client/public/images/tokens/` and declared here.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Spirit",
            imageUri = "/images/tokens/emn-spirit.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.emn.cards"
}
