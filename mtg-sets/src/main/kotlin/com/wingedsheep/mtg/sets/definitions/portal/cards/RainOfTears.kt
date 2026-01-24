package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Rain of Tears
 * {1}{B}{B}
 * Sorcery
 * Destroy target land.
 */
val RainOfTears = card("Rain of Tears") {
    manaCost = "{1}{B}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(filter = PermanentTargetFilter.Land)
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Jim Nelson"
        flavorText = "The land weeps as corruption takes hold."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f.jpg"
    }
}
