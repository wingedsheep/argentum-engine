package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Daru Lancer
 * {4}{W}{W}
 * Creature — Human Soldier
 * 3/4
 * First strike
 * Morph {2}{W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 */
val DaruLancer = card("Daru Lancer") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4

    keywords(Keyword.FIRST_STRIKE)
    morph = "{2}{W}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Brian Snõddy"
        flavorText = "Although the Order frowned upon his preparations for the pits, behind closed doors most saw the fights as a necessary evil."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd888ca8-0ebe-46f0-9317-3b193ccc43fb.jpg?1562943531"
    }
}
