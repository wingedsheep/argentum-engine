package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Flamekin Gildweaver
 * {3}{R}
 * Creature — Elemental Sorcerer
 * 4/3
 *
 * Trample
 * When this creature enters, create a Treasure token.
 */
val FlamekinGildweaver = card("Flamekin Gildweaver") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Elemental Sorcerer"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Aurore Folny"
        flavorText = "Few flamekin have the patience necessary to weave goldenglow moth silk. Those who do are highly revered, and highly rewarded."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1628ece-a028-49d4-9065-ee997838c20a.jpg?1767862536"
    }
}
