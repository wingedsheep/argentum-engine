package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedWhilePropertyAtMost

/**
 * Tetsuko Umezawa, Fugitive
 * {1}{U}
 * Legendary Creature — Human Rogue
 * 1/3
 * Creatures you control with power or toughness 1 or less can't be blocked.
 *
 * The defaults of [CantBeBlockedWhilePropertyAtMost] are exactly this card: both stats tested, and
 * the ability's controller's creatures as the affected group. Stature, Size Shifter is the same
 * ability narrowed to power-only and `GroupFilter.source()`.
 */
val TetsukoUmezawaFugitive = card("Tetsuko Umezawa, Fugitive") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Rogue"
    power = 1
    toughness = 3
    oracleText = "Creatures you control with power or toughness 1 or less can't be blocked."

    staticAbility {
        ability = CantBeBlockedWhilePropertyAtMost(maxValue = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Randy Vargas"
        flavorText = "\"My ancestor Toshiro used to say, 'Life is a series of choices between bad and worse.' I say it's time to find a third option.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16185c50-f7b8-4cea-a129-dfad8e9df781.jpg?1591605108"
    }
}
