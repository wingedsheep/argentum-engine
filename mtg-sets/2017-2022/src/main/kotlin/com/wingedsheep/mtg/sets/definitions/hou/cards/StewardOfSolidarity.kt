package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Steward of Solidarity
 * {1}{W} Creature — Human Warrior 2/2
 * {T}, Exert this creature: Create a 1/1 white Warrior creature token with vigilance.
 *
 * HOU's token set has no Warrior token; the 1/1 white vigilance Warrior is Amonkhet's (TAKH).
 */
val StewardOfSolidarity = card("Steward of Solidarity") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "{T}, Exert this creature: Create a 1/1 white Warrior creature token with vigilance. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Exert)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Warrior"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/2/d/2d1446ed-f114-421d-bb60-9aeb655e5adb.jpg?1783936412"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Sara Winters"
        flavorText = "In the midst of disaster, heroic acts became almost ordinary, and ordinary warriors became heroes."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/eda3cd3b-d4c5-4537-8c81-5ccf269f810e.jpg?1783936058"
    }
}
