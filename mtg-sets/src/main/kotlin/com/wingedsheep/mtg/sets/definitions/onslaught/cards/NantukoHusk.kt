package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Nantuko Husk
 * {2}{B}
 * Creature — Zombie Insect
 * 2/2
 * Sacrifice a creature: Nantuko Husk gets +2/+2 until end of turn.
 */
val NantukoHusk = card("Nantuko Husk") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Insect"
    power = 2
    toughness = 2
    oracleText = "Sacrifice a creature: Nantuko Husk gets +2/+2 until end of turn."

    activatedAbility {
        cost = AbilityCost.Sacrifice(GameObjectFilter.Creature)
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 2,
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Carl Critchlow"
        flavorText = "The soul sheds light, and death is its shadow. When the light dims, life and death embrace. —Nantuko teaching"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1ff31ece-f132-4107-9415-fcf30e251167.jpg?1562902507"
    }
}
