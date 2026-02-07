package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.AddAnyColorManaEffect

/**
 * Birchlore Rangers
 * {G}
 * Creature — Elf Druid Ranger
 * 1/1
 * Tap two untapped Elves you control: Add one mana of any color.
 * Morph {G}
 */
val BirchloreRangers = card("Birchlore Rangers") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Druid Ranger"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Elf"))
        effect = AddAnyColorManaEffect(1)
        manaAbility = true
    }

    morph = "{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ce3a3a1-3569-4909-a604-f78d4888781e.jpg?1626726613"
    }
}
