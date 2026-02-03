package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Vengeance
 * {3}{W}
 * Sorcery
 * Destroy target tapped creature.
 */
val Vengeance = card("Vengeance") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature(filter = TargetFilter.TappedCreature)
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Andrew Robinson"
        flavorText = "\"Bitter as wormwood, sweet as mulled wine.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c91c249b-157c-4f1d-8171-29d1e75b1c9f.jpg"
    }
}
