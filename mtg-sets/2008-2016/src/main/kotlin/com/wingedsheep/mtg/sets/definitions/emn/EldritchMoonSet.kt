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
 * Every card is implemented (canonicals here, reprints as `Printing` rows). Not yet draftable:
 * archetypes and the booster strategy land with the set's finishing PR.
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
     * `web-client/public/images/tokens/` and declared here. Thraben Standard Bearer's white 1/1
     * Human Soldier isn't on the `temn` sheet either; it borrows Shadows over Innistrad's token art.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Spirit",
            imageUri = "/images/tokens/emn-spirit.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Human Soldier",
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9cbf36e-4044-4f08-9bae-f0dcb2455716.jpg?1783937683",
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.emn.cards"
}
