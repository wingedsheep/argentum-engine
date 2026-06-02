package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Scarblade Scout
 * {1}{B}
 * Creature — Elf Scout
 * 2/2
 *
 * Lifelink
 * When this creature enters, mill two cards.
 */
val ScarbladeScout = card("Scarblade Scout") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "When this creature enters, mill two cards. (Put the top two cards of your library into your graveyard.)"

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.mill(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2412fea-4591-4345-913f-edc2da9ad975.jpg?1767871946"
    }
}
