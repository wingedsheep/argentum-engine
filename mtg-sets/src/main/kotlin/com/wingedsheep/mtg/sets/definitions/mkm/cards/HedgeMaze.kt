package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Hedge Maze
 * Land — Forest Island
 *
 * ({T}: Add {G} or {U}.)
 * This land enters tapped.
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val HedgeMaze = card("Hedge Maze") {
    typeLine = "Land — Forest Island"
    oracleText = "({T}: Add {G} or {U}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Forest -> {G}, Island -> {U})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5260f8ae-805b-4eae-badf-62de0f768867.jpg?1759236526"
    }
}
