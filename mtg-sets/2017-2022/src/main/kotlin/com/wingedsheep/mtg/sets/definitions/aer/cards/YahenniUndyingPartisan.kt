package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yahenni, Undying Partisan — Aether Revolt #74
 * {2}{B} · Legendary Creature — Aetherborn Vampire · 2/2
 * Haste
 * Whenever a creature an opponent controls dies, put a +1/+1 counter on Yahenni.
 * Sacrifice another creature: Yahenni gains indestructible until end of turn.
 */
val YahenniUndyingPartisan = card("Yahenni, Undying Partisan") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Aetherborn Vampire"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Whenever a creature an opponent controls dies, put a +1/+1 counter on Yahenni.\n" +
        "Sacrifice another creature: Yahenni gains indestructible until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Lius Lasahido"
        flavorText = "\"The time for subtlety is over, darling.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c37e2466-57c3-453f-aebe-340995f2eca7.jpg?1783936757"
        ruling(
            "2017-02-09",
            "If Yahenni and a creature an opponent controls die simultaneously (perhaps because they fought " +
                "or were in combat together), Yahenni won't be on the battlefield as its triggered ability " +
                "resolves. It can't be saved by the +1/+1 counter that would have been put on it."
        )
    }
}
