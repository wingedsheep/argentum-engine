package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Raucous Theater
 * Land — Swamp Mountain
 *
 * ({T}: Add {B} or {R}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val RaucousTheater = card("Raucous Theater") {
    typeLine = "Land — Swamp Mountain"
    oracleText = "({T}: Add {B} or {R}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Swamp -> {B}, Mountain -> {R})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "266"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b598c93e-dae1-4d71-a9e4-917abf76d2d0.jpg?1759236530"
    }
}
