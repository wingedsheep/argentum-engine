package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
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
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Rebecca Guay"
        flavorText = "Winter's cold grip claims all, even the land itself."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2215de4-da49-4270-aec7-5e16a938bae4.jpg"
    }
}
