package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter

/**
 * Krosan Drover
 * {3}{G}
 * Creature — Elf
 * 2/2
 * Creature spells you cast with mana value 6 or greater cost {2} less to cast.
 */
val KrosanDrover = card("Krosan Drover") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "Creature spells you cast with mana value 6 or greater cost {2} less to cast."

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter.Creature.manaValueAtLeast(6),
            amount = 2
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Arnie Swekel"
        flavorText = "\"Sit.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e92a7141-119f-4bf8-a82d-eb7c0c37185c.jpg?1562536633"
    }
}
