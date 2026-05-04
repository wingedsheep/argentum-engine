package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mechan Navigator
 * {1}{U}
 * Artifact Creature — Robot Pilot
 * Whenever this creature becomes tapped, draw a card, then discard a card.
 */
val MechanNavigator = card("Mechan Navigator") {
    manaCost = "{1}{U}"
    typeLine = "Artifact Creature — Robot Pilot"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature becomes tapped, draw a card, then discard a card."

    // Whenever this creature becomes tapped, draw a card, then discard a card
    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = EffectPatterns.loot(draw = 1, discard = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Konstantin Porubov"
        flavorText = "Illvoi temper their curiosity with caution, deploying mechan pilots before they risk mortal scientists."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1fe1d39-42c8-41d0-8bf0-46973e4b07d4.jpg?1752946803"
    }
}
