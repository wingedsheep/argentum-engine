package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Steel Leaf Champion
 * {G}{G}{G}
 * Creature — Elf Knight
 * 5/4
 * Steel Leaf Champion can't be blocked by creatures with power 2 or less.
 */
val SteelLeafChampion = card("Steel Leaf Champion") {
    manaCost = "{G}{G}{G}"
    typeLine = "Creature — Elf Knight"
    power = 5
    toughness = 4
    oracleText = "Steel Leaf Champion can't be blocked by creatures with power 2 or less."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Chris Rahn"
        flavorText = "\"I am the shield of Llanowar, and I say this forest is not yours to claim.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24d8a688-79d4-49b9-ab0c-c7f5c9b551f4.jpg?1562732792"
    }
}
