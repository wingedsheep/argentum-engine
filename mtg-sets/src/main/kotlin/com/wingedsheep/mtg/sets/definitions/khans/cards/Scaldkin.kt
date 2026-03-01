package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scaldkin
 * {3}{U}
 * Creature — Elemental
 * 2/2
 * Flying
 * {2}{R}, Sacrifice Scaldkin: It deals 2 damage to any target.
 */
val Scaldkin = card("Scaldkin") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "Flying\n{2}{R}, Sacrifice Scaldkin: It deals 2 damage to any target."

    keywords(Keyword.FLYING)

    // {2}{R}, Sacrifice Scaldkin: It deals 2 damage to any target.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}"),
            Costs.SacrificeSelf
        )
        val t = target("any target", AnyTarget())
        effect = DealDamageEffect(
            amount = DynamicAmount.Fixed(2),
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Cliff Childs"
        flavorText = "The Temur believe that scaldkin are born when eruptions melt the frozen whispers of sleeping ancestors."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8110eb0a-63dc-43df-b55a-9241f45b1cc6.jpg?1562789336"
    }
}
