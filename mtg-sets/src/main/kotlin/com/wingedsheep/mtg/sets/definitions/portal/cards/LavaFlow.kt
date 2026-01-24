package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Lava Flow
 * {3}{R}{R}
 * Sorcery
 * Destroy target creature or land.
 */
val LavaFlow = card("Lava Flow") {
    manaCost = "{3}{R}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(filter = PermanentTargetFilter.CreatureOrLand)
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "John Coulthart"
        flavorText = "Nothing stands before the river of fire."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18a9b0c1-d2e3-f4a5-b6c7-d8e9f0a1b2c3.jpg"
    }
}
