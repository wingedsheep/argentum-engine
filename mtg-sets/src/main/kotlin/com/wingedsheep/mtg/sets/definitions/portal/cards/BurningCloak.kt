package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Burning Cloak
 * {R}
 * Sorcery
 * Target creature gets +2/+0 until end of turn. Burning Cloak deals 2 damage to that creature.
 */
val BurningCloak = card("Burning Cloak") {
    manaCost = "{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 0,
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn
        ) then DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Roger Raupp"
        flavorText = "Power comes at a price."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1b2c3d4-e0f1-2a3b-4c5d-6e7f8a9b0c1d.jpg"
    }
}
