package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Howling Fury
 * {2}{B}
 * Sorcery
 * Target creature gets +4/+0 until end of turn.
 */
val HowlingFury = card("Howling Fury") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = ModifyStatsEffect(
            powerModifier = 4,
            toughnessModifier = 0,
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Clyde Caldwell"
        flavorText = "The rage of the cursed can be harnessed but never controlled."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/821de8a5-2c8f-4a8d-a7a2-df7b2e2f7a9e.jpg"
    }
}
