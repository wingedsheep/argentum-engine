package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Underground Mortuary
 * Land — Swamp Forest
 *
 * ({T}: Add {B} or {G}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val UndergroundMortuary = card("Underground Mortuary") {
    typeLine = "Land — Swamp Forest"
    oracleText = "({T}: Add {B} or {G}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Swamp -> {B}, Forest -> {G})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "271"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6ca59cd-8779-4a84-a54b-e863b79c61f0.jpg?1759236535"
    }
}
