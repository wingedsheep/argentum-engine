package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreadmobile — Modern Horizons 3 #87
 * {2}{B} · Artifact — Vehicle · 3/3 · Uncommon
 *
 * Menace
 * {1}, Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle.
 * Crew 1
 *
 * "Another" is [Costs.SacrificeAnother], so the Vehicle can't feed itself. The counter lands
 * whether or not the Vehicle is currently crewed — it simply sits there until the Vehicle
 * becomes a creature.
 */
val Dreadmobile = card("Dreadmobile") {
    manaCost = "{2}{B}"
    typeLine = "Artifact — Vehicle"
    oracleText = "Menace\n" +
        "{1}, Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 3
    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Artifact or GameObjectFilter.Creature),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{1}, Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Rockey Chen"
        flavorText = "Made tough, with extra corpsepower."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/831506ea-017a-40ff-91c1-ff2d70dc0013.jpg?1783911282"
    }
}
