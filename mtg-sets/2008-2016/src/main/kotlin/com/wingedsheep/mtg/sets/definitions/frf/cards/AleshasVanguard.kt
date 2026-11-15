package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alesha's Vanguard
 * {3}{B}
 * Creature — Orc Warrior
 * 3/3
 * Dash {2}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 */
val AleshasVanguard = card("Alesha's Vanguard") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    power = 3
    toughness = 3
    oracleText = "Dash {2}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{2}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Torstein Nordstrand"
        flavorText = "Boundaries drawn on maps or marked with stone have no meaning for the Mardu."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69bb135f-1c58-42ed-bbf2-4a2e842f99d7.jpg"
    }
}
