package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stonewise Fortifier
 * {1}{W}
 * Creature — Human Wizard
 * 2/2
 * {4}{W}: Prevent all damage that would be dealt to this creature by target creature this turn.
 *
 * The shield is bound to the targeted creature as the ability resolves: damage from any other
 * source still reaches this creature, and the target's damage to anything else is untouched.
 */
val StonewiseFortifier = card("Stonewise Fortifier") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{4}{W}: Prevent all damage that would be dealt to this creature by target creature this turn."

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        val attacker = target(TargetFilter.Creature)
        effect = Effects.PreventDamage(
            target = EffectTarget.Self,
            sources = PreventionSourceFilter.Specific(attacker)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Ryan Alexander Lee"
        flavorText = "Ephara taught some mortals to feel every vein in marble as though it flows with their own blood."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b1cd291-92a0-4f0f-b73a-64fc498da1ef.jpg?1783939454"
    }
}
