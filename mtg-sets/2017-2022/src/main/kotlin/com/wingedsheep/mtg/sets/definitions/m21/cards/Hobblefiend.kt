package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hobblefiend — Core Set 2021 #152 (canonical printing)
 * {1}{R} · Creature — Devil · 2/1
 *
 * Trample
 * {1}, Sacrifice another creature: Put a +1/+1 counter on this creature.
 *
 * The Gobbling Ooze shape: [Costs.SacrificeAnother] excludes Hobblefiend itself.
 */
val Hobblefiend = card("Hobblefiend") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 2
    toughness = 1
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "{1}, Sacrifice another creature: Put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{1}, Sacrifice another creature: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Nicholas Gregory"
        flavorText = "Bedbugs aren't the only things you need to worry about biting you at night."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bfffc18-b36a-4dd5-909e-60ea9f8eb60b.jpg?1783930687"
    }
}
