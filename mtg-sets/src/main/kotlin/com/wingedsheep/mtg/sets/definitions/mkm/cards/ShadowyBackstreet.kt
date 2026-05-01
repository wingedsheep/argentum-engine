package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Shadowy Backstreet
 * Land — Plains Swamp
 *
 * ({T}: Add {W} or {B}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val ShadowyBackstreet = card("Shadowy Backstreet") {
    typeLine = "Land — Plains Swamp"
    oracleText = "({T}: Add {W} or {B}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Plains -> {W}, Swamp -> {B})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c1b656-1d67-499c-bf0f-417682a86c7d.jpg?1759236532"
    }
}
