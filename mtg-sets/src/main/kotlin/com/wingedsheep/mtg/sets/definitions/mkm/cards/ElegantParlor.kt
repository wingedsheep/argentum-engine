package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Elegant Parlor
 * Land — Mountain Plains
 *
 * ({T}: Add {R} or {W}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val ElegantParlor = card("Elegant Parlor") {
    typeLine = "Land — Mountain Plains"
    oracleText = "({T}: Add {R} or {W}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Mountain -> {R}, Plains -> {W})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Kamila Szutenberg"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72c6d541-e2cb-4d6e-acac-90a8f53b7006.jpg?1759236525"
    }
}
