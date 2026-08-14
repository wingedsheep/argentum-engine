package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gisela, the Broken Blade
 * {2}{W}{W}
 * Legendary Creature — Angel Horror
 * 4/3
 *
 * Flying, first strike, lifelink
 *
 * Meld is not yet supported by the engine. As with Bruna, the Fading Light and the other
 * Eldritch Moon meld cards, the printed meld trigger remains in [oracleText] but is not wired.
 */
val GiselaTheBrokenBlade = card("Gisela, the Broken Blade") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel Horror"
    oracleText = "Flying, first strike, lifelink\n" +
        "At the beginning of your end step, if you both own and control Gisela and a creature " +
        "named Bruna, the Fading Light, exile them, then meld them into Brisela, Voice of Nightmares."
    power = 4
    toughness = 3

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "28"
        artist = "Clint Cearley"
        flavorText = "She now hears only Emrakul's murmurs."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c75c035a-7da9-4b36-982d-fca8220b1797.jpg?1783937515"
    }
}
