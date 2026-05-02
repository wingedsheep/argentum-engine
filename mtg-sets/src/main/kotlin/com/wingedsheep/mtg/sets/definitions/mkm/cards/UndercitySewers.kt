package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Undercity Sewers
 * Land — Island Swamp
 *
 * ({T}: Add {U} or {B}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val UndercitySewers = card("Undercity Sewers") {
    typeLine = "Land — Island Swamp"
    oracleText = "({T}: Add {U} or {B}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Island -> {U}, Swamp -> {B})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b5801fb-2026-4f25-98bc-ebb2f99684b9.jpg?1759236533"
    }
}
