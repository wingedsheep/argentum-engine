package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
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
        target = TargetPermanent(filter = TargetFilter.CreatureOrLandPermanent)
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "John Coulthart"
        flavorText = "Nothing stands before the river of fire."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e825e4-98be-49f0-bc5e-c8988118dcef.jpg"
    }
}
