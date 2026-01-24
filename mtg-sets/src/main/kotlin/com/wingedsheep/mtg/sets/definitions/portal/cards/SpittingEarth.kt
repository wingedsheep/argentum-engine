package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDynamicDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Spitting Earth
 * {1}{R}
 * Sorcery
 * Spitting Earth deals damage to target creature equal to the number of Mountains you control.
 */
val SpittingEarth = card("Spitting Earth") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = DealDynamicDamageEffect(
            amount = DynamicAmount.LandsWithSubtypeYouControl(Subtype.MOUNTAIN),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Adrian Smith"
        flavorText = "The mountains themselves rise to defend their domain."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4e0f1a2-b3c4-d5e6-f7a8-b9c0d1e2f3a4.jpg"
    }
}
