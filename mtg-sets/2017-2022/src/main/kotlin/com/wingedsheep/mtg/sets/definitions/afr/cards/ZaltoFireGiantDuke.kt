package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zalto, Fire Giant Duke
 * {3}{R}{R}
 * Legendary Creature — Giant Barbarian
 * 7/3
 * Trample
 * Whenever Zalto is dealt damage, venture into the dungeon. (Enter the first room or advance to the next room.)
 */
val ZaltoFireGiantDuke = card("Zalto, Fire Giant Duke") {
    manaCost = "{3}{R}{R}"
    typeLine = "Legendary Creature — Giant Barbarian"
    power = 7
    toughness = 3
    oracleText = "Trample\nWhenever Zalto is dealt damage, venture into the dungeon. (Enter the first room or advance to the next room.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Zezhou Chen"
        flavorText = "Fire giants love to fight on a large scale, and Duke Zalto is one of the greatest warmongers of his age."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c6da066-accb-4c09-9acb-4de263dadad3.jpg?1783926468"
        ruling("2021-07-23", "If Zalto is dealt lethal damage, you will still get to venture into the dungeon.")
        ruling("2021-07-23", "If Zalto is dealt damage by multiple sources at the same time (usually creatures blocking it), you will only venture into the dungeon once.")
    }
}
