package com.wingedsheep.mtg.sets.definitions.aer

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Aether Revolt (2017)
 *
 * Every card the set prints is implemented: originals as canonicals here, reprints as
 * `Printing` rows.
 *
 * Set Code: AER
 * Release Date: 2017-01-20
 */
object AetherRevoltSet : MtgSet {

    override val code = "AER"
    override val displayName = "Aether Revolt"
    override val releaseDate = "2017-01-20"
    override val block = "Kaladesh"

    /**
     * Draft booster: ten commons, three uncommons and one rare or mythic (the fifteenth slot is a
     * token or basic land, supplied at deck building; the foil or Kaladesh Inventions masterpiece that
     * replaces a common is cosmetic and not modelled). The rare sheet prints each of the 42 rares
     * twice and each of the 12 mythics once, so a pack's rare slot is mythic 12 times in 96. The
     * Planeswalker Deck cards (#185–194) are `inBooster = false`, or reprint rows flagged the same.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 12.0 / 96)

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
     * Aether Revolt's token set (TAER) has no Servo: the set's Servo makers mint Kaladesh's
     * Servo token (TKLD), so that printing stands in for the set's own art.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Servo",
            imageUri = "https://cards.scryfall.io/normal/front/6/0/60842b1a-6ae7-4b3b-a23f-0d94a3d89884.jpg?1783937239",
            power = 1,
            toughness = 1,
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.aer.cards"
}
