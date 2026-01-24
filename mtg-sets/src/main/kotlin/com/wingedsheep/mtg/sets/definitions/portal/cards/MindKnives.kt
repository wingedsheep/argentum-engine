package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardRandomEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent

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
        target = TargetOpponent()
        effect = DiscardRandomEffect(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Andrew Robinson"
        flavorText = "The mind can cut deeper than any blade."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4e92e13-b8cc-4d8a-a9f3-1e3c50b76cca.jpg"
    }
}
