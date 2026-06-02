package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Mind Rot
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards.
 */
val MindRot = card("Mind Rot") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetOpponent())
        effect = HandPatterns.discardCards(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Brian Snoddy"
        flavorText = "The mind is a fragile thing, easily shattered."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b91d355d-8409-4f0b-87ce-7590a8b9ebc0.jpg"
    }
}
