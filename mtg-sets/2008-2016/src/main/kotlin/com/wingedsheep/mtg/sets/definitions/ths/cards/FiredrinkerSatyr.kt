package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firedrinker Satyr
 * {R}
 * Creature — Satyr Shaman
 * 2/1
 *
 * Whenever this creature is dealt damage, it deals that much damage to you.
 * {1}{R}: This creature gets +1/+0 until end of turn and deals 1 damage to you.
 *
 * Same dealt-damage shape as Truefire Captain / Coalhauler Swine, aimed at the controller.
 */
val FiredrinkerSatyr = card("Firedrinker Satyr") {
    manaCost = "{R}"
    typeLine = "Creature — Satyr Shaman"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature is dealt damage, it deals that much damage to you.\n" +
        "{1}{R}: This creature gets +1/+0 until end of turn and deals 1 damage to you."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Controller,
        )
        description = "Whenever this creature is dealt damage, it deals that much damage to you."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.DealDamage(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Anthony Palumbo"
        flavorText = "Attending a satyr revel requires a high tolerance for pain."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/683eaf5a-2f43-477f-b212-70ca5fe04f13.jpg?1783939763"
    }
}
