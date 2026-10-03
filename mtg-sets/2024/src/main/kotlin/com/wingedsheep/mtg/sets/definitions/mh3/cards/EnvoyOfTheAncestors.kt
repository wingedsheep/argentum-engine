package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Envoy of the Ancestors (MH3 #23)
 * {2}{W}
 * Creature — Human Cleric
 * 2/3
 * Outlast {W} ({W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Modified creatures you control have lifelink. (Equipment, Auras you control, and counters are
 * modifications.)
 *
 * "Modified" is [StatePredicate.IsModified] (CR 700.9) narrowing a creatures-you-control group.
 */
val EnvoyOfTheAncestors = card("Envoy of the Ancestors") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Outlast {W} ({W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\n" +
        "Modified creatures you control have lifelink. (Equipment, Auras you control, and counters are modifications.)"

    // Outlast {W}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Modified creatures you control have lifelink.
    staticAbility {
        ability = GrantKeyword(
            Keyword.LIFELINK,
            GroupFilter(
                GameObjectFilter.Creature.youControl().let {
                    it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
                }
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Irina Nordsol"
        flavorText = "\"No Abzan has ever stood alone.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc1dfbfc-90e0-48fa-98f1-39929f823619.jpg?1783911304"
    }
}
