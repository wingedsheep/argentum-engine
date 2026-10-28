package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity

/**
 * Springmantle Cleric {4}{G} — Zendikar Rising #205
 * Creature — Elf Cleric 2/3
 *
 * "This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."
 *
 * Unlabelled converge: modelled via [convergeEntersWithCounters], i.e.
 * `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`.
 */
val SpringmantleCleric = card("Springmantle Cleric") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Cleric"
    power = 2
    toughness = 3
    oracleText = "This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."

    convergeEntersWithCounters()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d822730-6d81-4dbc-a3c3-ce96d29a1078.jpg?1783929332"
    }
}
