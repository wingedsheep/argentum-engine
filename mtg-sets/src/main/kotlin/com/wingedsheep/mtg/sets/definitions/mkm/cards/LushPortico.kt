package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Lush Portico
 * Land — Forest Plains
 *
 * ({T}: Add {G} or {W}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val LushPortico = card("Lush Portico") {
    typeLine = "Land — Forest Plains"
    oracleText = "({T}: Add {G} or {W}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Forest -> {G}, Plains -> {W})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "263"
        artist = "Kamila Szutenberg"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c17816e8-28b1-4295-a637-efb0e5c18873.jpg?1759236528"
    }
}
