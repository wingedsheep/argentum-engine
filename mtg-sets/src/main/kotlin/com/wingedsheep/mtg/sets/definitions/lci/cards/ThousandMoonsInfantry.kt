package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps

/**
 * Thousand Moons Infantry
 * {2}{W}
 * Creature — Human Soldier
 * Untap this creature during each other player's untap step.
 */
val ThousandMoonsInfantry = card("Thousand Moons Infantry") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    oracleText = "Untap this creature during each other player's untap step."
    power = 2
    toughness = 4

    staticAbility {
        ability = UntapSelfDuringOtherUntapSteps
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Manuel Castañón"
        flavorText = "The Thousand Moons train and fight in tight-knit squads of ten. Every tenth Moon functions as an officer, every hundredth as a captain, and the thousandth, Anim Pakal, as supreme commander."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1974a8c-3328-4ff5-9a00-cb79ebb8ccf6.jpg?1782694580"
    }
}
