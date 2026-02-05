package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
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
        effect = DealDamageEffect(
            amount = DynamicAmounts.landsWithSubtype(Subtype.MOUNTAIN),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Adrian Smith"
        flavorText = "The mountains themselves rise to defend their domain."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb16998c-cfa4-49cc-8e37-2dfc33fa2f1e.jpg"
    }
}
