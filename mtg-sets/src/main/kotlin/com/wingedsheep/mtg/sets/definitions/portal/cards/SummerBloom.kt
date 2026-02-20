package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect

/**
 * Summer Bloom
 * {1}{G}
 * Sorcery
 * You may play up to three additional lands this turn.
 */
val SummerBloom = card("Summer Bloom") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"

    spell {
        effect = PlayAdditionalLandsEffect(count = 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Alan Rabinowitz"
        flavorText = "In summer's warmth, the land yields its bounty freely."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e86abcc-272e-4959-90ee-343b9f546ea7.jpg"
    }
}
