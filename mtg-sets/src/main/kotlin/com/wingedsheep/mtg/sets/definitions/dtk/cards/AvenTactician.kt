package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Aven Tactician
 * {4}{W}
 * Creature — Bird Soldier
 * 2/3
 *
 * Flying
 * When this creature enters, bolster 1. (Choose a creature with the least toughness
 * among creatures you control and put a +1/+1 counter on it.)
 */
val AvenTactician = card("Aven Tactician") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, bolster 1. (Choose a creature with the least toughness " +
        "among creatures you control and put a +1/+1 counter on it.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MiscPatterns.bolster(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9dfc3ec-ca06-40a6-b77c-c2432af9802f.jpg?1562792120"
    }
}
