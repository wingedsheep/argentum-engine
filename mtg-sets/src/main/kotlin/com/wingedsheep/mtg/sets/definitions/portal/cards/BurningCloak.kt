package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

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
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b8f443-dba5-45a5-bb2e-f57b4fdd1d01.jpg"
    }
}
