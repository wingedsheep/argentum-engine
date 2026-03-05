package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Crater's Claws
 * {X}{R}
 * Sorcery
 * Crater's Claws deals X damage to any target.
 * Ferocious — Crater's Claws deals X plus 2 damage instead if you control a creature with power 4 or greater.
 */
val CratersClaws = card("Crater's Claws") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"
    oracleText = "Crater's Claws deals X damage to any target.\nFerocious — Crater's Claws deals X plus 2 damage instead if you control a creature with power 4 or greater."

    spell {
        val t = target("target", AnyTarget())
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            effect = DealDamageEffect(DynamicAmount.Add(DynamicAmount.XValue, DynamicAmount.Fixed(2)), t),
            elseEffect = DealDamageEffect(DynamicAmount.XValue, t)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95dde66b-b4a1-4a1e-8c9e-0bec4790b1e5.jpg?1562790652"
    }
}
