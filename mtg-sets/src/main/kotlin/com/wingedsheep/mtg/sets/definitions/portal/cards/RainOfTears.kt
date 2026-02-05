package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
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
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Jim Nelson"
        flavorText = "The land weeps as corruption takes hold."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/803ba4ef-24ed-4f45-aed8-f9442322e31e.jpg"
    }
}
