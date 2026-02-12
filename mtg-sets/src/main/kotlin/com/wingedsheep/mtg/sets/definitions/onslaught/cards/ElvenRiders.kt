package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptByKeyword

/**
 * Elven Riders
 * {3}{G}{G}
 * Creature — Elf
 * 3/3
 * Elven Riders can't be blocked except by creatures with flying.
 */
val ElvenRiders = card("Elven Riders") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Elf"
    power = 3
    toughness = 3
    oracleText = "Elven Riders can't be blocked except by creatures with flying."

    staticAbility {
        ability = CantBeBlockedExceptByKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Darrell Riche"
        flavorText = "They are seldom combated, and never caught."
        imageUri = "https://cards.scryfall.io/large/front/f/7/f7c1aa30-0271-48d9-b9d0-3b1da26d98bf.jpg?1562953705"
    }
}
