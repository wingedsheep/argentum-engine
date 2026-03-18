package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Zahid, Djinn of the Lamp
 * {4}{U}{U}
 * Legendary Creature — Djinn
 * 5/6
 * You may pay {3}{U} and tap an untapped artifact you control rather than pay this spell's mana cost.
 * Flying
 */
val ZahidDjinnOfTheLamp = card("Zahid, Djinn of the Lamp") {
    manaCost = "{4}{U}{U}"
    typeLine = "Legendary Creature — Djinn"
    power = 5
    toughness = 6
    oracleText = "You may pay {3}{U} and tap an untapped artifact you control rather than pay this spell's mana cost.\nFlying"

    keywords(Keyword.FLYING)

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = "{3}{U}",
        additionalCosts = listOf(
            AdditionalCost.TapPermanents(count = 1, filter = GameObjectFilter.Artifact)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Magali Villeneuve"
        flavorText = "\"I do as I please, little mortal. Do go on about your wishes, though—they amuse me to no end.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/303ea0c1-cd97-4160-87df-646ad763f5fc.jpg?1562911270"
    }
}
