package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Meticulous Archive
 * Land — Plains Island
 *
 * ({T}: Add {W} or {U}.)
 * This land enters tapped.
 * When this land enters, surveil 1.
 */
val MeticulousArchive = card("Meticulous Archive") {
    typeLine = "Land — Plains Island"
    oracleText = "({T}: Add {W} or {U}.)\nThis land enters tapped.\nWhen this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // Mana abilities are intrinsic from basic land types (Plains -> {W}, Island -> {U})

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/652236c2-84ef-45e4-b5fc-ed6170bc3d6c.jpg?1759236529"
    }
}
