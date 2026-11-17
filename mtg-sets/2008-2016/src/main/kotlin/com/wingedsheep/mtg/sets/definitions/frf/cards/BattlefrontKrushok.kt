package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battlefront Krushok — Fate Reforged #125
 * {4}{G} · Creature — Beast · 3 / 4
 *
 * This creature can't be blocked by more than one creature.
 * Each creature you control with a +1/+1 counter on it can't be blocked by more than one creature.
 *
 * Two [CantBeBlockedByMoreThan] statics: the first covers the Krushok itself (default
 * `GroupFilter.source()`), the second is the group form scoped to creatures you control with a
 * +1/+1 counter (same shape as Flopsie, Bumi's Buddy's power-4 filter). Blocker validation
 * matches attackers as blockers are declared, so a counter added after blocks doesn't undo a
 * double block — exactly the ruling below.
 */
val BattlefrontKrushok = card("Battlefront Krushok") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "This creature can't be blocked by more than one creature.\n" +
        "Each creature you control with a +1/+1 counter on it can't be blocked by more than one creature."

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(
            maxBlockers = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Steve Prescott"
        flavorText = "Krushoks clear debris from fertile fields, fallow fields, and battlefields."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b425cd-c5a5-48e9-b697-3860dfa6d5d3.jpg?1783938681"
        ruling(
            "2014-11-24",
            "The second ability applies only as blockers are declared. If a creature you control with no " +
                "+1/+1 counters on it becomes blocked by more than one creature, putting a +1/+1 counter on " +
                "it won't change or undo the block.",
        )
    }
}
