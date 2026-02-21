package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

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
    oracleText = "{T}: Sparksmith deals X damage to target creature and X damage to you, where X is the number of Goblins on the battlefield."

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", TargetCreature())
        effect = DealDamageEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")),
            t
        ) then DealDamageEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")),
            EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Jim Nelson"
        flavorText = "The more goblins a sparksmith is around, the more they feel like showing off."
        imageUri = "https://cards.scryfall.io/large/front/1/5/15a4460d-3fe8-4b1f-9990-0a19c3345367.jpg?1562900172"
    }
}
