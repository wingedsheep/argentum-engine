package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thallid Soothsayer
 * {3}{B}
 * Creature — Fungus
 * 2/3
 * {2}, Sacrifice a creature: Draw a card.
 */
val ThallidSoothsayer = card("Thallid Soothsayer") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Fungus"
    power = 2
    toughness = 3
    oracleText = "{2}, Sacrifice a creature: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Jason A. Engle"
        flavorText = "\"Some of the thallids that escaped into the fens of Urborg began emulating the Cabal's bloodsoaked rituals in their own peculiar way.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/204173b6-ae51-49ca-bd67-0703e882bf9e.jpg?1562732458"
    }
}
