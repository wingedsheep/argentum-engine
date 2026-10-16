package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cathedral Acolyte — Fallout #75
 * {1}{G} · Creature — Human Cleric · 1/2
 *
 * Each creature you control with a counter on it has ward {1}.
 * {T}: Put a +1/+1 counter on target creature that entered this turn.
 *
 * The ward grant is [GrantWard] over creatures you control with any counter (the Acolyte
 * itself included, so self-exclusion stays off); the group is re-evaluated as counters come
 * and go. The tap ability targets any creature (either player's) that entered this turn.
 */
val CathedralAcolyte = card("Cathedral Acolyte") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "Each creature you control with a counter on it has ward {1}. (Whenever it becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "{T}: Put a +1/+1 counter on target creature that entered this turn."

    staticAbility {
        ability = GrantWard(
            WardCost.Mana("{1}"),
            GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter())
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter(GameObjectFilter.Creature.enteredThisTurn()))
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Jeremy Paillotin"
        flavorText = "\"Master, evolve me to perfection!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84118221-20a3-46a9-9e6e-a928b46a8633.jpg?1783912398"
    }
}
