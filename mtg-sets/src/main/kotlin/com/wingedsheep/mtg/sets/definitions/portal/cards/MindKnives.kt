package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Mind Knives
 * {1}{B}
 * Sorcery
 * Target opponent discards a card at random.
 */
val MindKnives = card("Mind Knives") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetOpponent())
        effect = Effects.DiscardRandom(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Andrew Robinson"
        flavorText = "The mind can cut deeper than any blade."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d17e23a6-6313-416d-b826-5df5833371dc.jpg"
    }
}
