package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Search Warrant
 * {W}{U}
 * Sorcery
 * Target player reveals their hand. You gain life equal to the number of cards in that player's hand.
 *
 * Inquisition's shape with the payoff pointed at you: reveal, then count the target's hand at
 * resolution and gain that much life. Targeting yourself is legal and counts your own hand.
 */
val SearchWarrant = card("Search Warrant") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. You gain life equal to the number of cards in that player's hand."

    spell {
        val suspect = target(Targets.Player)
        effect = Effects.RevealHand(suspect) then
            Effects.GainLife(DynamicAmounts.count(suspect.asPlayer, Zone.HAND))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Steven Belledin"
        flavorText = "\"And would you care to explain why your satchel is radiating an aura of unstable energy?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d55f625a-6e9e-40ba-ae46-cf6bafc0a41b.jpg?1783940332"
    }
}
