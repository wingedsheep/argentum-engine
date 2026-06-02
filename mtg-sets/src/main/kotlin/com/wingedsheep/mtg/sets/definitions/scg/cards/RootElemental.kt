package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Root Elemental
 * {4}{G}{G}
 * Creature — Elemental
 * 6/5
 * Morph {5}{G}{G}
 * When this creature is turned face up, you may put a creature card from your hand
 * onto the battlefield.
 */
val RootElemental = card("Root Elemental") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 5
    oracleText = "Morph {5}{G}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, you may put a creature card from your hand onto the battlefield."

    morph = "{5}{G}{G}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = HandPatterns.putFromHand(
            filter = GameObjectFilter.Creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5161968e-b757-45b8-826f-98415291024d.jpg?1562529053"
    }
}
