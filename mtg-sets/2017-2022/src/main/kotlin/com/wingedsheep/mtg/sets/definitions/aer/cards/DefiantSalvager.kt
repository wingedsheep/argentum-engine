package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Defiant Salvager
 * {2}{B}
 * Creature — Aetherborn Artificer
 * 2/2
 * Sacrifice an artifact or creature: Put a +1/+1 counter on this creature. Activate only as a sorcery.
 */
val DefiantSalvager = card("Defiant Salvager") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Artificer"
    power = 2
    toughness = 2
    oracleText = "Sacrifice an artifact or creature: Put a +1/+1 counter on this creature. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Volkan Baǵa"
        flavorText = "\"I didn't survive that crash just so I could fall to the likes of you.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0afbfb2c-3f1a-4ef9-9f61-6ca51af853d8.jpg?1783936764"
        ruling("2020-11-10", "You can sacrifice Defiant Salvager to activate its own ability. It won't receive a counter, but abilities that trigger on the sacrifice or watch for a creature dying will see it.")
        ruling("2020-11-10", "If you sacrifice an artifact creature to activate Defiant Salvager's ability, you put one +1/+1 counter on Defiant Salvager, not two.")
    }
}
