package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightshell Duo
 * {3}{U}
 * Creature — Rat Otter
 * 3/4
 * Prowess
 * When this creature enters, surveil 2.
 */
val LightshellDuo = card("Lightshell Duo") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Rat Otter"
    oracleText = "Prowess\nWhen this creature enters, surveil 2."
    power = 3
    toughness = 4

    prowess()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Mariah Tekulve"
        flavorText = "\"Lightning and snail as one!\" cried the rat. \"It's so beautiful!\" the otter gasped in awe."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f00c834-b4a9-45b8-bb3f-22c2a42314a0.jpg?1721426129"
    }
}
