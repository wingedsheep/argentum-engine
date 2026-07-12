package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Selhoff Entomber
 * {1}{U}
 * Creature — Zombie
 * 1/3
 *
 * {T}, Discard a creature card: Draw a card.
 */
val SelhoffEntomber = card("Selhoff Entomber") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 3
    oracleText = "{T}, Discard a creature card: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Discard(GameObjectFilter.Creature))
        effect = Effects.DrawCards(1)
        description = "{T}, Discard a creature card: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Johann Bodin"
        flavorText = "It knows one fundamental truth: bodies belong in graves."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8805db2-5a26-43cf-9b74-f882c283e5e4.jpg?1782703138"
    }
}
