package com.wingedsheep.mtg.sets.definitions.c19

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

object Commander2019Set : MtgSet {
    override val code = "C19"
    override val displayName = "Commander 2019"
    override val releaseDate = "2019-08-23"
    override val sealedSupported = false
    override val incomplete = true
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    override val tokenArt: List<TokenPrinting> = listOf(
        // tc19 #26 — Idol of Oblivion's 10/10 colorless Eldrazi.
        TokenPrinting(
            name = "Eldrazi",
            imageUri = "https://cards.scryfall.io/normal/front/c/9/c9854bcf-8729-45b9-9ea3-c43898e4fe5f.jpg?1783932823",
            power = 10,
            toughness = 10,
        ),
    )
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.c19.cards"
}
