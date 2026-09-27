package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vassal's Duty
 * {3}{W}
 * Enchantment
 * {1}: The next 1 damage that would be dealt to target legendary creature you control this turn is
 * dealt to you instead.
 */
val VassalsDuty = card("Vassal's Duty") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next 1 damage that would be dealt to target legendary creature you control " +
        "this turn is dealt to you instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl().legendary()))
        effect = Effects.RedirectNextDamage(
            protectedTargets = listOf(creature),
            redirectTo = EffectTarget.Controller,
            amount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Dave Dorman"
        flavorText = "\"My life is yours, my lord. There is no greater service than to yield it for your safety.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ef25165-2458-4c00-8b7c-0ad6fd98e3af.jpg?1783944331"
    }
}
