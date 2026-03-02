package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodfire Mentor
 * {2}{R}
 * Creature — Efreet Shaman
 * 0/5
 * {2}{U}, {T}: Draw a card, then discard a card.
 */
val BloodfireMentor = card("Bloodfire Mentor") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Efreet Shaman"
    power = 0
    toughness = 5
    oracleText = "{2}{U}, {T}: Draw a card, then discard a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.Loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Chase Stone"
        flavorText = "The adept underwent months of preparation to withstand pain, until he was finally ready to receive the efreet master's teachings."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4836fb70-a8f3-44ed-bae4-890ef7d07f88.jpg?1562786023"
    }
}
