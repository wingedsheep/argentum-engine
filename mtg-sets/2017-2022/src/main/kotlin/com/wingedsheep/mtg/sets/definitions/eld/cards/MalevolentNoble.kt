package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malevolent Noble
 * {1}{B}
 * Creature — Human Noble
 * 2/2
 * {2}, Sacrifice an artifact or another creature: Put a +1/+1 counter on this creature.
 */
val MalevolentNoble = card("Malevolent Noble") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Noble"
    power = 2
    toughness = 2
    oracleText = "{2}, Sacrifice an artifact or another creature: Put a +1/+1 counter on this creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            // "an artifact or another creature": only the creature branch excludes this permanent.
            Costs.Sacrifice(
                GameObjectFilter.Artifact or
                    GameObjectFilter.Creature.copy(statePredicates = listOf(StatePredicate.Not(StatePredicate.IsSource)))
            ),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{2}, Sacrifice an artifact or another creature: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Randy Vargas"
        flavorText = "He has slain a dozen witches, luring each with a promise of fresh children's bones."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bc7e996-151f-4d88-8e1e-91bb88f5db02.jpg?1783932637"
    }
}
