package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Mind Rot
 * {2}{B}
 * Sorcery
 * Target player discards two cards.
 */
val MindRot = card("Mind Rot") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetPlayer()
        effect = DiscardCardsEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Brian Snoddy"
        flavorText = "The mind is a fragile thing, easily shattered."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81c7c75b-2c3c-4c93-8a8f-a21d0f14b9e7.jpg"
    }
}
