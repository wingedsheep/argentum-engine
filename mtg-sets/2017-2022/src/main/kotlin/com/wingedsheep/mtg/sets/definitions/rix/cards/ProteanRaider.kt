package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Protean Raider
 * {1}{U}{R}
 * Creature — Shapeshifter Pirate
 * 2/2
 * Raid — If you attacked this turn, you may have this creature enter as a copy of any creature on
 * the battlefield.
 */
val ProteanRaider = card("Protean Raider") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Shapeshifter Pirate"
    power = 2
    toughness = 2
    oracleText = "Raid — If you attacked this turn, you may have this creature enter as a copy of any creature on the battlefield."

    replacementEffect(EntersAsCopy(optional = true, condition = Conditions.YouAttackedThisTurn))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Izzy"
        flavorText = "Imitation is the sincerest form of piracy."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41ed9247-16c0-4ca4-9b46-d3da5a375e4c.jpg?1783935271"
    }
}
