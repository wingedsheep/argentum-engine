package com.wingedsheep.mtg.sets.definitions.dka

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DedicatedSlot
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.SheetSelector
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Dark Ascension (2012)
 *
 * Set Code: DKA
 * Release Date: 2012-02-03
 */
object DarkAscensionSet : MtgSet {

    override val code = "DKA"
    override val displayName = "Dark Ascension"
    override val releaseDate = "2012-02-03"
    override val block = "Innistrad"

    /** Dark Ascension printed no basic lands; its boosters carried Innistrad's. */
    override val basicLandsFallbackCode = "ISD"

    /**
     * Draft Booster: 9 single-faced commons, 3 single-faced uncommons, a single-faced rare or mythic, and a
     * double-faced card of any rarity in its own slot (it did not replace a common). The double-faced sheet
     * prints each common 12 times, each uncommon 6 times, each rare twice and each mythic once — with 4 commons,
     * 4 uncommons, 3 rares and 2 mythics that is 48 : 24 : 6 : 2. The basic land (Innistrad's — see
     * [basicLandsFallbackCode]) is supplied at deck building, and the traditional foil that replaced a common in
     * 9 packs in 40 is not modelled. Figures from taw/magic-search-engine `data/boosters/dka/draft.yaml`.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        DedicatedSlotBooster(
            base = StandardBooster(commons = 9, uncommons = 3),
            slots = listOf(
                DedicatedSlot(
                    SheetSelector.DoubleFaced,
                    mapOf(
                        Rarity.COMMON to 4.0 * 12,
                        Rarity.UNCOMMON to 4.0 * 6,
                        Rarity.RARE to 3.0 * 2,
                        Rarity.MYTHIC to 2.0 * 1,
                    ),
                ),
            ),
        )
    }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Scryfall's `tdka` sync brings back only Dark Ascension's Human and Vampire, so Lingering
     * Souls' Spirit had no art of its own. Self-hosted under `web-client/public/images/tokens/`.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Spirit",
            imageUri = "/images/tokens/dka-spirit.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dka.cards"
}
