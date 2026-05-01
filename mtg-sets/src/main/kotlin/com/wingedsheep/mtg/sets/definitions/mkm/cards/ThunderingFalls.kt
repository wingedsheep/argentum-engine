package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Thundering Falls
 * Land — Island Mountain
 *
 * ({T}: Add {U} or {R}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val ThunderingFalls = card("Thundering Falls") {
    typeLine = "Land — Island Mountain"
    oracleText = "({T}: Add {U} or {R}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Island -> {U}, Mountain -> {R})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "269"
        artist = "Grady Frederick"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17260fff-b239-4af4-9306-3236ae3fa5a5.jpg?1759236532"
    }
}
