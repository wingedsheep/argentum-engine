package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Winter's Grasp
 * {1}{G}{G}
 * Sorcery
 * Destroy target land.
 */
val WintersGrasp = card("Winter's Grasp") {
    manaCost = "{1}{G}{G}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(filter = PermanentTargetFilter.Land)
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Rebecca Guay"
        flavorText = "Winter's cold grip claims all, even the land itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7e8f9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a.jpg"
    }
}
