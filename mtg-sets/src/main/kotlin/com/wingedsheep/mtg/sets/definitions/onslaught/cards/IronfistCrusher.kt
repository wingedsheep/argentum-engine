package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber

/**
 * Ironfist Crusher
 * {4}{W}
 * Creature — Human Soldier
 * 2/4
 * Ironfist Crusher can block any number of creatures.
 * Morph {3}{W} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val IronfistCrusher = card("Ironfist Crusher") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4

    staticAbility {
        ability = CanBlockAnyNumber()
    }

    morph = "{3}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Iain McCaig"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7284e32-de54-4c83-a7de-7b249c47319a.jpg?1562942046"
    }
}
