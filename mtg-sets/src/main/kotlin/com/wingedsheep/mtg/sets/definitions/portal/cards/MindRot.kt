package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Mind Rot
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards.
 */
val MindRot = card("Mind Rot") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DiscardCardsEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Brian Snoddy"
        flavorText = "The mind is a fragile thing, easily shattered."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b91d355d-8409-4f0b-87ce-7590a8b9ebc0.jpg"
    }
}
