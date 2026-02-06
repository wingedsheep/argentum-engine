package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Sparksmith
 * {1}{R}
 * Creature — Goblin
 * 1/1
 * {T}: Sparksmith deals X damage to target creature and X damage to you,
 * where X is the number of Goblins on the battlefield.
 */
val Sparksmith = card("Sparksmith") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetCreature()
        effect = DealDamageEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")),
            EffectTarget.ContextTarget(0)
        ) then DealDamageEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")),
            EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Pete Venters"
        flavorText = "The more goblins a sparksmith is around, the more they feel like showing off."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36013ddf-ecfe-4e1b-a848-6600189f3044.jpg?1562906232"
    }
}
