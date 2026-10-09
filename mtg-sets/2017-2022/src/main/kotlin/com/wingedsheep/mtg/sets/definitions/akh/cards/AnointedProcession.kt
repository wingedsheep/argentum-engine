package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation

/**
 * Anointed Procession
 * {3}{W}
 * Enchantment
 * If an effect would create one or more tokens under your control, it creates twice that many of
 * those tokens instead.
 *
 * Doubling Season's first clause on its own: [MultiplyTokenCreation] with its default
 * "under your control" pattern. Multiple copies stack multiplicatively.
 */
val AnointedProcession = card("Anointed Procession") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText =
        "If an effect would create one or more tokens under your control, it creates twice that many of those tokens instead."

    replacementEffect(MultiplyTokenCreation())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Victor Adame Minguez"
        flavorText = "\"The gods here may walk among the people, but they are not with them.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a52c265-6920-4929-ba0a-70da08df01f1.jpg?1783936544"
        ruling("2017-04-18", "If an effect creates more than one kind of token, it'll create twice as many of each kind. For example, if you cast Bestial Menace while controlling Anointed Procession, you'll create two Snake tokens, two Wolf tokens, and two Elephant tokens.")
        ruling("2017-04-18", "If you control two Anointed Processions, then the number of tokens created is four times the original number. If you control three, then the number of tokens created is eight times the original number, and so on.")
        ruling("2017-04-18", "If the effect creating the tokens instructs you to do something with those tokens at a later time, like exiling them at the end of combat, you'll do that for all the tokens.")
    }
}
