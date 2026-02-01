package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Warrior
 * {G}{G}
 * Creature — Elf Warrior
 * 2/3
 */
val ElvishWarrior = card("Elvish Warrior") {
    manaCost = "{G}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Christopher Moeller"
        flavorText = "\"My tales of war are the stories most asked for around the fires at night, but they're the ones I care least to tell.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d0485a-209d-4040-94ab-856bdee83b81.jpg"
    }
}
