package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undercity Necrolisk
 * {3}{B}
 * Creature — Zombie Lizard
 * 3/3
 * {1}, Sacrifice another creature: Put a +1/+1 counter on this creature. It gains menace until end
 * of turn. Activate only as a sorcery.
 */
val UndercityNecrolisk = card("Undercity Necrolisk") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Lizard"
    power = 3
    toughness = 3
    oracleText = "{1}, Sacrifice another creature: Put a +1/+1 counter on this creature. It gains menace " +
        "until end of turn. Activate only as a sorcery. (It can't be blocked except by two or more creatures.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Tomasz Jedruszek"
        flavorText = "In its territory, living is an affront."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2571d168-38d4-46d7-afae-ffbd12bd2313.jpg?1783934169"
    }
}
