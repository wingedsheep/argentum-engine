package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Putrid Raptor
 * {4}{B}{B}
 * Creature — Zombie Dinosaur Beast
 * 4/4
 * Morph—Discard a Zombie card.
 */
val PutridRaptor = card("Putrid Raptor") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie Dinosaur Beast"
    power = 4
    toughness = 4
    oracleText = "Morph—Discard a Zombie card."

    morphCost = PayCost.Discard(GameObjectFilter.Creature.withSubtype("Zombie"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Pete Venters"
        flavorText = "In a world of magic and monsters, no creature truly dies."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9127942b-d73d-42a9-9f97-6a39fa798a8b.jpg?1562532123"
    }
}
