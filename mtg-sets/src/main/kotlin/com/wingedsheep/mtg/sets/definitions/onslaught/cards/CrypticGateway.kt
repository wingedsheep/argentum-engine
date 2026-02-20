package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PutCreatureFromHandSharingTypeWithTappedEffect

/**
 * Cryptic Gateway
 * {5}
 * Artifact
 * Tap two untapped creatures you control: You may put a creature card from your hand
 * that shares a creature type with each creature tapped this way onto the battlefield.
 */
val CrypticGateway = card("Cryptic Gateway") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "Tap two untapped creatures you control: You may put a creature card from your hand that shares a creature type with each creature tapped this way onto the battlefield."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature)
        effect = PutCreatureFromHandSharingTypeWithTappedEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "306"
        artist = "Mark Tedin"
        flavorText = "\"Its lock changes to fit each key.\""
        imageUri = "https://cards.scryfall.io/large/front/7/f/7f379966-6a0a-434c-8682-1cf528a9a4a1.jpg?1562925013"
    }
}
