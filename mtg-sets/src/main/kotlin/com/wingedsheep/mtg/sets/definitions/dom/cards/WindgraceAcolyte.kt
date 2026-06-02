package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Windgrace Acolyte
 * {4}{B}
 * Creature — Cat Warrior
 * 3/2
 * Flying
 * When this creature enters, mill three cards and you gain 3 life.
 */
val WindgraceAcolyte = card("Windgrace Acolyte") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cat Warrior"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, mill three cards and you gain 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.mill(3)
            .then(Effects.GainLife(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Bayard Wu"
        flavorText = "Acolytes of the lost Lord Windgrace fight to keep Urborg relics out of Cabal hands."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f835285e-96d9-4574-a497-3b612cae8e22.jpg?1562745894"
    }
}
