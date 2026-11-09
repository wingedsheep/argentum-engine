package com.wingedsheep.mtg.sets.definitions.emn

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
 * Eldritch Moon (2016)
 *
 * Set Code: EMN
 * Release Date: 2016-07-22
 */
object EldritchMoonSet : MtgSet {

    override val code = "EMN"
    override val displayName = "Eldritch Moon"
    override val releaseDate = "2016-07-22"
    override val block = "Shadows over Innistrad"

    /** Eldritch Moon printed no basic lands; its boosters carried Shadows over Innistrad's. */
    override val basicLandsFallbackCode = "SOI"

    /**
     * Draft Booster: 8 single-faced commons, 3 single-faced uncommons and a single-faced rare or mythic, a
     * double-faced-or-meld common or uncommon, and one more slot that is a single-faced common in seven packs
     * in eight and a double-faced-or-meld rare or mythic in the eighth. The double-faced/meld sheet prints each
     * common 5 times and each uncommon twice (so a pack's sheet card is a common half the time), and each rare
     * twice and each mythic once. The basic land (Shadows over Innistrad's — see [basicLandsFallbackCode]) is
     * supplied at deck building, and the traditional foil that replaced a common in 9 packs in 40 is not
     * modelled. Figures from taw/magic-search-engine `data/boosters/emn/draft.yaml`, after lethe.xyz's EMN
     * collation notes; the 1-in-8 rare double-faced slot matches the MTR pack distribution MTGGoldfish cites.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        val doubleFacedSheet = SheetSelector.Named(DOUBLE_FACED_SHEET)
        DedicatedSlotBooster(
            base = StandardBooster(commons = 8, uncommons = 3),
            slots = listOf(
                DedicatedSlot(doubleFacedSheet, mapOf(Rarity.COMMON to 4.0 * 5, Rarity.UNCOMMON to 10.0 * 2)),
                DedicatedSlot(
                    doubleFacedSheet,
                    mapOf(Rarity.COMMON to 7.0, Rarity.RARE to 10.0 / 12, Rarity.MYTHIC to 2.0 / 12),
                    ordinaryRarities = setOf(Rarity.COMMON),
                ),
            ),
        )
    }

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

    /** Eldritch Moon's 15 transforming double-faced cards and 6 meld cards, which shared their own print sheet. */
    val DOUBLE_FACED_SHEET: Set<String> = setOf(
        // Commons
        "Ulvenwald Captive", "Vildin-Pack Outcast", "Graf Rats", "Midnight Scavengers",
        // Uncommons
        "Conduit of Storms", "Cryptolith Fragment", "Curious Homunculus", "Extricator of Sin", "Grizzled Angler",
        "Kessig Prowler", "Lone Rider", "Shrill Howler", "Smoldering Werewolf", "Tangleclaw Werewolf",
        // Rares and mythics
        "Docent of Perfection", "Voldaren Pariah", "Bruna, the Fading Light", "Hanweir Battlements",
        "Hanweir Garrison", "Ulrich of the Krallenhorde", "Gisela, the Broken Blade",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.emn.cards"
}
