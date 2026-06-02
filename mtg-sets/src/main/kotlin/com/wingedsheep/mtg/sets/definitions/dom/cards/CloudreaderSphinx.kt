package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Cloudreader Sphinx
 * {4}{U}
 * Creature — Sphinx
 * 3/4
 * Flying
 * When this creature enters, scry 2.
 */
val CloudreaderSphinx = card("Cloudreader Sphinx") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, scry 2."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c85a696-fa1a-4e05-b0aa-79b3381e849a.jpg?1562737341"
    }
}
