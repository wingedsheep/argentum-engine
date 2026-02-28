package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.IncreaseMorphCost

/**
 * Exiled Doomsayer
 * {1}{W}
 * Creature — Human Cleric
 * 1/2
 * All morph costs cost {2} more.
 * (This doesn't affect the cost to cast creature spells face down.)
 */
val ExiledDoomsayer = card("Exiled Doomsayer") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric"
    oracleText = "All morph costs cost {2} more. (This doesn't affect the cost to cast creature spells face down.)"
    power = 1
    toughness = 2

    staticAbility {
        ability = IncreaseMorphCost(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Brian Snõddy"
        flavorText = "He's desperate to hold back the future because he has seen it."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74aaf095-143a-43fc-a858-b1e82a4b906e.jpg?1562530656"
    }
}
