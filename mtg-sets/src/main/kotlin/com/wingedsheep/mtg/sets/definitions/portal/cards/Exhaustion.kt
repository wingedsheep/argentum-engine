package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.SkipUntapEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Exhaustion
 * {2}{U}
 * Sorcery
 * Creatures and lands target opponent controls don't untap during their next untap step.
 */
val Exhaustion = card("Exhaustion") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = SkipUntapEffect(
            target = EffectTarget.ContextTarget(0),
            affectsCreatures = true,
            affectsLands = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d6a5c33-cf74-4cec-a4f4-1aac9e7b8f79.jpg"
    }
}
