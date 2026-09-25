package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pain Kami
 * {2}{R}
 * Creature — Spirit
 * 2/2
 * {X}{R}, Sacrifice this creature: It deals X damage to target creature.
 *
 * Cinder Elemental's shape without the {T} and narrowed to a creature target. "It" is the sacrificed
 * Pain Kami, so the damage source is read through last-known information.
 */
val PainKami = card("Pain Kami") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    oracleText = "{X}{R}, Sacrifice this creature: It deals X damage to target creature."
    power = 2
    toughness = 2

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Mana("{X}{R}"), Costs.SacrificeSelf)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)
        description = "{X}{R}, Sacrifice this creature: It deals X damage to target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Tomas Giorello"
        flavorText = "\"All kami are our enemies now, A very tough lesson to learn. But it's one that's taken quickly, When you feel what it's like to burn!\"\n—Ku-Ku, akki poet"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/693317ea-0237-4b65-812f-b31a6427b2a3.jpg?1783944296"
    }
}
