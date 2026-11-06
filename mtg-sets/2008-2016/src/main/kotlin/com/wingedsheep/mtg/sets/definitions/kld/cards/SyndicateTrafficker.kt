package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Syndicate Trafficker
 * {1}{B}
 * Creature — Aetherborn Rogue
 * 3/1
 * {1}, Sacrifice an artifact: Put a +1/+1 counter on this creature. It gains indestructible until
 * end of turn.
 */
val SyndicateTrafficker = card("Syndicate Trafficker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Rogue"
    power = 3
    toughness = 1
    oracleText = "{1}, Sacrifice an artifact: Put a +1/+1 counter on this creature. It gains " +
        "indestructible until end of turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Artifact)
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "{1}, Sacrifice an artifact: Put a +1/+1 counter on this creature. It gains " +
            "indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Tony Foti"
        flavorText = "\"The price isn't negotiable, but I'm sure you'll agree it's fair.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d89427f0-08d5-49a1-9684-ebe8769430f6.jpg?1783937200"
    }
}
